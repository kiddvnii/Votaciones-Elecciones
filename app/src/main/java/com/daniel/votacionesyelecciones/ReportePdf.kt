package com.daniel.votacionesyelecciones

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.daniel.votacionesyelecciones.modelo.CandidatoModelo
import com.daniel.votacionesyelecciones.modelo.Eleccion
import com.daniel.votacionesyelecciones.modelo.RegistroVoto
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportePdf {
    fun escribir(salida: OutputStream, elecciones: List<Eleccion>, candidatos: List<CandidatoModelo>, votos: List<RegistroVoto>) {
        PDDocument().use { pdf ->
            var stream: PDPageContentStream? = null
            var y = 0f
            val font = PDType1Font.HELVETICA
            fun pagina() {
                stream?.close()
                val hoja = PDPage(PDRectangle.A4)
                pdf.addPage(hoja)
                stream = PDPageContentStream(pdf, hoja)
                y = PDRectangle.A4.height - 42f
            }
            fun imprimible(texto: String): String = buildString {
                texto.codePoints().forEach { punto ->
                    val caracter = String(Character.toChars(punto))
                    append(if (runCatching { font.getStringWidth(caracter) }.isSuccess && punto >= 32) caracter else "?")
                }
            }
            fun linea(texto: String, tamano: Float = 11f) {
                var restante = imprimible(texto)
                do {
                    if (stream == null || y < 42f + tamano) pagina()
                    var cantidad = 0
                    var ancho = 0f
                    for (caracter in restante) {
                        val siguiente = font.getStringWidth(caracter.toString()) / 1000f * tamano
                        if (ancho + siguiente > PDRectangle.A4.width - 84f) break
                        ancho += siguiente; cantidad++
                    }
                    val fragmento = restante.take(cantidad)
                    stream!!.apply {
                        beginText(); setFont(font, tamano); newLineAtOffset(42f, y); showText(fragmento); endText()
                    }
                    y -= tamano + 8f
                    restante = restante.drop(cantidad).trimStart()
                } while (restante.isNotEmpty())
            }
            try {
                linea("Reporte de votaciones", 19f)
                linea("Generado: " + SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()))
                linea("Total de votos: ${votos.size}", 13f)
                for (eleccion in elecciones.sortedBy { it.nombre }) {
                    linea("")
                    linea("${eleccion.nombre} (${eleccion.estado})", 13f)
                    linea("Inicio: ${eleccion.fecha} · Término: ${eleccion.fechaTermino.ifBlank { "Sin definir" }}")
                    linea("Lugar: ${eleccion.lugar}")
                    val registrados = votos.filter { it.eleccionId == eleccion.id }
                    linea("Votos registrados: ${registrados.size}")
                    val participantes = candidatos.filter { it.eleccionId == eleccion.id }
                    for (candidato in participantes.sortedBy { it.nombre }) {
                        linea("- ${candidato.nombre} (${candidato.lista}): ${registrados.count { it.candidatoId == candidato.id }} voto(s)")
                    }
                    if (participantes.isEmpty()) linea("Sin candidatos registrados.")
                }
                if (elecciones.isEmpty()) linea("No hay elecciones registradas.")
            } finally { stream?.close() }
            pdf.save(salida)
        }
    }
}
