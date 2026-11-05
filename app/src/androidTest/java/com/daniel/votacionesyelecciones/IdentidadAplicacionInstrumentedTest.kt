package com.daniel.votacionesyelecciones

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdentidadAplicacionInstrumentedTest {
    @Test fun conservaIdentificadorDeInstalacionConElPaqueteNuevo() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.daniel.votacioneselecciones", contexto.packageName)
        assertEquals("com.daniel.votacionesyelecciones", BuildConfig::class.java.`package`?.name)
    }
}
