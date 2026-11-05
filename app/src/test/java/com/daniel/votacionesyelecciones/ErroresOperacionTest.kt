package com.daniel.votacionesyelecciones

import org.junit.Assert.*
import org.junit.Test

class ErroresOperacionTest {
    @Test fun conservaValidacionLocal() {
        assertEquals("La elección está cerrada.", ErroresOperacion.mensaje(
            RuntimeException(IllegalArgumentException("La elección está cerrada."))))
    }
    @Test fun errorDesconocidoNoSePresentaComoProblemaDeWifi() {
        assertEquals("No se pudo completar la operación [UnsupportedOperationException].",
            ErroresOperacion.mensaje(UnsupportedOperationException("detalle")))
    }
}
