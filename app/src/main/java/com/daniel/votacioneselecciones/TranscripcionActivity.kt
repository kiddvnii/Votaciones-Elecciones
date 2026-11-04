package com.daniel.votacioneselecciones

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale

class TranscripcionActivity : PantallaConSesion() {
    private lateinit var texto: EditText
    private val dictado = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val frase = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull().orEmpty()
            if (frase.isNotBlank()) {
                val anterior = texto.text.toString().trim()
                texto.setText(listOf(anterior, frase).filter { it.isNotEmpty() }.joinToString("\n"))
                texto.setSelection(texto.length())
            }
        }
    }
    private val guardar = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { salida ->
                    salida.write(texto.text.toString().toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo crear el archivo")
                aviso("Texto guardado correctamente.")
            } catch (error: Exception) {
                aviso("No se pudo guardar el archivo de texto.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Transcripción de voz"
        val margen = (20 * resources.displayMetrics.density).toInt()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margen, margen, margen, margen)
        }
        panel.addView(TextView(this).apply {
            text = "Voz a texto"
            textSize = 23f
            setTextColor(getColor(R.color.azul_primario))
        })
        panel.addView(TextView(this).apply {
            text = "Usa el reconocimiento de voz disponible en tu teléfono y guarda el resultado como .txt."
        })
        texto = EditText(this).apply {
            hint = "La transcripción aparecerá aquí. También puedes editarla."
            gravity = android.view.Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        panel.addView(texto, LinearLayout.LayoutParams(-1, 0, 1f))
        panel.addView(Button(this).apply {
            text = "Hablar y transcribir"
            setOnClickListener {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para transcribir")
                }
                try {
                    dictado.launch(intent)
                } catch (error: android.content.ActivityNotFoundException) {
                    aviso("El dispositivo no tiene disponible reconocimiento de voz.")
                }
            }
        })
        panel.addView(Button(this).apply {
            text = "Guardar archivo TXT"
            setOnClickListener {
                if (texto.text.isBlank()) aviso("Primero transcribe o escribe algún texto.")
                else guardar.launch("transcripcion.txt")
            }
        })
        setContentView(panel)
    }
}
