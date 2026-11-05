package com.daniel.votacionesyelecciones.modelo

import android.content.Context
import android.os.Bundle
import com.daniel.votacionesyelecciones.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {
    private lateinit var analytics: FirebaseAnalytics

    fun inicializar(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context.applicationContext)
        analytics.logEvent("app_iniciada", Bundle().apply {
            putString("version_app", BuildConfig.VERSION_NAME)
        })
    }

    fun pantalla(nombre: String) {
        require(nombre in setOf("login", "menu_principal", "elecciones", "candidatos", "votaciones"))
        analytics.logEvent("pantalla_consultada", Bundle().apply { putString("pantalla", nombre) })
    }

     
    fun registrar(evento: String, tipoEntidad: String) {
        require(evento in setOf("registro_creado", "registro_actualizado", "registro_eliminado", "listado_consultado"))
        require(tipoEntidad in setOf("eleccion", "candidato"))
        analytics.logEvent(evento, Bundle().apply { putString("tipo_entidad", tipoEntidad) })
    }
}
