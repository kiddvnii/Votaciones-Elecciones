package com.daniel.votacionesyelecciones.modelo

data class RegistroVoto(
    val id: String = "",
    val eleccionId: String = "",
    val candidatoId: String = "",
    val fechaHora: String = "",
    val creadoEn: Long = 0,
    val ownerUid: String = ""
)
