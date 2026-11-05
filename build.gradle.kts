import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

 
plugins {
    id("com.android.application") version "8.10.1" apply false
    id("com.android.asset-pack") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.21" apply false
     
     
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.firebase.crashlytics") version "3.0.7" apply false
}
// Models are acquired at BUILD time, never by the installed app.
// Every normal Android Studio/CI build includes the same pinned chatbot weights.
val prepararModelos by tasks.registering {
    group = "build setup"
    description = "Obtiene y verifica los modelos incluidos antes de empaquetar la app"
    val destino = layout.projectDirectory.dir("modelos/src/main/assets/modelos")
    outputs.dir(destino)
    outputs.upToDateWhen { false } // Recheck cached bytes, including after interrupted builds.
    doLast {
        val carpeta = destino.asFile.apply { mkdirs() }
        fun sha(archivo: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            archivo.inputStream().use { entrada ->
                val buffer = ByteArray(1024 * 1024)
                while (true) { val n = entrada.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        fun obtener(nombre: String, url: String, checksum: String?, minimo: Long, maximo: Long) {
            val archivo = File(carpeta, nombre)
            fun valido() = archivo.isFile && archivo.length() in minimo..maximo &&
                (checksum == null || sha(archivo) == checksum)
            if (valido()) { logger.lifecycle("Modelo verificado: $nombre (${archivo.length()} bytes)"); return }
            val temporal = File(carpeta, "$nombre.part")
            var ultimo: Exception? = null
            for (intento in 1..3) {
                try {
                    logger.lifecycle("Descargando modelo incluido: $nombre (intento $intento/3)")
                    val conexion = URI(url + "?download=true&build=" + System.currentTimeMillis()).toURL().openConnection() as HttpURLConnection
                    conexion.connectTimeout = 30000; conexion.readTimeout = 120000
                    conexion.setRequestProperty("User-Agent", "VotacionesElecciones-build/1.4")
                    try {
                        check(conexion.responseCode == 200) { "HTTP ${conexion.responseCode}: $nombre" }
                        conexion.inputStream.use { entrada -> temporal.outputStream().use { salida ->
                            val buffer = ByteArray(1024 * 1024); var total = 0L
                            while (true) {
                                val n = entrada.read(buffer); if (n < 0) break
                                total += n; check(total <= maximo) { "Modelo demasiado grande: $nombre" }
                                salida.write(buffer, 0, n)
                            }
                        } }
                    } finally { conexion.disconnect() }
                    check(temporal.length() in minimo..maximo) { "Descarga incompleta: $nombre" }
                    val calculado = sha(temporal)
                    check(checksum == null || calculado == checksum) { "SHA-256 incorrecto: $nombre" }
                    Files.move(temporal.toPath(), archivo.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    logger.lifecycle("Modelo incluido: $nombre SHA-256=$calculado bytes=${archivo.length()}")
                    return
                } catch (error: Exception) { ultimo = error; temporal.delete() }
            }
            throw GradleException("No se pudo obtener $nombre. La compilacion se detiene para no distribuir una app sin modelos.", ultimo)
        }
        obtener("APACHE-2.0.txt", "https://www.apache.org/licenses/LICENSE-2.0.txt", "cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30", 10000, 20000)
        obtener("QWEN-MODEL-CARD.md", "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/e6687177e00897731e7716267eebd4eba0dde7c2/README.md", "429356404fc4eb4eb6897750bb31c114c21a788323e54533669565b11dc066f0", 1000, 100000)
        obtener("voz-es.zip", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip",
            "09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f", 30_000_000, 60_000_000)
        obtener("asistente-qwen3.litertlm", "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/e6687177e00897731e7716267eebd4eba0dde7c2/Qwen3-0.6B.litertlm",
            "c5f39b4191926786c3e6668b62af4d6eb15248dfc7a5348252a9b406f33f2108", 600_000_000, 630_000_000)
    }
}
