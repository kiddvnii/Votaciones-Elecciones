package com.daniel.votacionesyelecciones

import com.daniel.votacionesyelecciones.modelo.*
import java.util.Date
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class FechasEleccionTest {
    private fun eleccion(inicio: String, fin: String) = Eleccion(fecha = inicio, fechaTermino = fin)
    @Test fun tresMesesDeCalendarioNoNoventaDias() {
        assertEquals("05-02-2027", FechasEleccion.terminoAutomatico("05-11-2026"))
        assertEquals("30-04-2027", FechasEleccion.terminoAutomatico("31-01-2027"))
        assertEquals("29-02-2024", FechasEleccion.terminoAutomatico("30-11-2023"))
        assertEquals("28-02-2027", FechasEleccion.terminoAutomatico("30-11-2026"))
    }
    @Test fun permiteHoyYFuturoPeroNoPasado() {
        val hoy = "05-11-2026"
        assertNull(FechasEleccion.errorPeriodo(eleccion(hoy, "05-02-2027"), fechaHoy = hoy))
        assertNull(FechasEleccion.errorPeriodo(eleccion("20-12-2026", "05-02-2027"), fechaHoy = hoy))
        assertNotNull(FechasEleccion.errorPeriodo(eleccion("04-11-2026", "05-02-2027"), fechaHoy = hoy))
        assertNotNull(FechasEleccion.errorPeriodo(eleccion(hoy, "04-11-2026"), fechaHoy = hoy))
        assertNotNull(FechasEleccion.errorPeriodo(eleccion("10-12-2026", "09-12-2026"), fechaHoy = hoy))
    }
    @Test fun editarConservaHistoriaPeroNoCambiaAUnaFechaPasadaDistinta() {
        val anterior = eleccion("01-10-2026", "02-11-2026")
        assertNull(FechasEleccion.errorPeriodo(anterior, anterior, "05-11-2026"))
        assertNotNull(FechasEleccion.errorPeriodo(anterior.copy(fecha = "02-10-2026"), anterior, "05-11-2026"))
        assertNotNull(FechasEleccion.errorPeriodo(anterior.copy(fechaTermino = "03-11-2026"), anterior, "05-11-2026"))
        assertNull(FechasEleccion.errorPeriodo(anterior.copy(fechaTermino = "10-11-2026"), anterior, "05-11-2026"))
    }
    @Test fun rechazaFechasInvalidasYTerminoVacio() {
        assertNotNull(FechasEleccion.errorPeriodo(eleccion("31-11-2026", "05-02-2027"), fechaHoy = "05-11-2026"))
        assertNotNull(FechasEleccion.errorPeriodo(eleccion("05-11-2026", ""), fechaHoy = "05-11-2026"))
    }
    @Test fun unaFechaDeCambioHorarioNoSeRechazaPorLaMedianocheInexistente() {
        val anterior = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"))
            assertTrue(Validaciones.fechaValida("06-09-2026"))
            assertNotNull(FechasEleccion.milis("06-09-2026"))
        } finally { TimeZone.setDefault(anterior) }
    }
    @Test fun hoyUsaElDiaLocalNoElDiaUtcDelSelector() {
        val instante = Date(FechasEleccion.milis("05-11-2026")!! + 2 * 3600000L)
        assertEquals("04-11-2026", FechasEleccion.hoy(instante, TimeZone.getTimeZone("GMT-03:00")))
        assertEquals("05-11-2026", FechasEleccion.hoy(instante, TimeZone.getTimeZone("UTC")))
        assertEquals("05-11-2026", FechasEleccion.desdeMilis(FechasEleccion.milis("05-11-2026")!!))
    }
}
