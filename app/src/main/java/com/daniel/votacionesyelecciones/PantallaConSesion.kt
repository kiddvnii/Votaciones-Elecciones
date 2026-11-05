package com.daniel.votacionesyelecciones

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.daniel.votacionesyelecciones.modelo.SesionLocal
import com.daniel.votacionesyelecciones.modelo.AnalyticsHelper
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import android.util.Log

abstract class PantallaConSesion : AppCompatActivity() {
    private var carga: AlertDialog? = null
    protected var ocupada = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        comprobarSesion()
    }

    override fun onResume() {
        super.onResume()
        comprobarSesion()
        if (!isFinishing) {
            val pantalla = when (this) {
                is MenuPrincipalActivity -> "menu_principal"
                is Elecciones -> "elecciones"
                is Candidato -> "candidatos"
                is Votaciones -> "votaciones"
                else -> null
            }
            pantalla?.let { AnalyticsHelper.pantalla(it) }
        }
    }

    private fun comprobarSesion() {
        if (!SesionLocal.haySesionActiva()) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    protected fun aviso(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    protected fun <T> ejecutar(
        mensaje: String,
        tarea: () -> Task<T>,
        reintentar: (() -> Unit)? = null,
        alExito: (T) -> Unit
    ) {
        if (ocupada || isFinishing || isDestroyed) return
        ocupada = true
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val margen = (24 * resources.displayMetrics.density).toInt()
            setPadding(margen, margen, margen, margen)
            addView(ProgressBar(context))
            addView(TextView(context).apply {
                text = mensaje
                setPadding(margen, 0, 0, 0)
            })
        }
        carga = AlertDialog.Builder(this).setView(panel).setCancelable(false).show()
        try {
            tarea().addOnCompleteListener { resultado ->
                carga?.dismiss()
                carga = null
                ocupada = false
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                if (resultado.isSuccessful) alExito(resultado.result)
                else informarError(resultado.exception ?: IllegalStateException("No se pudo completar la operación."), reintentar, mensaje)
            }
        } catch (error: Exception) {
            carga?.dismiss()
            carga = null
            ocupada = false
            informarError(error, reintentar, mensaje)
        }
    }

    private fun informarError(error: Exception, reintentar: (() -> Unit)?, operacion: String) {
        Log.e("OperacionFirebase", "$operacion Falló la operación.", error)
        val mensaje = ErroresOperacion.mensaje(error)
        if (reintentar == null) aviso(mensaje)
        else Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_INDEFINITE)
            .setAction("Reintentar") { reintentar() }.show()
    }

    override fun onDestroy() {
        carga?.dismiss()
        carga = null
        super.onDestroy()
    }
}
