package com.daniel.votacioneselecciones

object CompatibilidadMotor {
    // An x86 emulator must load the x86_64 library, not translated ARM machine code.
    fun usaArmTraducido(abiSistema: String, carpetaNativa: String): Boolean =
        abiSistema.startsWith("x86") && carpetaNativa.substringAfterLast('/').startsWith("arm")
}
