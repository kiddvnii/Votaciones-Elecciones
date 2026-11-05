import { readFile } from 'node:fs/promises';
import { before, after, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, getDocs, collection, query, where, updateDoc, deleteDoc, runTransaction } from 'firebase/firestore';
let env;
const fecha = date => `${String(date.getUTCDate()).padStart(2,'0')}-${String(date.getUTCMonth()+1).padStart(2,'0')}-${date.getUTCFullYear()}`;
const hoy = fecha(new Date());
const futura = fecha(new Date(Date.now() + 90*86400000));
const pasada = fecha(new Date(Date.now() - 7*86400000));
const datos = (extra = {}) => ({ ownerUid:'ana', nombre:'Elección de prueba', fecha:hoy, fechaTermino:futura, estado:'Activa', lugar:'Sede', mesas:2, ...extra });
const db = uid => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
before(async () => {
  env = await initializeTestEnvironment({ projectId:'demo-votaciones-pruebas', firestore:{ host:'127.0.0.1', port:8080, rules:await readFile(new URL('../../firestore.rules', import.meta.url),'utf8') } });
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });
test('propietario crea, consulta por ownerUid, edita término y elimina', async () => {
  const base = db('ana'); const ref = doc(base,'elecciones/e1');
  await assertSucceeds(setDoc(ref, datos()));
  await assertSucceeds(getDocs(query(collection(base,'elecciones'),where('ownerUid','==','ana'))));
  await assertSucceeds(updateDoc(ref,{fechaTermino:fecha(new Date(Date.now()+120*86400000))}));
  await assertSucceeds(deleteDoc(ref));
});
test('la transacción de alta puede leer que aún no existe el documento', async () => {
  const base = db('ana'); const ref = doc(base,'elecciones/nueva');
  await assertSucceeds(runTransaction(base, async tx => { const previo = await tx.get(ref); if (previo.exists()) throw new Error('duplicado'); tx.set(ref,datos()); }));
});
test('rechaza acceso público, lectura ajena y secuestro de propietario', async () => {
  await assertSucceeds(setDoc(doc(db('ana'),'elecciones/e1'),datos()));
  await assertFails(getDoc(doc(db(null),'elecciones/e1')));
  await assertFails(getDoc(doc(db('bea'),'elecciones/e1')));
  await assertFails(setDoc(doc(db('bea'),'elecciones/e2'),datos()));
  await assertFails(updateDoc(doc(db('ana'),'elecciones/e1'),{ownerUid:'bea'}));
});
test('rechaza fechas inexistentes, término anterior, fechas pasadas y campos extra', async () => {
  const base = db('ana');
  for (const [i,cambio] of [{fecha:'31-02-2027'},{fechaTermino:'31-02-2027'},{fechaTermino:pasada},{fecha:pasada},{admin:true},{nombre:'x'.repeat(101)},{fechaTermino:42}].entries()) {
    await assertFails(setDoc(doc(base,`elecciones/mala${i}`),datos(cambio)));
  }
  const inicio = fecha(new Date(Date.now()+60*86400000));
  await assertFails(setDoc(doc(base,'elecciones/invertida'),datos({fecha:inicio,fechaTermino:hoy})));
});
test('lectura de registros antiguos y migración al editar sin inventar fecha histórica', async () => {
  const antiguo = datos({fecha:pasada}); delete antiguo.fechaTermino;
  await env.withSecurityRulesDisabled(async ctx => { await setDoc(doc(ctx.firestore(),'elecciones/antigua'),antiguo); });
  await assertSucceeds(getDoc(doc(db('ana'),'elecciones/antigua')));
  await assertSucceeds(setDoc(doc(db('ana'),'elecciones/antigua'),datos({fecha:pasada})));
});
test('no permite saltarse validaciones mediante update', async () => {
  const ref = doc(db('ana'),'elecciones/e1'); await assertSucceeds(setDoc(ref,datos()));
  await assertFails(updateDoc(ref,{fechaTermino:pasada}));
  await assertFails(updateDoc(ref,{nombre:'x'.repeat(101)}));
  await assertFails(updateDoc(ref,{admin:true}));
});
test('candidatos/votos solo con padres propios y elección activa', async () => {
  const base = db('ana'); await assertSucceeds(setDoc(doc(base,'elecciones/e1'),datos()));
  const c = {ownerUid:'ana',nombre:'Persona',lista:'Lista A',edad:25,cargo:'Presidencia',propuesta:'Propuesta de prueba',eleccionId:'e1'};
  await assertSucceeds(setDoc(doc(base,'candidatos/c1'),c));
  await assertFails(setDoc(doc(db('bea'),'candidatos/c2'),{...c,ownerUid:'bea'}));
  const voto = {ownerUid:'ana',eleccionId:'e1',candidatoId:'c1',fechaHora:hoy+' 12:00',creadoEn:Date.now()};
  await assertSucceeds(setDoc(doc(base,'votos/v1'),voto));
  await assertFails(setDoc(doc(db('bea'),'votos/v2'),{...voto,ownerUid:'bea'}));
  await assertSucceeds(updateDoc(doc(base,'elecciones/e1'),{estado:'Cerrada'}));
  await assertFails(setDoc(doc(base,'votos/v3'),{...voto,creadoEn:Date.now()}));
});
