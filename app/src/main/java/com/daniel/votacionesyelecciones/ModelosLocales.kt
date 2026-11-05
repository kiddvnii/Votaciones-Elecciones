package com.daniel.votacionesyelecciones

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
object ModelosLocales {
    fun copiar(entrada: InputStream, destino: File, limite: Long) {
        destino.outputStream().use { salida ->
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = entrada.read(buffer)
                if (n < 0) break
                total += n
                require(total <= limite) { "El archivo supera el límite de importación" }
                salida.write(buffer, 0, n)
            }
            require(total > 0) { "Archivo vacío" }
        }
    }

    fun extraerVosk(entrada: InputStream, carpeta: File): File {
        require(carpeta.mkdirs())
        val raiz = carpeta.canonicalPath + File.separator
        var total = 0L
        var archivos = 0
        ZipInputStream(entrada).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++archivos <= 10000) { "Demasiadas entradas" }
                val archivo = File(carpeta, entry.name)
                require(archivo.canonicalPath.startsWith(raiz)) { "Ruta ZIP inválida" }
                if (entry.isDirectory) archivo.mkdirs() else {
                    archivo.parentFile?.mkdirs()
                    archivo.outputStream().use { salida ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            total += n
                            require(total <= 512L * 1024 * 1024) { "Modelo descomprimido demasiado grande" }
                            salida.write(buffer, 0, n)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        return carpeta.walkTopDown().firstOrNull {
            it.isDirectory && File(it, "am/final.mdl").isFile && File(it, "conf/model.conf").isFile
        } ?: error("ZIP sin modelo Vosk (am/final.mdl y conf/model.conf)")
    }
}
