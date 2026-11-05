package com.daniel.votacionesyelecciones.modelo

data class Eleccion(
    val id: String = "",
    val nombre: String = "",
    val fecha: String = "",
    val estado: String = "Activa",
    val lugar: String = "",
    val mesas: Int = 1,
    val ownerUid: String = "",
    val fechaTermino: String = ""
)
