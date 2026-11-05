package com.daniel.votacioneselecciones

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.util.concurrent.Executors

class TranscripcionActivity : PantallaConSesion(), RecognitionListener {
    private lateinit var texto: EditText
    private lateinit var estado: TextView
    private lateinit var hablar: Button
    private lateinit var importar: Button
    private var modelo: Model? = null
    private var servicio: SpeechService? = null
    private var reconocedorActual: Recognizer? = null
    private var generacion = 0
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var destruida = false
    private val preferencias by lazy { getSharedPreferences("modelos-locales", MODE_PRIVATE) }
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
        if (permitido) iniciar() else estado.text = "Micrófono denegado. Puedes conceder permiso al volver a pulsar Hablar o desde Ajustes."
    }
    private val selector = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            detener()
            importar.isEnabled = false; hablar.isEnabled = false
            estado.text = "Importando y comprobando modelo Vosk…"
            worker.execute {
                val staging = File(filesDir, "vosk-${java.util.UUID.randomUUID()}")
                var nuevo: Model? = null
                try {
                    val carpeta = contentResolver.openInputStream(uri)?.use { ModelosLocales.extraerVosk(it, staging) }
                        ?: error("No se pudo leer el ZIP")
                    nuevo = Model(carpeta.path)
                    val cargado = nuevo
                    runOnUiThread {
                        if (destruida) { cargado.close(); staging.deleteRecursively() }
                        else {
                            modelo?.close()
                            modelo = cargado
                            val anterior = preferencias.getString("vosk", null)
                            preferencias.edit().putString("vosk", carpeta.path).apply()
                            // Old imports are private directories created by this screen only.
                            if (anterior != null) worker.execute { eliminarImportacion(anterior) }
                            listo("Modelo Vosk listo. El audio se procesa sin conexión.")
                        }
                    }
                } catch (error: Exception) {
                    nuevo?.close(); staging.deleteRecursively()
                    ui { listo("No se pudo importar. Usa el ZIP original de un modelo Vosk pequeño; comprueba el espacio libre.") }
                } catch (error: LinkageError) {
                    staging.deleteRecursively()
                    ui { listo("Vosk no es compatible con este dispositivo.") }
                }
            }
        }
    }
    private val guardar = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val contenido = texto.text.toString()
            worker.execute {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(contenido.toByteArray(Charsets.UTF_8)) }
                        ?: error("No se pudo crear el archivo")
                    ui { aviso("Texto guardado correctamente.") }
                } catch (error: Exception) { ui { aviso("No se pudo guardar el archivo.") } }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Voz a texto local"
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        panel.addView(TextView(this).apply {
            text = "Vosk · reconocimiento offline\nDescarga vosk-model-small-es-0.42 desde alphacephei.com/vosk/models e importa el ZIP original (39 MB, Apache 2.0). El modelo no viene incluido. Puedes editar y guardar lo reconocido."
        })
        estado = TextView(this); panel.addView(estado)
        importar = Button(this).apply { text = "Importar modelo Vosk ZIP"; setOnClickListener { selector.launch(arrayOf("*/*")) } }
        panel.addView(importar)
        texto = EditText(this).apply {
            hint = "Transcripción editable"; gravity = android.view.Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(savedInstanceState?.getString("texto").orEmpty())
        }
        panel.addView(texto, LinearLayout.LayoutParams(-1, 0, 1f))
        hablar = Button(this).apply {
            text = "Hablar"; isEnabled = false
            setOnClickListener {
                if (servicio != null) detener()
                else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) iniciar()
                else permiso.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        panel.addView(hablar)
        panel.addView(Button(this).apply {
            text = "Guardar TXT"; setOnClickListener {
                if (texto.text.isBlank()) aviso("Primero transcribe o escribe texto.") else guardar.launch("transcripcion.txt")
            }
        })
        setContentView(panel)
        val ruta = preferencias.getString("vosk", null)
        if (ruta == null) listo("Falta el modelo Vosk. Importa un ZIP para habilitar el micrófono.")
        else {
            importar.isEnabled = false; estado.text = "Cargando Vosk…"
            worker.execute {
                try {
                    val cargado = Model(ruta)
                    runOnUiThread {
                        if (destruida) cargado.close()
                        else { modelo = cargado; listo("Vosk listo para transcribir sin conexión.") }
                    }
                } catch (error: Exception) { ui { listo("Modelo no disponible o inválido. Importa otro ZIP.") } }
                catch (error: LinkageError) { ui { listo("Motor Vosk incompatible con este dispositivo.") } }
            }
        }
    }

    private fun iniciar() {
        val actual = modelo ?: return
        var reconocedor: Recognizer? = null
        try {
            reconocedor = Recognizer(actual, 16000f)
            val nuevo = SpeechService(reconocedor, 16000f)
            servicio = nuevo
            reconocedorActual = reconocedor
            val turno = ++generacion
            val escucha = object : RecognitionListener {
                private fun vigente() = !destruida && turno == generacion
                override fun onPartialResult(h: String) { if (vigente()) this@TranscripcionActivity.onPartialResult(h) }
                override fun onResult(h: String) { if (!destruida) this@TranscripcionActivity.onResult(h) }
                override fun onFinalResult(h: String) { if (vigente()) this@TranscripcionActivity.onFinalResult(h) }
                override fun onError(e: Exception) { if (vigente()) this@TranscripcionActivity.onError(e) }
                override fun onTimeout() { if (vigente()) this@TranscripcionActivity.onTimeout() }
            }
            check(nuevo.startListening(escucha))
            importar.isEnabled = false; hablar.text = "Detener"
            estado.text = "Escuchando… Habla en español."
        } catch (error: Exception) {
            if (servicio == null) reconocedor?.close()
            detener(); estado.text = "No se pudo iniciar el micrófono. Comprueba el permiso y que no esté ocupado."
        }
    }
    private fun detener() {
        generacion++
        servicio?.cancel()
        if (!destruida) reconocedorActual?.let { agregar(it.finalResult) }
        servicio?.shutdown(); servicio = null
        reconocedorActual?.close(); reconocedorActual = null
        if (::hablar.isInitialized) { hablar.text = "Hablar"; importar.isEnabled = true; estado.text = "Grabación detenida." }
    }
    private fun listo(mensaje: String) {
        estado.text = mensaje; importar.isEnabled = true; hablar.isEnabled = modelo != null
    }
    private fun agregar(resultado: String) {
        val frase = runCatching { JSONObject(resultado).optString("text") }.getOrDefault("")
        if (frase.isNotBlank()) texto.append((if (texto.text.isEmpty()) "" else "\n") + frase)
    }
    override fun onPartialResult(hypothesis: String) {
        if (!destruida && servicio != null) estado.text = runCatching { JSONObject(hypothesis).optString("partial") }.getOrDefault("Escuchando…")
    }
    override fun onResult(hypothesis: String) { if (!destruida) agregar(hypothesis) }
    override fun onFinalResult(hypothesis: String) { if (!destruida) { agregar(hypothesis); reconocedorActual?.reset(); detener() } }
    override fun onError(exception: Exception) { if (!destruida) { detener(); estado.text = "Falló la transcripción. Puedes volver a intentarlo." } }
    override fun onTimeout() { if (!destruida) detener() }
    private fun ui(accion: () -> Unit) = runOnUiThread { if (!destruida) accion() }
    private fun eliminarImportacion(ruta: String) {
        var carpeta = File(ruta).canonicalFile
        val raiz = filesDir.canonicalFile
        while (carpeta.parentFile != null && carpeta.parentFile != raiz) carpeta = carpeta.parentFile!!
        if (carpeta.parentFile == raiz && carpeta.name.startsWith("vosk-")) carpeta.deleteRecursively()
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("texto", texto.text.toString()); super.onSaveInstanceState(outState) }
    override fun onPause() { detener(); super.onPause() }
    override fun onDestroy() {
        destruida = true; detener(); modelo?.close(); modelo = null
        worker.shutdown(); super.onDestroy()
    }
}
