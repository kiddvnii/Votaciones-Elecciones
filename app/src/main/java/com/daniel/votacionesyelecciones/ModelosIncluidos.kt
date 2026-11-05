package com.daniel.votacionesyelecciones

import android.content.Context
import java.io.File
import java.security.MessageDigest
object ModelosIncluidos {
    private const val SHA_CHAT = "c5f39b4191926786c3e6668b62af4d6eb15248dfc7a5348252a9b406f33f2108"
    @Synchronized fun voz(contexto: Context): File {
        val destino = File(contexto.filesDir, "voz-es-0.42-incluida")
        val modelo = File(destino, "vosk-model-small-es-0.42")
        if (File(destino, ".preparado").isFile && File(modelo, "am/final.mdl").isFile) return modelo
        val temporal = File(contexto.filesDir, "voz-incluida-temporal")
        temporal.deleteRecursively()
        try {
            contexto.assets.open("modelos/voz-es.zip").use { ModelosLocales.extraerVosk(it, temporal) }
            check(File(temporal, "vosk-model-small-es-0.42/am/final.mdl").isFile)
            File(temporal, ".preparado").writeText("0.42")
            destino.deleteRecursively()
            check(temporal.renameTo(destino)) { "No se pudo preparar la voz" }
            return modelo
        } finally { temporal.deleteRecursively() }
    }
    @Synchronized fun chat(contexto: Context, progreso: (Int) -> Unit): File {
        val destino = File(contexto.filesDir, "qwen3-c5f39b41.litertlm")
        val marca = File(contexto.filesDir, "qwen3-c5f39b41.preparado")
        if (destino.isFile && destino.length() == 613_722_264L && marca.isFile) return destino
        val temporal = File(contexto.filesDir, "qwen3-incluido-temporal")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            contexto.assets.open("modelos/asistente-qwen3.litertlm").use { entrada ->
                temporal.outputStream().use { salida ->
                    val buffer = ByteArray(1024 * 1024)
                    var total = 0L; var ultimo = -1
                    while (true) {
                        val n = entrada.read(buffer); if (n < 0) break
                        total += n; check(total <= 630_000_000) { "Modelo incluido invalido" }
                        digest.update(buffer, 0, n); salida.write(buffer, 0, n)
                        val mb = (total / 1024 / 1024).toInt()
                        if (mb / 8 != ultimo) { ultimo = mb / 8; progreso(mb) }
                    }
                }
            }
            check(digest.digest().joinToString("") { "%02x".format(it) } == SHA_CHAT) { "Modelo incluido incompleto" }
            check(temporal.renameTo(destino))
            marca.writeText(SHA_CHAT)
            return destino
        } finally { temporal.delete() }
    }
}
