package com.daniel.votacioneselecciones

import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.ai.edge.litertlm.*
import java.io.File
import java.util.concurrent.Executors

class AsistenteActivity : PantallaConSesion() {
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var destruida = false
    private var engine: Engine? = null
    private var conversacion: Conversation? = null
    private lateinit var estado: TextView
    private lateinit var historial: TextView
    private lateinit var mensaje: EditText
    private lateinit var enviar: Button
    private lateinit var importar: Button
    private lateinit var reiniciar: Button
    private val archivo get() = File(filesDir, "chatbot.litertlm")
    private val selector = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            ocupado("Importando modelo local…")
            worker.execute {
                val temporal = File(filesDir, "chatbot-import.litertlm")
                try {
                    contentResolver.openInputStream(uri)?.use {
                        ModelosLocales.copiar(it, temporal, 4L * 1024 * 1024 * 1024)
                    } ?: error("No se pudo leer el modelo")
                    cerrarModelo()
                    // Validate before replacing the previous on-disk model.
                    val nuevo = Engine(EngineConfig(modelPath = temporal.path, backend = Backend.CPU(),
                        cacheDir = cacheDir.path, maxNumTokens = 1024))
                    try { nuevo.initialize() } finally { if (nuevo.isInitialized()) nuevo.close() }
                    check(temporal.renameTo(archivo)) { "No se pudo instalar el modelo" }
                    cargar()
                } catch (error: Exception) {
                    ui { disponible("No se pudo importar: modelo incompatible, espacio insuficiente o archivo inválido.") }
                } catch (error: LinkageError) {
                    ui { disponible("El motor nativo no es compatible con este dispositivo.") }
                } finally { temporal.delete() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Asistente local"
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        panel.addView(TextView(this).apply {
            text = "Chatbot IA · LiteRT-LM\nImporta un modelo .litertlm compatible (por ejemplo Gemma3-1B-IT de litert-community, aceptando su licencia). Requiere memoria y espacio libres; puede no funcionar en tu teléfono. Las respuestas pueden equivocarse. No consulta tus votos ni Internet."
        })
        estado = TextView(this)
        panel.addView(estado)
        importar = Button(this).apply {
            text = "Importar modelo .litertlm"
            setOnClickListener { selector.launch(arrayOf("*/*")) }
        }
        panel.addView(importar)
        historial = TextView(this).apply { textSize = 16f; setTextIsSelectable(true) }
        panel.addView(ScrollView(this).apply { addView(historial) }, LinearLayout.LayoutParams(-1, 0, 1f))
        mensaje = EditText(this).apply { hint = "Escribe tu consulta"; maxLines = 4 }
        panel.addView(mensaje)
        enviar = Button(this).apply { text = "Enviar"; isEnabled = false; setOnClickListener { consultar() } }
        panel.addView(enviar)
        reiniciar = Button(this).apply {
            text = "Nueva conversación"; isEnabled = false
            setOnClickListener {
                ocupado("Reiniciando…")
                worker.execute {
                    try {
                        conversacion?.close()
                        conversacion = engine?.createConversation()
                        ui { historial.text = ""; disponible("Conversación nueva, modelo listo.") }
                    } catch (error: Exception) { ui { disponible("No se pudo reiniciar. Reimporta el modelo.") } }
                }
            }
        }
        panel.addView(reiniciar)
        setContentView(panel)
        if (archivo.isFile) {
            ocupado("Cargando modelo…")
            worker.execute { cargar() }
        } else disponible("Falta un modelo. Importa uno para habilitar el chatbot.")
    }

    private fun cargar() {
        try {
            cerrarModelo()
            engine = Engine(EngineConfig(modelPath = archivo.path, backend = Backend.CPU(),
                cacheDir = cacheDir.path, maxNumTokens = 1024))
            engine!!.initialize()
            conversacion = engine!!.createConversation()
            ui { historial.text = ""; disponible("Modelo local listo. Conversación limitada a 1024 tokens; reiníciala si se llena.") }
        } catch (error: Exception) {
            cerrarModelo()
            ui { disponible("No se pudo cargar. Comprueba el modelo, memoria y espacio; puedes importar otro.") }
        } catch (error: LinkageError) {
            ui { disponible("Motor nativo incompatible con este dispositivo.") }
        }
    }

    private fun consultar() {
        val pregunta = mensaje.text.toString().trim()
        if (pregunta.isEmpty() || !enviar.isEnabled) return
        if (pregunta.length > 2000) { aviso("Acorta tu consulta a 2000 caracteres."); return }
        historial.append("\nTú: $pregunta\n")
        mensaje.setText("")
        ocupado("Generando en el dispositivo…")
        worker.execute {
            try {
                val respuesta = checkNotNull(conversacion).sendMessage(pregunta).toString()
                ui { historial.append("\nAsistente: $respuesta\n"); disponible("Modelo listo.") }
            } catch (error: Exception) {
                ui { disponible("No se pudo generar. Prueba Nueva conversación; el contexto o la memoria pueden agotarse.") }
            }
        }
    }

    private fun ocupado(texto: String) {
        estado.text = texto
        enviar.isEnabled = false; importar.isEnabled = false; reiniciar.isEnabled = false
    }
    private fun disponible(texto: String) {
        estado.text = texto
        enviar.isEnabled = conversacion != null
        importar.isEnabled = true; reiniciar.isEnabled = conversacion != null
    }
    private fun ui(accion: () -> Unit) = runOnUiThread { if (!destruida) accion() }
    private fun cerrarModelo() {
        conversacion?.close(); conversacion = null
        engine?.let { if (it.isInitialized()) it.close() }; engine = null
    }
    override fun onDestroy() {
        destruida = true
        worker.execute { cerrarModelo() }
        worker.shutdown()
        super.onDestroy()
    }
}
