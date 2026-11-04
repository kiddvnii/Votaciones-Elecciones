package com.daniel.votacioneselecciones

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.launch

class AsistenteActivity : PantallaConSesion() {
    private val modelo by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-3.8-flash")
    }
    private val conversacion by lazy { modelo.startChat() }
    private lateinit var historial: TextView
    private lateinit var mensaje: EditText
    private lateinit var enviar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Asistente electoral"
        val margen = (18 * resources.displayMetrics.density).toInt()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margen, margen, margen, margen)
        }
        val titulo = TextView(this).apply {
            text = "Asistente electoral"
            textSize = 23f
            setTextColor(getColor(R.color.azul_primario))
        }
        val descripcion = TextView(this).apply {
            text = "Haz preguntas sobre elecciones y cómo usar la aplicación. Las respuestas de IA pueden equivocarse."
        }
        historial = TextView(this).apply {
            text = "Asistente: ¡Hola! ¿En qué puedo ayudarte?\n"
            textSize = 16f
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(historial)
        }
        mensaje = EditText(this).apply {
            hint = "Escribe tu consulta"
            maxLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        enviar = Button(this).apply { text = "Enviar" }
        panel.addView(titulo)
        panel.addView(descripcion)
        panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        panel.addView(mensaje)
        panel.addView(enviar)
        setContentView(panel)
        enviar.setOnClickListener { consultar() }
    }

    private fun consultar() {
        val pregunta = mensaje.text.toString().trim()
        if (pregunta.isEmpty() || !enviar.isEnabled) return
        mensaje.setText("")
        historial.append("\nTú: $pregunta\n")
        enviar.isEnabled = false
        enviar.text = "Consultando…"
        lifecycleScope.launch {
            try {
                val respuesta = conversacion.sendMessage(pregunta).text
                    ?.takeIf { it.isNotBlank() } ?: "No recibí una respuesta. Intenta reformular tu pregunta."
                historial.append("\nAsistente: $respuesta\n")
            } catch (error: Exception) {
                historial.append("\nAsistente: No pude conectarme. Comprueba Firebase AI Logic, App Check y tu conexión.\n")
            } finally {
                enviar.isEnabled = true
                enviar.text = "Enviar"
            }
        }
    }
}
