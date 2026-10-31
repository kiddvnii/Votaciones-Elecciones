package com.daniel.votacioneselecciones

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.daniel.votacioneselecciones.databinding.ActivityMenuPrincipalBinding
import com.daniel.votacioneselecciones.modelo.SesionLocal

class MenuPrincipalActivity : PantallaConSesion() {
    private lateinit var binding: ActivityMenuPrincipalBinding
    private val mensajes by lazy { getSharedPreferences(Notificaciones.PREFERENCIAS, MODE_PRIVATE) }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, clave ->
        if (clave == "titulo" || clave == "contenido") runOnUiThread { mostrarUltimoMensaje() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBienvenida.text = getString(R.string.bienvenida, SesionLocal.usuarioActivo.orEmpty())
        binding.cardElecciones.setOnClickListener { startActivity(Intent(this, Elecciones::class.java)) }
        binding.cardCandidatos.setOnClickListener { startActivity(Intent(this, Candidato::class.java)) }
        binding.cardVotaciones.setOnClickListener { startActivity(Intent(this, Votaciones::class.java)) }
        binding.btnCerrarSesion.setOnClickListener {
            SesionLocal.cerrarSesion()
            mensajes.edit().remove("titulo").remove("contenido").apply()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        PruebasApp.configurar(this, binding.panelPruebas)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        mostrarUltimoMensaje()
    }

    override fun onStart() {
        super.onStart()
        mensajes.registerOnSharedPreferenceChangeListener(listener)
        mostrarUltimoMensaje()
    }

    override fun onStop() {
        mensajes.unregisterOnSharedPreferenceChangeListener(listener)
        super.onStop()
    }

    private fun mostrarUltimoMensaje() {
        if (!::binding.isInitialized) return
        val titulo = mensajes.getString("titulo", null)
        val contenido = mensajes.getString("contenido", "").orEmpty()
        binding.txtUltimaNotificacion.text = if (titulo == null) "Sin notificaciones recibidas en primer plano."
            else titulo + "\n" + contenido
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 100 && (results.isEmpty() || results[0] != PackageManager.PERMISSION_GRANTED)) {
            aviso("Las notificaciones están desactivadas. Puedes habilitarlas en Ajustes de Android.")
        }
    }
}
