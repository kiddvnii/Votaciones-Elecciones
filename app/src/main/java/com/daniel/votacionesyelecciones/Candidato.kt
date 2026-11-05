package com.daniel.votacionesyelecciones

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.daniel.votacionesyelecciones.adaptadores.CandidatoAdapter
import com.daniel.votacionesyelecciones.databinding.ActivityCandidatoBinding
import com.daniel.votacionesyelecciones.databinding.DialogCandidatoBinding
import com.daniel.votacionesyelecciones.modelo.CandidatoModelo
import com.daniel.votacionesyelecciones.modelo.Eleccion
import com.daniel.votacionesyelecciones.modelo.RepositorioDatos
import com.daniel.votacionesyelecciones.modelo.Validaciones

class Candidato : PantallaConSesion() {
    private lateinit var binding: ActivityCandidatoBinding
    private lateinit var adaptador: CandidatoAdapter
    private var elecciones = emptyList<Eleccion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCandidatoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAtras.setOnClickListener { finish() }
        adaptador = CandidatoAdapter(emptyList(),
            { id -> elecciones.find { it.id == id }?.nombre ?: "Elección no disponible" },
            ::prepararDialogo, ::confirmarEliminarCandidato)
        binding.recyclerCandidatos.layoutManager = LinearLayoutManager(this)
        binding.recyclerCandidatos.adapter = adaptador
        binding.fabAgregar.setOnClickListener { prepararDialogo(null) }
    }

    override fun onResume() {
        super.onResume()
        actualizarLista()
    }

    private fun actualizarLista() {
        ejecutar("Consultando candidatos…", {
            RepositorioDatos.obtenerElecciones().onSuccessTask { lista ->
                elecciones = lista
                RepositorioDatos.obtenerCandidatos()
            }
        }, ::actualizarLista) { lista ->
            adaptador.actualizarDatos(lista)
            binding.recyclerCandidatos.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
            binding.estadoVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            binding.txtEstadoVacio.text = getString(
                if (elecciones.isEmpty()) R.string.sin_elecciones_aviso else R.string.lista_vacia_candidatos)
        }
    }

    private fun prepararDialogo(existente: CandidatoModelo?) {
        ejecutar("Consultando elecciones…", { RepositorioDatos.obtenerElecciones() }) { lista ->
            elecciones = lista
            if (lista.isEmpty()) aviso(getString(R.string.sin_elecciones_aviso))
            else mostrarDialogo(existente, lista)
        }
    }

    private fun mostrarDialogo(existente: CandidatoModelo?, lista: List<Eleccion>) {
        val campos = DialogCandidatoBinding.inflate(layoutInflater)
        val id = existente?.id ?: RepositorioDatos.nuevoId("candidatos")
        campos.spinnerEleccion.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, lista.map { it.nombre })
        existente?.let {
            val posicion = lista.indexOfFirst { eleccion -> eleccion.id == it.eleccionId }
            if (posicion < 0) { aviso("La elección asociada ya no existe."); return }
            campos.spinnerEleccion.setSelection(posicion)
             
            campos.spinnerEleccion.isEnabled = false
            campos.edtNombreCandidato.setText(it.nombre)
            campos.edtListaCandidato.setText(it.lista)
            campos.edtEdadCandidato.setText(it.edad.toString())
            campos.edtCargoCandidato.setText(it.cargo)
            campos.edtPropuestaCandidato.setText(it.propuesta)
        }
        val dialogo = AlertDialog.Builder(this)
            .setTitle(if (existente == null) R.string.agregar_candidato else R.string.editar_candidato)
            .setView(campos.root)
            .setPositiveButton(R.string.accion_guardar, null)
            .setNegativeButton(R.string.accion_cancelar, null).create()
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (ocupada) return@setOnClickListener
                val edad = Validaciones.enteroEnRango(campos.edtEdadCandidato.text.toString(), 18..120)
                if (edad == null) {
                    campos.edtEdadCandidato.error = "Ingresa una edad entera entre 18 y 120."
                    return@setOnClickListener
                }
                val seleccion = lista.getOrNull(campos.spinnerEleccion.selectedItemPosition)
                if (seleccion == null) { aviso("Selecciona una elección."); return@setOnClickListener }
                val candidato = CandidatoModelo(
                    id = id, nombre = campos.edtNombreCandidato.text.toString().trim(),
                    lista = campos.edtListaCandidato.text.toString().trim(), eleccionId = seleccion.id,
                    edad = edad, cargo = campos.edtCargoCandidato.text.toString().trim(),
                    propuesta = campos.edtPropuestaCandidato.text.toString().trim()
                )
                val error = Validaciones.candidato(candidato)
                if (error != null) { aviso(error); return@setOnClickListener }
                ejecutar("Guardando candidato…", { RepositorioDatos.guardarCandidato(candidato, existente == null) }) {
                    dialogo.dismiss()
                    aviso(if (existente == null) "Candidato creado correctamente." else "Candidato actualizado.")
                    actualizarLista()
                }
            }
        }
        dialogo.show()
    }

    private fun confirmarEliminarCandidato(candidato: CandidatoModelo) {
        if (ocupada) return
        AlertDialog.Builder(this).setTitle(R.string.confirmar_eliminar_titulo)
            .setMessage("Se eliminará este candidato y sus votos asociados. ¿Deseas continuar?")
            .setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_eliminar) { _, _ ->
                ejecutar("Eliminando candidato…", { RepositorioDatos.eliminarCandidato(candidato.id) }) {
                    aviso("Candidato y votos asociados eliminados.")
                    actualizarLista()
                }
            }.show()
    }
}
