package com.daniel.votacionesyelecciones

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
         
        getSharedPreferences(Notificaciones.PREFERENCIAS, MODE_PRIVATE).edit()
            .putString("token_fcm", token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val titulo = message.notification?.title ?: message.data["titulo"]
            ?: message.data["title"] ?: "Aviso de elecciones"
        val contenido = message.notification?.body ?: message.data["contenido"]
            ?: message.data["body"] ?: "Revisa la información de tus elecciones."
        Notificaciones.mostrar(this, titulo, contenido)
    }
}
