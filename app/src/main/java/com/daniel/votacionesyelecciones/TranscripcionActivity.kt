package com.daniel.votacionesyelecciones

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.util.concurrent.Executors

class TranscripcionActivity : PantallaConSesion() {
    private lateinit var diseno: HerramientaUi
    private lateinit var texto: EditText
    private lateinit var parcial: TextView
    private lateinit var contador: TextView
    private lateinit var hablar: Button
    private lateinit var guardarBoton: Button
    private lateinit var reintentar: Button
    private var modelo: Model? = null
    private var servicio: SpeechService? = null
    private var reconocedor: Recognizer? = null
    private var generacion = 0
    private var preparando = false
    private var ultimaParcial = ""
    private var palabrasDetectadas = false
    private val avisos = android.os.Handler(android.os.Looper.getMainLooper())
    private val sinPalabras = Runnable {
        if (!destruida && servicio != null && !palabrasDetectadas) {
            diseno.estado("Aún no se reconocen palabras. Si usas el emulador, activa Microphone → Virtual microphone uses host audio input. En un teléfono, revisa el permiso y habla cerca del micrófono.")
        }
    }
    private val borrador by lazy {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        getSharedPreferences("borrador-dictado-$uid", MODE_PRIVATE)
    }
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var destruida = false
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
        if (permitido) iniciar()
        else diseno.estado("Necesitamos permiso de micrófono para escucharte. Puedes concederlo desde Ajustes o al volver a pulsar el botón.", error = true)
    }
    private val guardar = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val contenido = texto.text.toString()
            worker.execute {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(contenido.toByteArray(Charsets.UTF_8)) }
                        ?: error("No se pudo crear el archivo")
                    ui { aviso("Transcripción guardada.") }
                } catch (error: Exception) { ui { aviso("No se pudo guardar. Elige otra ubicación.") } }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        diseno = HerramientaUi(this, "Voz a texto", "Dictado en español")
        diseno.cabecera(R.drawable.ic_microfono, "TU VOZ SE QUEDA CONTIGO", "Habla. Nosotros escribimos.",
            "Convierte tus ideas en texto, edita el resultado y guárdalo. Funciona sin conexión.")
        diseno.estadoInicial()
        reintentar = diseno.boton("Volver a preparar", true).apply {
            visibility = View.GONE; setOnClickListener { preparar() }
        }
        diseno.agregar(reintentar)
        val tarjeta = diseno.tarjeta()
        val cabecera = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        cabecera.addView(diseno.texto("Tu transcripción", 18, diseno.azul, true), LinearLayout.LayoutParams(0, -2, 1f))
        contador = diseno.texto("0 caracteres", 11)
        cabecera.addView(contador); tarjeta.addView(cabecera)
        diseno.espacio(tarjeta, 10)
        texto = EditText(this).apply {
            hint = "Pulsa Empezar a dictar y habla con naturalidad.\n\nTambién puedes escribir aquí."
            textSize = 16f; minLines = 8; gravity = Gravity.TOP
            setTextColor(Color.rgb(30, 41, 59)); setHintTextColor(diseno.gris)
            setPadding(0, diseno.dp(8), 0, diseno.dp(8)); background = null
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(savedInstanceState?.getString("texto") ?: borrador.getString("texto", "").orEmpty())
        }
        tarjeta.addView(texto, LinearLayout.LayoutParams(-1, -2))
        parcial = diseno.texto("", 14).apply { visibility = View.GONE }
        parcial.setPadding(0, 0, 0, diseno.dp(10))
        parcial.maxLines = 3
        parcial.ellipsize = android.text.TextUtils.TruncateAt.END
        diseno.pie.addView(parcial, LinearLayout.LayoutParams(-1, -2))
        diseno.agregar(tarjeta)
        diseno.agregar(diseno.texto("El borrador se conserva en este dispositivo. Usa Guardar TXT para crear un archivo. Para dictar, habla cerca del micrófono y evita el ruido.", 13))
        hablar = diseno.boton("Empezar a dictar").apply {
            isEnabled = false
            setIconResource(R.drawable.ic_microfono)
            setOnClickListener {
                if (servicio != null) detener()
                else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) iniciar()
                else permiso.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        diseno.pie.addView(hablar, LinearLayout.LayoutParams(-1, diseno.dp(58)))
        diseno.espacio(diseno.pie, 10)
        val fila = LinearLayout(this)
        val limpiar = diseno.boton("Borrar", true).apply {
            setOnClickListener {
                if (texto.text.isNotEmpty()) AlertDialog.Builder(this@TranscripcionActivity)
                    .setTitle("¿Borrar transcripción?").setMessage("El texto que no hayas guardado se perderá.")
                    .setNegativeButton("Cancelar", null).setPositiveButton("Borrar") { _, _ -> detener(); texto.setText("") }.show()
            }
        }
        guardarBoton = diseno.boton("Guardar TXT", true).apply {
            isEnabled = texto.text.isNotBlank()
            setOnClickListener {
                detener()
                if (texto.text.isNotBlank()) guardar.launch("transcripcion.txt")
                else aviso("Todavía no se reconoció texto. Revisa la entrada del micrófono.")
            }
        }
        fila.addView(limpiar, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = diseno.dp(10) })
        fila.addView(guardarBoton, LinearLayout.LayoutParams(0, -2, 1f))
        diseno.pie.addView(fila)
        diseno.pie.addView(diseno.texto("Modelo de voz incluido · Vosk español · Apache 2.0", 11).apply {
            gravity = Gravity.CENTER; setPadding(0, diseno.dp(8), 0, 0)
        })
        texto.addTextChangedListener {
            contador.text = "${it?.length ?: 0} caracteres"
            guardarBoton.isEnabled = !it.isNullOrBlank() || servicio != null
            borrador.edit().putString("texto", it?.toString().orEmpty()).apply()
        }
        contador.text = "${texto.length()} caracteres"
        preparar()
    }
    private fun preparar() {
        if (preparando || modelo != null) return
        preparando = true; hablar.isEnabled = false; reintentar.visibility = View.GONE
        diseno.estado("Preparando la voz incluida. No necesitas descargar archivos…", ocupado = true)
        worker.execute {
            try {
                val ruta = ModelosIncluidos.voz(applicationContext)
                val cargado = Model(ruta.path)
                runOnUiThread {
                    if (destruida) cargado.close()
                    else {
                        modelo = cargado; preparando = false; hablar.isEnabled = true
                        diseno.estado("Listo para escucharte · Sin conexión")
                    }
                }
            } catch (error: Exception) { ui { fallo("No se pudo preparar la voz. Comprueba el espacio libre y vuelve a intentarlo.") } }
            catch (error: LinkageError) { ui { fallo("Este dispositivo no puede abrir el motor de voz incluido.") } }
        }
    }
    private fun iniciar() {
        val actual = modelo ?: return
        try {
            reconocedor = Recognizer(actual, 16000f)
            val nuevo = SpeechService(reconocedor, 16000f)
            servicio = nuevo
            ultimaParcial = ""; palabrasDetectadas = false
            val turno = ++generacion
            val escucha = object : RecognitionListener {
                private fun vigente() = !destruida && turno == generacion
                override fun onPartialResult(h: String) {
                    if (vigente()) {
                        ultimaParcial = frase(h, "partial")
                        if (ultimaParcial.isNotBlank()) {
                            palabrasDetectadas = true
                            parcial.visibility = View.VISIBLE
                            parcial.text = "En curso: $ultimaParcial"
                        }
                    }
                }
                override fun onResult(h: String) { if (!destruida) agregar(h) }
                override fun onFinalResult(h: String) { if (vigente()) { agregar(h); detener(false) } }
                override fun onError(e: Exception) {
                    if (vigente()) { detener(false); diseno.estado("No se pudo escuchar. Revisa el permiso y que el micrófono no esté ocupado.", error = true) }
                }
                override fun onTimeout() { if (vigente()) detener() }
            }
            check(nuevo.startListening(escucha))
            hablar.text = "Detener dictado"
            guardarBoton.isEnabled = true
            parcial.visibility = View.VISIBLE; parcial.text = "Esperando voz…"
            avisos.postDelayed(sinPalabras, 10000)
            diseno.estado("Te estamos escuchando…", ocupado = true)
        } catch (error: Exception) {
            detener(false)
            diseno.estado("No se pudo abrir el micrófono. Comprueba el permiso y vuelve a intentarlo.", error = true)
        }
    }
    private fun detener(recogerFinal: Boolean = true) {
        if (servicio == null && reconocedor == null) return
        generacion++
        avisos.removeCallbacks(sinPalabras)
        servicio?.cancel()
        if (recogerFinal && !destruida) {
            val final = reconocedor?.let { frase(it.finalResult, "text") }.orEmpty()
            agregarFrase(final.ifBlank { ultimaParcial })
        }
        servicio?.shutdown(); servicio = null
        reconocedor?.close(); reconocedor = null
        if (!destruida) {
            hablar.text = "Empezar a dictar"; parcial.visibility = View.GONE
            guardarBoton.isEnabled = texto.text.isNotBlank()
            diseno.estado(if (palabrasDetectadas) "Dictado detenido · Borrador conservado; puedes editar y guardar TXT"
                else "No se reconocieron palabras. Revisa el micrófono; en el emulador debes habilitar la entrada de audio del PC.")
        }
    }
    private fun frase(json: String, campo: String) = runCatching { JSONObject(json).optString(campo) }.getOrDefault("")
    private fun agregar(resultado: String) {
        ultimaParcial = ""
        parcial.visibility = View.GONE
        agregarFrase(frase(resultado, "text"))
    }
    private fun agregarFrase(frase: String) {
        if (frase.isNotBlank()) {
            palabrasDetectadas = true
            texto.append((if (texto.text.isEmpty()) "" else "\n") + frase)
        }
    }
    private fun fallo(mensaje: String) {
        preparando = false; diseno.estado(mensaje, error = true)
        reintentar.visibility = View.VISIBLE; hablar.isEnabled = false
    }
    private fun ui(accion: () -> Unit) = runOnUiThread { if (!destruida) accion() }
    override fun onSaveInstanceState(outState: Bundle) { detener(); outState.putString("texto", texto.text.toString()); super.onSaveInstanceState(outState) }
    override fun onPause() {
        detener()
        if (::texto.isInitialized) borrador.edit().putString("texto", texto.text.toString()).apply()
        super.onPause()
    }
    override fun onDestroy() {
        destruida = true; avisos.removeCallbacksAndMessages(null); detener(false); modelo?.close(); modelo = null; worker.shutdown(); super.onDestroy()
    }
}
