package com.daniel.votacionesyelecciones

import com.daniel.votacionesyelecciones.modelo.CandidatoModelo
import com.daniel.votacionesyelecciones.modelo.Eleccion
import com.daniel.votacionesyelecciones.modelo.Validaciones
import org.junit.Assert.*
import org.junit.Test

class ValidacionesTest {
    private val eleccion = Eleccion(nombre = "Centro de alumnos", fecha = "20-10-2026",
        lugar = "Sede de prueba", mesas = 2)
    private val candidato = CandidatoModelo(nombre = "Persona ficticia", lista = "Lista A",
        eleccionId = "prueba", edad = 20, cargo = "Presidencia", propuesta = "Mejorar la biblioteca.")

    @Test fun fechasRealesYBisiestos() {
        assertTrue(Validaciones.fechaValida("29-02-2024"))
        assertTrue(Validaciones.fechaValida("31-12-2100"))
        for (fecha in listOf("29-02-2025", "31-04-2026", "00-01-2026", "1-1-2026",
            "20-10-2026 basura", "20-10-1999", "01-01-2101", "   ")) {
            assertFalse(fecha, Validaciones.fechaValida(fecha))
        }
    }

    @Test fun numerosInvalidosYDesbordamiento() {
        for (valor in listOf("", " ", "0", "-1", "1001", "1.5", "abc", "99999999999999999"))
            assertNull(valor, Validaciones.enteroEnRango(valor, 1..1000))
        assertEquals(1, Validaciones.enteroEnRango("1", 1..1000))
        assertEquals(1000, Validaciones.enteroEnRango(" 1000 ", 1..1000))
    }

    @Test fun cincoCamposDeEleccionValidados() {
        assertNull(Validaciones.eleccion(eleccion))
        for (dato in listOf(
            eleccion.copy(nombre = " \t "), eleccion.copy(nombre = "X".repeat(101)),
            eleccion.copy(fecha = "31-02-2026"), eleccion.copy(estado = "Otro"),
            eleccion.copy(lugar = " "), eleccion.copy(mesas = 0), eleccion.copy(mesas = 1001))) {
            assertNotNull(Validaciones.eleccion(dato))
        }
    }

    @Test fun cincoCamposDeCandidatoValidados() {
        assertNull(Validaciones.candidato(candidato))
        for (dato in listOf(candidato.copy(nombre = " "), candidato.copy(lista = "\t"),
            candidato.copy(edad = 17), candidato.copy(edad = 121), candidato.copy(cargo = "  "),
            candidato.copy(propuesta = " "), candidato.copy(propuesta = "a".repeat(501)),
            candidato.copy(eleccionId = ""))) {
            assertNotNull(Validaciones.candidato(dato))
        }
        assertNull(Validaciones.candidato(candidato.copy(edad = 18, propuesta = "Propuesta de prueba\ncon dos líneas.")))
        assertNull(Validaciones.candidato(candidato.copy(edad = 120)))
    }
}
