package com.daniel.votacionesyelecciones.modelo

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FechasEleccion {
    private val utc get() = TimeZone.getTimeZone("UTC")
    private fun formato() = SimpleDateFormat("dd-MM-yyyy", Locale.ROOT).apply { timeZone = utc; isLenient = false }
    fun hoy(ahora: Date = Date(), zona: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("dd-MM-yyyy", Locale.ROOT).apply { timeZone = zona }.format(ahora)
    fun milis(fecha: String): Long? = if (Validaciones.fechaValida(fecha)) formato().parse(fecha)?.time else null
    fun desdeMilis(fecha: Long): String = formato().format(Date(fecha))
    fun visible(fecha: String): String = milis(fecha)?.let {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).apply { timeZone = utc }.format(Date(it))
    } ?: fecha
    fun terminoAutomatico(creacion: String = hoy()): String {
        val calendario = Calendar.getInstance(utc).apply {
            timeInMillis = requireNotNull(milis(creacion))
            add(Calendar.MONTH, 3)
        }
        return desdeMilis(calendario.timeInMillis)
    }
    fun errorPeriodo(e: Eleccion, anterior: Eleccion? = null, fechaHoy: String = hoy()): String? {
        val inicio = milis(e.fecha) ?: return "Selecciona una fecha de inicio válida."
        val termino = milis(e.fechaTermino) ?: return "Selecciona una fecha de término válida."
        val hoy = requireNotNull(milis(fechaHoy))
        if (inicio < hoy && e.fecha != anterior?.fecha) return "El inicio debe ser hoy o una fecha futura."
        if (termino < hoy && e.fechaTermino != anterior?.fechaTermino) return "El término debe ser hoy o una fecha futura."
        if (termino < inicio) return "El término no puede ser anterior al inicio."
        return null
    }
}
