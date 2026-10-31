package com.daniel.votacioneselecciones

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.daniel.votacioneselecciones.adaptadores.EleccionAdapter
import com.daniel.votacioneselecciones.databinding.ActivityEleccionesBinding
import com.daniel.votacioneselecciones.databinding.DialogEleccionBinding
import com.daniel.votacioneselecciones.modelo.Eleccion
import com.daniel.votacioneselecciones.modelo.RepositorioDatos
import com.daniel.votacioneselecciones.modelo.Validaciones

class Elecciones : PantallaConSesion() {
    private lateinit var binding: ActivityEleccionesBinding
    private lateinit var adaptador: EleccionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEleccionesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAtras.setOnClickListener { finish() }
        adaptador = EleccionAdapter(emptyList(), ::mostrarDialogoEleccion, ::confirmarEliminarEleccion)
        binding.recyclerElecciones.layoutManager = LinearLayoutManager(this)
        binding.recyclerElecciones.adapter = adaptador
        binding.fabAgregar.setOnClickListener { mostrarDialogoEleccion(null) }
    }

    override fun onResume() {
        super.onResume()
        actualizarLista()
    }

    private fun actualizarLista() {
        ejecutar("Consultando elecciones…", { RepositorioDatos.obtenerElecciones() }, ::actualizarLista) { lista ->
            adaptador.actualizarDatos(lista)
            binding.recyclerElecciones.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
            binding.estadoVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun mostrarDialogoEleccion(existente: Eleccion?) {
        if (ocupada) return
        val campos = DialogEleccionBinding.inflate(layoutInflater)
        val id = existente?.id ?: RepositorioDatos.nuevoId("elecciones")
        existente?.let {
            campos.edtNombreEleccion.setText(it.nombre)
            campos.edtFechaEleccion.setText(it.fecha)
            campos.edtLugarEleccion.setText(it.lugar)
            campos.edtMesasEleccion.setText(it.mesas.toString())
            campos.radioCerrada.isChecked = it.estado == "Cerrada"
            campos.radioActiva.isChecked = it.estado != "Cerrada"
        }
        val dialogo = AlertDialog.Builder(this)
            .setTitle(if (existente == null) R.string.agregar_eleccion else R.string.editar_eleccion)
            .setView(campos.root)
            .setPositiveButton(R.string.accion_guardar, null)
            .setNegativeButton(R.string.accion_cancelar, null).create()
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (ocupada) return@setOnClickListener
                val mesas = Validaciones.enteroEnRango(campos.edtMesasEleccion.text.toString(), 1..1000)
                if (mesas == null) {
                    campos.edtMesasEleccion.error = "Ingresa un entero entre 1 y 1000."
                    return@setOnClickListener
                }
                val eleccion = Eleccion(
                    id = id, nombre = campos.edtNombreEleccion.text.toString().trim(),
                    fecha = campos.edtFechaEleccion.text.toString().trim(),
                    estado = if (campos.radioCerrada.isChecked) "Cerrada" else "Activa",
                    lugar = campos.edtLugarEleccion.text.toString().trim(), mesas = mesas
                )
                val error = Validaciones.eleccion(eleccion)
                if (error != null) { aviso(error); return@setOnClickListener }
                ejecutar("Guardando elección…", { RepositorioDatos.guardarEleccion(eleccion, existente == null) }) {
                    dialogo.dismiss()
                    aviso(if (existente == null) "Elección creada correctamente." else "Elección actualizada.")
                    actualizarLista()
                }
            }
        }
        dialogo.show()
    }

    private fun confirmarEliminarEleccion(eleccion: Eleccion) {
        if (ocupada) return
        AlertDialog.Builder(this).setTitle(R.string.confirmar_eliminar_titulo)
            .setMessage("Se eliminará esta elección junto con sus candidatos y votos. ¿Deseas continuar?")
            .setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_eliminar) { _, _ ->
                ejecutar("Eliminando elección…", { RepositorioDatos.eliminarEleccion(eleccion.id) }) {
                    aviso("Elección y registros asociados eliminados.")
                    actualizarLista()
                }
            }.show()
    }
}
