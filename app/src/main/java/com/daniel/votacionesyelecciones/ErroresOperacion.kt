package com.daniel.votacionesyelecciones

import com.google.firebase.firestore.FirebaseFirestoreException

internal object ErroresOperacion {
    fun mensaje(error: Throwable): String {
        val causas = generateSequence(error) { it.cause }.take(20).toList()
        val firestore = causas.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestore != null) {
            val detalle = when (firestore.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Firebase rechazó el acceso. Revisa las reglas y el propietario de los registros."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Tu sesión no fue aceptada. Cierra sesión y vuelve a entrar."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Firebase no está disponible. Comprueba la conexión y vuelve a intentar."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Firebase tardó demasiado en responder. Vuelve a intentar."
                FirebaseFirestoreException.Code.ABORTED ->
                    "Los datos cambiaron durante la operación. Actualiza y vuelve a intentar."
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    "Se alcanzó un límite de Firebase. Intenta más tarde."
                else -> "Firebase no pudo completar la operación."
            }
            return "$detalle [${firestore.code.name}]"
        }
        val validacion = causas.firstOrNull { it is IllegalArgumentException || it is IllegalStateException }
        if (validacion != null) return validacion.message ?: "Revisa los datos ingresados."
        return "No se pudo completar la operación [${causas.last().javaClass.simpleName}]."
    }
}
