package com.daniel.votacioneselecciones

object CompatibilidadMotor {
    fun usaArmTraducido(abiSistema: String, carpetaNativa: String): Boolean =
        abiSistema.startsWith("x86") && carpetaNativa.substringAfterLast('/').startsWith("arm")
}
