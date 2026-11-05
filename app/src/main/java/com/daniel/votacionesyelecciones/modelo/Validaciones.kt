package com.daniel.votacionesyelecciones.modelo

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

object Validaciones {
    fun fechaValida(valor: String): Boolean {
        if (!Regex("\\d{2}-\\d{2}-\\d{4}").matches(valor)) return false
        val anio = valor.takeLast(4).toIntOrNull() ?: return false
        if (anio !in 2000..2100) return false
        val posicion = ParsePosition(0)
        val fecha = SimpleDateFormat("dd-MM-yyyy", Locale.ROOT).apply { isLenient = false; timeZone = java.util.TimeZone.getTimeZone("UTC") }
        return fecha.parse(valor, posicion) != null && posicion.index == valor.length
    }

    fun enteroEnRango(valor: String, rango: IntRange): Int? =
        valor.trim().toIntOrNull()?.takeIf { it in rango }

    fun eleccion(e: Eleccion): String? = when {
        e.nombre.trim().length !in 2..100 -> "El nombre debe tener entre 2 y 100 caracteres."
        !fechaValida(e.fecha) -> "Ingresa una fecha real entre 2000 y 2100 con formato dd-mm-aaaa."
        e.estado !in setOf("Activa", "Cerrada") -> "Selecciona Activa o Cerrada."
        e.lugar.trim().length !in 2..120 -> "El lugar debe tener entre 2 y 120 caracteres."
        e.mesas !in 1..1000 -> "La cantidad de mesas debe ser un entero entre 1 y 1000."
        else -> null
    }

    fun candidato(c: CandidatoModelo): String? = when {
        c.nombre.trim().length !in 2..100 -> "El nombre debe tener entre 2 y 100 caracteres."
        c.lista.trim().length !in 1..80 -> "La lista o partido debe tener entre 1 y 80 caracteres."
        c.edad !in 18..120 -> "La edad debe ser un entero entre 18 y 120 años."
        c.cargo.trim().length !in 2..80 -> "El cargo debe tener entre 2 y 80 caracteres."
        c.propuesta.trim().length !in 5..500 -> "La propuesta debe tener entre 5 y 500 caracteres."
        c.eleccionId.isBlank() -> "Selecciona una elección."
        else -> null
    }
}
