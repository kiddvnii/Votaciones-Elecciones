package com.daniel.votacionesyelecciones

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.daniel.votacionesyelecciones.databinding.ActivityMainBinding
import com.daniel.votacionesyelecciones.modelo.SesionLocal
import com.daniel.votacionesyelecciones.modelo.AnalyticsHelper

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("prefs_login", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SesionLocal.haySesionActiva()) { abrirMenu(); return }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val guardado = prefs.getString("usuario_recordado", "")
        if (!guardado.isNullOrBlank()) {
            binding.edtUsuario.setText(guardado)
            binding.chkRecordar.isChecked = true
        }
        binding.btnIngresar.setOnClickListener { intentarIngresar() }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) AnalyticsHelper.pantalla("login")
    }

    private fun intentarIngresar() {
        val usuario = binding.edtUsuario.text.toString().trim()
        val clave = binding.edtClave.text.toString()
        if (!Patterns.EMAIL_ADDRESS.matcher(usuario).matches() || clave.isBlank()) {
            Toast.makeText(this, "Ingresa un correo válido y tu contraseña.", Toast.LENGTH_LONG).show()
            return
        }
        if (!binding.btnIngresar.isEnabled) return
        binding.btnIngresar.isEnabled = false
        binding.btnIngresar.text = "Ingresando…"
        SesionLocal.validar(usuario, clave, onExito = {
            if (isFinishing || isDestroyed) return@validar
            prefs.edit().apply {
                if (binding.chkRecordar.isChecked) putString("usuario_recordado", usuario)
                else remove("usuario_recordado")
                apply()
            }
            abrirMenu()
        }, onError = { mensaje ->
            if (!isFinishing && !isDestroyed) {
                binding.btnIngresar.isEnabled = true
                binding.btnIngresar.setText(R.string.accion_ingresar)
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun abrirMenu() {
        startActivity(Intent(this, MenuPrincipalActivity::class.java))
        finish()
    }
}
