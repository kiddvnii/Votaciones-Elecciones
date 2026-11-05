package com.daniel.votacionesyelecciones

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ai.edge.litertlm.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsistenteMotorInstrumentedTest {
    @Test(timeout = 120000) fun cargaVoskNativoSinInventarTextoCuandoSoloHaySilencio() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("x86_64", Build.SUPPORTED_ABIS.first())
        org.vosk.Model(ModelosIncluidos.voz(contexto).path).use { modelo ->
            org.vosk.Recognizer(modelo, 16000f).use { reconocedor ->
                val silencio = ByteArray(32000)
                reconocedor.acceptWaveForm(silencio, silencio.size)
                assertEquals("", org.json.JSONObject(reconocedor.finalResult).optString("text"))
            }
        }
    }
    @Test(timeout = 240000) fun cargaYRespondeConJniAndroidX86Nativa() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("x86_64", Build.SUPPORTED_ABIS.first())
        assertTrue("No se debe usar native bridge ARM", contexto.applicationInfo.nativeLibraryDir.endsWith("/x86_64"))
        val modelo = ModelosIncluidos.chat(contexto) { }
        val motor = Engine(EngineConfig(modelPath = modelo.path, backend = Backend.CPU(), maxNumTokens = 2048,
            cacheDir = contexto.cacheDir.path))
        try {
            motor.initialize()
            motor.createConversation(ConversationConfig(
                systemInstruction = Contents.of("Responde en español y brevemente."),
                extraContext = mapOf("enable_thinking" to false),
                samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0),
                maxOutputToken = 32
            )).use { chat ->
                assertTrue(chat.sendMessage("Di hola en español.").toString().isNotBlank())
                val respuesta = chat.sendMessage("¿Qué es un candidato en una elección?").toString()
                assertTrue("No se generó la respuesta de seguimiento", respuesta.isNotBlank())
                android.util.Log.i("PruebaMotor", "Motor Android x86_64: carga y dos respuestas completas sin cierre nativo")
            }
        } finally { if (motor.isInitialized()) motor.close() }
    }
}
