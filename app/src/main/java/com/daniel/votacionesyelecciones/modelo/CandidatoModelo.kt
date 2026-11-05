package com.daniel.votacionesyelecciones.modelo

data class CandidatoModelo(
    val id: String = "",
    val nombre: String = "",
    val lista: String = "",
    val eleccionId: String = "",
    val edad: Int = 18,
    val cargo: String = "",
    val propuesta: String = "",
    val ownerUid: String = ""
)
