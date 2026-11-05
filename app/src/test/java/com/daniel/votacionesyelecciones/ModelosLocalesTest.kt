package com.daniel.votacionesyelecciones

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ModelosLocalesTest {
    private fun zip(vararg rutas: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { salida ->
            rutas.forEach { salida.putNextEntry(ZipEntry(it)); salida.write(byteArrayOf(1)); salida.closeEntry() }
        }
        return bytes.toByteArray()
    }
    @Test fun rechazaTraversalSinEscribirFuera() {
        val raiz = Files.createTempDirectory("vosk-test").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                ModelosLocales.extraerVosk(ByteArrayInputStream(zip("../escape")), File(raiz, "import"))
            }
            assertFalse(File(raiz, "escape").exists())
        } finally { raiz.deleteRecursively() }
    }
    @Test fun encuentraModeloDentroDeCarpetaZip() {
        val raiz = Files.createTempDirectory("vosk-test").toFile()
        try {
            val modelo = ModelosLocales.extraerVosk(ByteArrayInputStream(zip("modelo/am/final.mdl", "modelo/conf/model.conf")), File(raiz, "import"))
            assertEquals("modelo", modelo.name)
        } finally { raiz.deleteRecursively() }
    }
    @Test fun rechazaZipSinEstructuraDeModelo() {
        val raiz = Files.createTempDirectory("vosk-test").toFile()
        try {
            assertThrows(IllegalStateException::class.java) {
                ModelosLocales.extraerVosk(ByteArrayInputStream(zip("texto.txt")), File(raiz, "import"))
            }
        } finally { raiz.deleteRecursively() }
    }
    @Test fun limitaCopiaYRechazaVacios() {
        val raiz = Files.createTempDirectory("modelo-test").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) { ModelosLocales.copiar(ByteArrayInputStream(byteArrayOf(1,2,3)), File(raiz, "grande"), 2) }
            assertThrows(IllegalArgumentException::class.java) { ModelosLocales.copiar(ByteArrayInputStream(byteArrayOf()), File(raiz, "vacio"), 2) }
        } finally { raiz.deleteRecursively() }
    }
}
