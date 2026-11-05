package com.daniel.votacioneselecciones

import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.*
import org.junit.Test

class ErroresOperacionTest {
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
    @Test fun conservaValidacionLocal() {
        assertEquals("La elección está cerrada.", ErroresOperacion.mensaje(
            RuntimeException(IllegalArgumentException("La elección está cerrada."))))
    }
    @Test fun errorDesconocidoNoSePresentaComoProblemaDeWifi() {
        assertEquals("No se pudo completar la operación [UnsupportedOperationException].",
            ErroresOperacion.mensaje(UnsupportedOperationException("detalle")))
    }
}
