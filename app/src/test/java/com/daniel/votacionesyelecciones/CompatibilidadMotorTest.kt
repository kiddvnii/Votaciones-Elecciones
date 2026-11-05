package com.daniel.votacionesyelecciones

import org.junit.Assert.*
import org.junit.Test

class CompatibilidadMotorTest {
    @Test fun detectaArmTraducidoEnEmuladorX86() {
        assertTrue(CompatibilidadMotor.usaArmTraducido("x86_64", "/data/app/paquete/lib/arm64"))
    }
    @Test fun permiteX86Nativo() {
        assertFalse(CompatibilidadMotor.usaArmTraducido("x86_64", "/data/app/paquete/lib/x86_64"))
    }
    @Test fun permiteTelefonoArm64Nativo() {
        assertFalse(CompatibilidadMotor.usaArmTraducido("arm64-v8a", "/data/app/paquete/lib/arm64"))
    }
}
