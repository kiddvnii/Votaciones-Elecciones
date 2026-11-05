package com.daniel.votacionesyelecciones

import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class ErroresFirebaseInstrumentedTest {
    @Test fun conservaCodigoFirestoreAunqueTengaCausaInterna() {
        val firebase = FirebaseFirestoreException("denegado", FirebaseFirestoreException.Code.PERMISSION_DENIED,
            RuntimeException("detalle interno"))
        val mensaje = ErroresOperacion.mensaje(RuntimeException("envoltorio", firebase))
        assertTrue(mensaje.contains("[PERMISSION_DENIED]"))
        assertFalse(mensaje.contains("conexión"))
    }
    @Test fun diferenciaFaltaDeRedDePermisos() {
        val mensaje = ErroresOperacion.mensaje(FirebaseFirestoreException("red", FirebaseFirestoreException.Code.UNAVAILABLE))
        assertTrue(mensaje.contains("[UNAVAILABLE]"))
        assertTrue(mensaje.contains("conexión"))
    }
}
