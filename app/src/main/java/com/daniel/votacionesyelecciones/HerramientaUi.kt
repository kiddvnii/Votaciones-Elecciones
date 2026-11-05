package com.daniel.votacionesyelecciones

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
class HerramientaUi(private val activity: AppCompatActivity, titulo: String, subtitulo: String) {
    val azul = Color.rgb(27, 58, 107)
    val gris = Color.rgb(100, 116, 139)
    private val fondo = Color.rgb(243, 245, 249)
    val raiz = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(fondo) }
    val contenido = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(24)) }
    val scroll = ScrollView(activity).apply { isFillViewport = true; clipToPadding = false; addView(contenido) }
    val estado = texto("Preparando…", 14, azul)
    val progreso = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = true }
    val pie = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(12)); setBackgroundColor(Color.WHITE)
    }
    init {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        barrasTransparentes()
        val barra = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE); setPadding(dp(8), dp(8), dp(20), dp(12))
        }
        barra.addView(ImageButton(activity).apply {
            setImageResource(R.drawable.ic_atras); imageTintList = ColorStateList.valueOf(azul)
            setBackgroundColor(Color.TRANSPARENT); contentDescription = "Volver"
            setOnClickListener { activity.finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        barra.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto(titulo, 20, azul, true)); addView(texto(subtitulo, 12, gris))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        raiz.addView(barra)
        raiz.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        raiz.addView(pie)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, maxOf(barras.bottom, teclado.bottom))
            insets
        }
        activity.setContentView(raiz)
        WindowCompat.getInsetsController(activity.window, raiz).apply {
            isAppearanceLightStatusBars = true; isAppearanceLightNavigationBars = true
        }
        ViewCompat.requestApplyInsets(raiz)
    }
    @Suppress("DEPRECATION")
    private fun barrasTransparentes() {
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = if (android.os.Build.VERSION.SDK_INT >= 26) Color.TRANSPARENT else azul
        if (android.os.Build.VERSION.SDK_INT >= 29) activity.window.isNavigationBarContrastEnforced = false
    }
    fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()
    fun texto(valor: String, tamano: Int = 15, color: Int = gris, negrita: Boolean = false) = TextView(activity).apply {
        text = valor; textSize = tamano.toFloat(); setTextColor(color)
        if (negrita) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    fun fondo(color: Int, radio: Int = 20, borde: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radio).toFloat(); borde?.let { setStroke(dp(1), it) }
    }
    fun tarjeta(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; background = fondo(Color.WHITE)
        setPadding(dp(20), dp(20), dp(20), dp(20))
    }
    fun agregar(vista: View, margen: Int = 16) {
        contenido.addView(vista, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margen) })
    }
    fun espacio(panel: LinearLayout, alto: Int = 12) { panel.addView(Space(activity), LinearLayout.LayoutParams(1, dp(alto))) }
    fun boton(titulo: String, secundario: Boolean = false): MaterialButton = MaterialButton(activity).apply {
        text = titulo; isAllCaps = false; textSize = 15f; cornerRadius = dp(14)
        minHeight = dp(52); insetTop = 0; insetBottom = 0
        backgroundTintList = ColorStateList.valueOf(if (secundario) Color.WHITE else azul)
        setTextColor(if (secundario) azul else Color.WHITE)
        if (secundario) { strokeWidth = dp(1); strokeColor = ColorStateList.valueOf(Color.rgb(207, 217, 230)) }
    }
    fun cabecera(icono: Int, etiqueta: String, titulo: String, descripcion: String) {
        val card = tarjeta().apply { background = fondo(azul); setPadding(dp(24), dp(24), dp(24), dp(24)) }
        card.addView(ImageView(activity).apply {
            setImageResource(icono); imageTintList = ColorStateList.valueOf(Color.rgb(242, 193, 83))
            contentDescription = null
        }, LinearLayout.LayoutParams(dp(40), dp(40)))
        espacio(card, 16); card.addView(texto(etiqueta, 11, Color.rgb(199, 217, 244), true))
        espacio(card, 6); card.addView(texto(titulo, 27, Color.WHITE, true))
        espacio(card, 10); card.addView(texto(descripcion, 14, Color.rgb(224, 233, 248)))
        agregar(card)
    }
    fun estadoInicial() {
        val card = tarjeta()
        card.addView(texto("EN ESTE DISPOSITIVO", 11, gris, true)); espacio(card, 8)
        estado.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        card.addView(estado); espacio(card, 10)
        card.addView(progreso, LinearLayout.LayoutParams(-1, dp(4)))
        agregar(card)
    }
    fun estado(mensaje: String, ocupado: Boolean = false, error: Boolean = false) {
        estado.text = mensaje
        estado.setTextColor(if (error) Color.rgb(170, 47, 47) else azul)
        progreso.visibility = if (ocupado) View.VISIBLE else View.GONE
    }
}
