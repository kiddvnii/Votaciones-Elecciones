package com.daniel.votacionesyelecciones.modelo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
object SesionLocal {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val usuarioActivo: String?
        get() = auth.currentUser?.email
    fun validar(
        correo: String,
        clave: String,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(correo.trim(), clave)
            .addOnSuccessListener { onExito() }
            .addOnFailureListener { excepcion ->
                val mensaje = when (excepcion) {
                    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
                    is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo"
                    else -> excepcion.localizedMessage ?: "No se pudo iniciar sesión"
                }
                onError(mensaje)
            }
    }
    fun cerrarSesion() {
        auth.signOut()
    }
    fun haySesionActiva(): Boolean = auth.currentUser != null
}