package com.daniel.votacionesyelecciones

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.ai.edge.litertlm.*
import java.util.concurrent.Executors

class AsistenteActivity : PantallaConSesion() {
    companion object { private val worker = Executors.newSingleThreadExecutor() }
    @Volatile private var destruida = false
    private var engine: Engine? = null
    private var conversacion: Conversation? = null
    private lateinit var diseno: HerramientaUi
    private lateinit var mensajes: LinearLayout
    private lateinit var sugerencias: LinearLayout
    private lateinit var mensaje: EditText
    private lateinit var enviar: Button
    private lateinit var reiniciar: Button
    private lateinit var reintentar: Button
    private var ocupadaIa = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        diseno = HerramientaUi(this, "Asistente electoral", "Inteligencia artificial en tu teléfono")
        diseno.cabecera(R.drawable.ic_asistente, "PRIVADO · SIN CONEXIÓN", "Conversemos.",
            "Resuelve dudas y prepara tus ideas. Tus mensajes se procesan en este dispositivo.")
        diseno.estadoInicial()
        reintentar = diseno.boton("Volver a preparar", true).apply {
            visibility = View.GONE; setOnClickListener { preparar() }
        }
        diseno.agregar(reintentar)
        sugerencias = diseno.tarjeta().apply {
            addView(diseno.texto("¿Por dónde empezamos?", 18, diseno.azul, true))
            diseno.espacio(this, 6)
            addView(diseno.texto("Elige una idea o escribe tu propia pregunta.", 14))
            for (pregunta in listOf("¿Qué es una elección?", "Ayúdame a redactar una propuesta", "¿Cómo organizar una votación?")) {
                diseno.espacio(this, 10)
                addView(diseno.boton(pregunta, true).apply {
                    setOnClickListener { mensaje.setText(pregunta); mensaje.requestFocus() }
                }, LinearLayout.LayoutParams(-1, -2))
            }
        }
        diseno.agregar(sugerencias)
        mensajes = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        diseno.agregar(mensajes, 4)
        diseno.agregar(diseno.texto("Puede equivocarse. Verifica la información importante. No accede a tus votos ni a resultados en tiempo real.", 12))
        val caja = diseno.tarjeta().apply {
            background = diseno.fondo(Color.rgb(243, 245, 249), 16, Color.rgb(219, 225, 234))
            setPadding(diseno.dp(14), diseno.dp(4), diseno.dp(14), diseno.dp(4))
        }
        mensaje = EditText(this).apply {
            hint = "Escribe tu pregunta…"; textSize = 16f; minLines = 1; maxLines = 4
            setTextColor(diseno.azul); setHintTextColor(diseno.gris)
            background = null; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(savedInstanceState?.getString("borrador").orEmpty())
        }
        caja.addView(mensaje, LinearLayout.LayoutParams(-1, -2))
        diseno.pie.addView(caja)
        diseno.espacio(diseno.pie, 10)
        val fila = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        reiniciar = diseno.boton("Nueva", true).apply { isEnabled = false; setOnClickListener { nueva() } }
        enviar = diseno.boton("Enviar pregunta").apply { isEnabled = false; setOnClickListener { consultar() } }
        fila.addView(reiniciar, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = diseno.dp(10) })
        fila.addView(enviar, LinearLayout.LayoutParams(0, -2, 2f))
        diseno.pie.addView(fila)
        val creditos = diseno.texto("Modelo incluido · Qwen3 0.6B · Licencias", 11).apply {
            gravity = Gravity.CENTER; setPadding(0, diseno.dp(8), 0, 0)
            setOnClickListener { licencias() }
        }
        diseno.pie.addView(creditos)
        preparar()
    }
    private fun preparar() {
        if (ocupadaIa) return
        if (CompatibilidadMotor.usaArmTraducido(android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty(), applicationInfo.nativeLibraryDir)) {
            fallo("Esta instalación usa ARM traducido en un emulador x86. Instala la versión 1.4.1 o posterior con soporte x86_64 nativo.")
            return
        }
        ocupado("Preparando el asistente incluido. La primera vez puede tardar un momento…")
        reintentar.visibility = View.GONE
        worker.execute {
            try {
                cerrarModelo()
                val modelo = ModelosIncluidos.chat(applicationContext) { mb ->
                    ui { diseno.estado("Preparando sin descargar: $mb MB copiados…", ocupado = true) }
                }
                ui { diseno.estado("Abriendo el asistente…", ocupado = true) }
                engine = Engine(EngineConfig(modelPath = modelo.path, backend = Backend.CPU(),
                    cacheDir = cacheDir.path, maxNumTokens = 2048))
                engine!!.initialize()
                conversacion = crearConversacion()
                ui { disponible("Listo para conversar · Sin conexión") }
            } catch (error: Exception) {
                cerrarModelo()
                ui { fallo("No se pudo preparar. Comprueba que tienes al menos 1 GB libre para la copia del modelo y memoria disponible.") }
            } catch (error: LinkageError) {
                ui { fallo("Este dispositivo no puede abrir el motor de IA incluido.") }
            }
        }
    }
    private fun crearConversacion() = checkNotNull(engine).createConversation(ConversationConfig(
        systemInstruction = Contents.of("Eres un asistente de una app educativa de elecciones. Responde en español, de forma breve y clara. No tienes acceso a registros de votos, internet ni datos en tiempo real. Si no sabes algo, dilo. No inventes resultados ni instrucciones oficiales."),
        extraContext = mapOf("enable_thinking" to false),
        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0),
        maxOutputToken = 256
    ))
    private fun nueva() {
        if (ocupadaIa) return
        ocupado("Iniciando una conversación nueva…")
        worker.execute {
            try {
                conversacion?.close(); conversacion = null
                conversacion = crearConversacion()
                ui { mensajes.removeAllViews(); sugerencias.visibility = View.VISIBLE; disponible("Conversación nueva · Sin conexión") }
            } catch (error: Exception) { ui { fallo("No se pudo reiniciar. Vuelve a preparar el asistente.") } }
        }
    }
    private fun consultar() {
        val pregunta = mensaje.text.toString().trim()
        if (pregunta.isEmpty() || !enviar.isEnabled) return
        if (pregunta.length > 1500) { aviso("Acorta tu consulta a 1500 caracteres."); return }
        agregarMensaje("Tú", pregunta, true)
        sugerencias.visibility = View.GONE; mensaje.setText("")
        ocupado("El asistente está escribiendo…")
        worker.execute {
            try {
                val respuesta = checkNotNull(conversacion).sendMessage(pregunta).toString().trim()
                check(respuesta.isNotEmpty())
                ui { agregarMensaje("Asistente", respuesta, false); disponible("Listo para continuar · Sin conexión") }
            } catch (error: Exception) {
                ui {
                    agregarMensaje("Aviso", "No pude responder. Pulsa Nueva para reiniciar la conversación y vuelve a intentarlo.", false)
                    disponible("La conversación puede haber alcanzado su límite. Pulsa Nueva.")
                }
            }
        }
    }
    private fun agregarMensaje(autor: String, contenido: String, propio: Boolean) {
        val burbuja = diseno.tarjeta().apply {
            background = diseno.fondo(if (propio) Color.rgb(224, 234, 249) else Color.WHITE, 18)
            addView(diseno.texto(autor, 12, diseno.azul, true)); diseno.espacio(this, 6)
            addView(diseno.texto(contenido, 16, Color.rgb(30, 41, 59)).apply { setTextIsSelectable(true) })
        }
        mensajes.addView(burbuja, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = diseno.dp(12)
            if (propio) marginStart = diseno.dp(32) else marginEnd = diseno.dp(24)
        })
        diseno.scroll.post { diseno.scroll.fullScroll(View.FOCUS_DOWN) }
    }
    private fun ocupado(texto: String) {
        ocupadaIa = true; diseno.estado(texto, ocupado = true)
        enviar.isEnabled = false; reiniciar.isEnabled = false; reintentar.isEnabled = false
    }
    private fun disponible(texto: String) {
        ocupadaIa = false; diseno.estado(texto)
        enviar.isEnabled = conversacion != null; reiniciar.isEnabled = conversacion != null
        reintentar.isEnabled = true
    }
    private fun fallo(texto: String) {
        ocupadaIa = false; diseno.estado(texto, error = true)
        enviar.isEnabled = false; reiniciar.isEnabled = false
        reintentar.isEnabled = true; reintentar.visibility = View.VISIBLE
    }
    private fun licencias() {
        val texto = assets.open("licencias/AVISOS_MODELOS.txt").bufferedReader().use { it.readText() }
        AlertDialog.Builder(this).setTitle("Modelos y licencias").setMessage(texto)
            .setPositiveButton("Cerrar", null).setNeutralButton("Apache 2.0") { _, _ ->
                val licencia = assets.open("modelos/APACHE-2.0.txt").bufferedReader().use { it.readText() }
                AlertDialog.Builder(this).setTitle("Apache License 2.0").setMessage(licencia).setPositiveButton("Cerrar", null).show()
            }.show()
    }
    private fun ui(accion: () -> Unit) = runOnUiThread { if (!destruida) accion() }
    private fun cerrarModelo() {
        conversacion?.close(); conversacion = null
        engine?.let { if (it.isInitialized()) it.close() }; engine = null
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("borrador", mensaje.text.toString()); super.onSaveInstanceState(outState) }
    override fun onDestroy() {
        destruida = true; worker.execute { cerrarModelo() }; super.onDestroy()
    }
}
