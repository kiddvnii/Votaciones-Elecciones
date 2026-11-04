package com.daniel.votacioneselecciones

import android.app.Application
import com.daniel.votacioneselecciones.modelo.AnalyticsHelper

class Aplicacion : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCheckConfiguracion.instalar()
        AnalyticsHelper.inicializar(this)
        Notificaciones.crearCanal(this)
    }
}
