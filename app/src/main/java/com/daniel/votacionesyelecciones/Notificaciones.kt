package com.daniel.votacionesyelecciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notificaciones {
    const val CANAL = "votaciones_channel"
    const val PREFERENCIAS = "mensajeria"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Avisos de elecciones", NotificationManager.IMPORTANCE_DEFAULT)
            canal.description = "Información sobre elecciones y candidatos de prueba."
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun mostrar(context: Context, titulo: String, contenido: String) {
         
        context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE).edit()
            .putString("titulo", titulo).putString("contenido", contenido).apply()
        crearCanal(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val abrir = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion).setContentTitle(titulo)
            .setContentText(contenido).setStyle(NotificationCompat.BigTextStyle().bigText(contenido))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setContentIntent(abrir)
            .setAutoCancel(true).build()
        try {
            manager.notify(System.currentTimeMillis().toInt(), notificacion)
        } catch (_: SecurityException) {
             
        }
    }
}
