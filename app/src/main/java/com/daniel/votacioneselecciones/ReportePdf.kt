package com.daniel.votacioneselecciones

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.daniel.votacioneselecciones.modelo.CandidatoModelo
import com.daniel.votacioneselecciones.modelo.Eleccion
import com.daniel.votacioneselecciones.modelo.RegistroVoto
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportePdf {
    fun escribir(
        salida: OutputStream,
        elecciones: List<Eleccion>,
        candidatos: List<CandidatoModelo>,
        votos: List<RegistroVoto>
    ) {
        val pdf = PdfDocument()
        val ancho = 595
        val alto = 842
        val margen = 42f
        val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
        }
        val titulo = Paint(normal).apply {
            textSize = 19f
            isFakeBoldText = true
        }
        val subtitulo = Paint(normal).apply {
            textSize = 13f
            isFakeBoldText = true
        }
        var numero = 0
        var pagina: PdfDocument.Page? = null
        var y = 0f
        fun siguientePagina() {
            pagina?.let { pdf.finishPage(it) }
            numero++
            pagina = pdf.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
            y = margen
        }
        fun linea(texto: String, estilo: Paint = normal, salto: Float = 19f) {
            if (pagina == null || y + salto > alto - margen) siguientePagina()
            val canvas = pagina!!.canvas
            val disponible = ancho - margen * 2
            var restante = texto
            if (restante.isEmpty()) {
                y += salto
                return
            }
            while (restante.isNotEmpty()) {
                if (y + salto > alto - margen) siguientePagina()
                val cantidad = estilo.breakText(restante, true, disponible, null).coerceAtLeast(1)
                pagina!!.canvas.drawText(restante.take(cantidad), margen, y + estilo.textSize, estilo)
                restante = restante.drop(cantidad).trimStart()
                y += salto
            }
        }
        try {
            linea("Reporte de votaciones", titulo, 30f)
            linea("Generado: " + SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()))
            linea("Total de votos: ${votos.size}", subtitulo, 24f)
            for (eleccion in elecciones.sortedBy { it.nombre }) {
                linea("")
                linea(eleccion.nombre + " (" + eleccion.estado + ")", subtitulo, 24f)
                linea("Fecha: ${eleccion.fecha} · Lugar: ${eleccion.lugar}")
                val registrados = votos.filter { it.eleccionId == eleccion.id }
                linea("Votos registrados: ${registrados.size}")
                val participantes = candidatos.filter { it.eleccionId == eleccion.id }
                for (candidato in participantes.sortedBy { it.nombre }) {
                    val cantidad = registrados.count { it.candidatoId == candidato.id }
                    linea("• ${candidato.nombre} (${candidato.lista}): $cantidad voto(s)")
                }
                if (participantes.isEmpty()) linea("Sin candidatos registrados.")
            }
            if (elecciones.isEmpty()) linea("No hay elecciones registradas.")
            pagina?.let { pdf.finishPage(it) }
            pdf.writeTo(salida)
        } finally {
            pdf.close()
        }
    }
}
