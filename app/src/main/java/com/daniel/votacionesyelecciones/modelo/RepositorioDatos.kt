package com.daniel.votacionesyelecciones.modelo

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

 
object RepositorioDatos {
    private val db get() = FirestoreHelper.db
    private val elecciones get() = db.collection("elecciones")
    private val candidatos get() = db.collection("candidatos")
    private val votos get() = db.collection("votos")

    private fun <T> conSesion(operacion: (String) -> Task<T>): Task<T> = try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Tu sesión terminó. Vuelve a iniciar sesión.")
        operacion(uid)
    } catch (error: Exception) {
        Tasks.forException(error)
    }

    private fun <T> Task<T>.evento(nombre: String, entidad: String): Task<T> =
        addOnSuccessListener { AnalyticsHelper.registrar(nombre, entidad) }

    fun nuevoId(coleccion: String): String = db.collection(coleccion).document().id

    fun obtenerElecciones(): Task<List<Eleccion>> = conSesion { uid ->
        elecciones.whereEqualTo("ownerUid", uid).get(Source.SERVER).continueWith { tarea ->
            tarea.result.documents.map { doc ->
                requireNotNull(doc.toObject(Eleccion::class.java)).copy(id = doc.id)
            }.sortedBy { it.nombre.lowercase(Locale.ROOT) }
        }.evento("listado_consultado", "eleccion")
    }

    fun obtenerCandidatos(): Task<List<CandidatoModelo>> = conSesion { uid ->
        candidatos.whereEqualTo("ownerUid", uid).get(Source.SERVER).continueWith { tarea ->
            tarea.result.documents.map { doc ->
                requireNotNull(doc.toObject(CandidatoModelo::class.java)).copy(id = doc.id)
            }.sortedBy { it.nombre.lowercase(Locale.ROOT) }
        }.evento("listado_consultado", "candidato")
    }

    fun obtenerVotos(): Task<List<RegistroVoto>> = conSesion { uid ->
        votos.whereEqualTo("ownerUid", uid).get(Source.SERVER).continueWith { tarea ->
            tarea.result.documents.map { doc ->
                requireNotNull(doc.toObject(RegistroVoto::class.java)).copy(id = doc.id)
            }.sortedByDescending { it.creadoEn }
        }
    }

    fun guardarEleccion(eleccion: Eleccion, nueva: Boolean): Task<Unit> = conSesion { uid ->
        require(Validaciones.eleccion(eleccion) == null) { Validaciones.eleccion(eleccion).orEmpty() }
        obtenerElecciones().onSuccessTask { lista ->
            require(lista.none { it.id != eleccion.id && it.nombre.equals(eleccion.nombre, true) }) {
                "Ya existe una elección con ese nombre."
            }
            val ref = elecciones.document(eleccion.id)
             
            db.runTransaction<Unit> { tx ->
                val anterior = tx.get(ref)
                if (nueva) require(!anterior.exists()) { "Este registro ya se guardó. Actualiza el listado." }
                else comprobarPropietario(anterior, uid)
                val errorFechas = FechasEleccion.errorPeriodo(eleccion, if (nueva) null else anterior.toObject(Eleccion::class.java))
                require(errorFechas == null) { errorFechas.orEmpty() }
                tx.set(ref, mapOf(
                    "nombre" to eleccion.nombre, "fecha" to eleccion.fecha,
                    "fechaTermino" to eleccion.fechaTermino,
                    "estado" to eleccion.estado, "lugar" to eleccion.lugar,
                    "mesas" to eleccion.mesas, "ownerUid" to uid
                ))
                Unit
            }.evento(if (nueva) "registro_creado" else "registro_actualizado", "eleccion")
        }
    }

    fun guardarCandidato(candidato: CandidatoModelo, nuevo: Boolean): Task<Unit> = conSesion { uid ->
        require(Validaciones.candidato(candidato) == null) { Validaciones.candidato(candidato).orEmpty() }
        obtenerCandidatos().onSuccessTask { lista ->
            require(lista.none {
                it.id != candidato.id && it.eleccionId == candidato.eleccionId &&
                    it.nombre.equals(candidato.nombre, true) && it.lista.equals(candidato.lista, true)
            }) { "Ya existe este candidato y lista en esa elección." }
            val ref = candidatos.document(candidato.id)
            db.runTransaction<Unit> { tx ->
                val anterior = tx.get(ref)
                val padre = tx.get(elecciones.document(candidato.eleccionId))
                comprobarPropietario(padre, uid)
                if (nuevo) require(!anterior.exists()) { "Este candidato ya se guardó. Actualiza el listado." }
                else {
                    comprobarPropietario(anterior, uid)
                    require(anterior.getString("eleccionId") == candidato.eleccionId) {
                        "La elección de un candidato existente no se puede cambiar."
                    }
                }
                tx.set(ref, mapOf(
                    "nombre" to candidato.nombre, "lista" to candidato.lista,
                    "edad" to candidato.edad, "cargo" to candidato.cargo,
                    "propuesta" to candidato.propuesta, "eleccionId" to candidato.eleccionId,
                    "ownerUid" to uid
                ))
                Unit
            }.evento(if (nuevo) "registro_creado" else "registro_actualizado", "candidato")
        }
    }

    fun eliminarEleccion(id: String): Task<Unit> = conSesion { uid ->
        val consultaCandidatos = candidatos.whereEqualTo("ownerUid", uid)
            .whereEqualTo("eleccionId", id).get(Source.SERVER)
        val consultaVotos = votos.whereEqualTo("ownerUid", uid)
            .whereEqualTo("eleccionId", id).get(Source.SERVER)
        Tasks.whenAllSuccess<QuerySnapshot>(consultaCandidatos, consultaVotos).onSuccessTask { resultados ->
            eliminarConDependencias(elecciones.document(id).get(Source.SERVER),
                resultados.flatMap { it.documents }, uid)
        }.evento("registro_eliminado", "eleccion")
    }

    fun eliminarCandidato(id: String): Task<Unit> = conSesion { uid ->
        votos.whereEqualTo("ownerUid", uid).whereEqualTo("candidatoId", id)
            .get(Source.SERVER).onSuccessTask { hijos ->
                eliminarConDependencias(candidatos.document(id).get(Source.SERVER), hijos.documents, uid)
            }.evento("registro_eliminado", "candidato")
    }

    private fun eliminarConDependencias(
        padre: Task<DocumentSnapshot>, hijos: List<DocumentSnapshot>, uid: String
    ): Task<Unit> = padre.onSuccessTask { documento ->
        require(hijos.size <= 450) { "Hay demasiados registros asociados. Elimina primero votos y candidatos." }
        db.runTransaction<Unit> { tx ->
            comprobarPropietario(tx.get(documento.reference), uid)
             
            hijos.forEach { comprobarPropietario(tx.get(it.reference), uid) }
            hijos.forEach { tx.delete(it.reference) }
            tx.delete(documento.reference)
            Unit
        }
    }

    fun registrarVoto(id: String, eleccionId: String, candidatoId: String): Task<Unit> = conSesion { uid ->
        val ref = votos.document(id)
        db.runTransaction<Unit> { tx ->
            val anterior = tx.get(ref)
            val eleccion = tx.get(elecciones.document(eleccionId))
            val candidato = tx.get(candidatos.document(candidatoId))
            require(!anterior.exists()) { "Este voto ya se registró. Actualiza el listado." }
            comprobarPropietario(eleccion, uid)
            comprobarPropietario(candidato, uid)
            require(eleccion.getString("estado") == "Activa") { "La elección está cerrada." }
            require(candidato.getString("eleccionId") == eleccionId) { "El candidato pertenece a otra elección." }
            tx.set(ref, mapOf(
                "eleccionId" to eleccionId, "candidatoId" to candidatoId,
                "fechaHora" to SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ROOT).format(Date()),
                "creadoEn" to System.currentTimeMillis(), "ownerUid" to uid
            ))
            Unit
        }
    }

    fun eliminarVoto(id: String): Task<Unit> = conSesion { uid ->
        val ref = votos.document(id)
        db.runTransaction<Unit> { tx ->
            comprobarPropietario(tx.get(ref), uid)
            tx.delete(ref)
            Unit
        }
    }

    private fun comprobarPropietario(doc: DocumentSnapshot, uid: String) {
        require(doc.exists()) { "El registro ya no existe. Actualiza el listado." }
        require(doc.getString("ownerUid") == uid) { "No tienes permiso para modificar este registro." }
    }
}
