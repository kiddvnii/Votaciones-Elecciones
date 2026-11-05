package com.daniel.votacionesyelecciones

import android.app.Application
import com.daniel.votacionesyelecciones.modelo.AnalyticsHelper

class Aplicacion : Application() {
    override fun onCreate() {
        super.onCreate()
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
        AppCheckConfiguracion.instalar()
        AnalyticsHelper.inicializar(this)
        Notificaciones.crearCanal(this)
    }
}
