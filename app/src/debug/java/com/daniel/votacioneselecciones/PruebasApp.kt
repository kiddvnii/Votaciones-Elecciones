package com.daniel.votacioneselecciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.daniel.votacioneselecciones.databinding.PanelPruebasBinding
import com.google.firebase.crashlytics.FirebaseCrashlytics

 
object PruebasApp {
    fun configurar(activity: AppCompatActivity, contenedor: ViewGroup) {
        val panel = PanelPruebasBinding.inflate(LayoutInflater.from(activity), contenedor, true)
        contenedor.visibility = View.VISIBLE
        panel.btnErrorNoFatal.setOnClickListener {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("pantalla", "menu_principal")
            crashlytics.setCustomKey("tipo_prueba", "no_fatal")
            try {
                throw IllegalStateException("Error no fatal de prueba desde el menú")
            } catch (error: IllegalStateException) {
                crashlytics.log("Prueba controlada: la aplicación sigue funcionando.")
                crashlytics.recordException(error)
                Toast.makeText(activity, "Error no fatal registrado. La app sigue funcionando.", Toast.LENGTH_LONG).show()
            }
        }
        panel.btnProbarCierre.setOnClickListener {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("pantalla", "menu_principal")
                setCustomKey("tipo_prueba", "fatal")
                log("El usuario pulsó Provocar cierre de prueba.")
            }
             
            throw RuntimeException("Error intencional de Crashlytics al pulsar el botón del menú")
        }
    }
}
