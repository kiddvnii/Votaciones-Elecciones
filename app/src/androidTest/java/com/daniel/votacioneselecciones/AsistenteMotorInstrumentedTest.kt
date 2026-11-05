package com.daniel.votacioneselecciones

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ai.edge.litertlm.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsistenteMotorInstrumentedTest {
    @Test(timeout = 240000) fun cargaYRespondeConJniAndroidX86Nativa() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("x86_64", Build.SUPPORTED_ABIS.first())
        assertTrue("No se debe usar native bridge ARM", contexto.applicationInfo.nativeLibraryDir.endsWith("/x86_64"))
        val modelo = ModelosIncluidos.chat(contexto) { }
        val motor = Engine(EngineConfig(modelPath = modelo.path, backend = Backend.CPU(), maxNumTokens = 2048,
            cacheDir = contexto.cacheDir.path))
        try {
            motor.initialize() // Exact nativeCreateEngine entry point that crashed in the user's log.
            motor.createConversation(ConversationConfig(
                systemInstruction = Contents.of("Responde en español y brevemente."),
                extraContext = mapOf("enable_thinking" to false),
                samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0),
                maxOutputToken = 32
            )).use { chat ->
                assertTrue(chat.sendMessage("Di hola en español.").toString().isNotBlank())
                val respuesta = chat.sendMessage("Cuánto es dos más dos?").toString()
                assertTrue(respuesta, Regex("(?i)(\\b4\\b|\\bcuatro\\b)").containsMatchIn(respuesta))
            }
        } finally { if (motor.isInitialized()) motor.close() }
    }
}
