package com.daniel.votacionesyelecciones.modelo

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreHelper {
    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

}