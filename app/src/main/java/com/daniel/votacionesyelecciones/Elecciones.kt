package com.daniel.votacionesyelecciones

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.daniel.votacionesyelecciones.adaptadores.EleccionAdapter
import com.daniel.votacionesyelecciones.databinding.ActivityEleccionesBinding
import com.daniel.votacionesyelecciones.databinding.DialogEleccionBinding
import com.daniel.votacionesyelecciones.modelo.Eleccion
import com.daniel.votacionesyelecciones.modelo.RepositorioDatos
import com.daniel.votacionesyelecciones.modelo.Validaciones
import com.daniel.votacionesyelecciones.modelo.FechasEleccion
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker

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
        var fechaInicio = existente?.fecha ?: FechasEleccion.hoy()
        var fechaTermino = existente?.fechaTermino?.takeIf { it.isNotBlank() } ?: FechasEleccion.terminoAutomatico()
        var terminoEditado = existente != null && !existente.fechaTermino.isBlank()
        campos.edtFechaEleccion.setText(FechasEleccion.visible(fechaInicio))
        campos.edtFechaTermino.setText(FechasEleccion.visible(fechaTermino))
        campos.edtFechaEleccion.setOnClickListener {
            elegirFecha(fechaInicio, FechasEleccion.hoy(), "Inicio de la elección", "fecha_inicio") { elegida ->
                fechaInicio = elegida
                campos.edtFechaEleccion.setText(FechasEleccion.visible(elegida))
                campos.campoFechaInicio.error = null
            }
        }
        campos.campoFechaInicio.setEndIconOnClickListener { campos.edtFechaEleccion.performClick() }
        campos.edtFechaTermino.setOnClickListener {
            val minima = if ((FechasEleccion.milis(fechaInicio) ?: 0) > FechasEleccion.milis(FechasEleccion.hoy())!!) fechaInicio else FechasEleccion.hoy()
            elegirFecha(fechaTermino, minima, "Término de la elección", "fecha_termino") { elegida ->
                fechaTermino = elegida; terminoEditado = true
                campos.edtFechaTermino.setText(FechasEleccion.visible(elegida))
                campos.campoFechaTermino.error = null
            }
        }
        campos.campoFechaTermino.setEndIconOnClickListener { campos.edtFechaTermino.performClick() }
        existente?.let {
            campos.edtNombreEleccion.setText(it.nombre)
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
                    fecha = fechaInicio,
                    fechaTermino = if (!terminoEditado && existente == null) FechasEleccion.terminoAutomatico() else fechaTermino,
                    estado = if (campos.radioCerrada.isChecked) "Cerrada" else "Activa",
                    lugar = campos.edtLugarEleccion.text.toString().trim(), mesas = mesas
                )
                val errorCampos = Validaciones.eleccion(eleccion)
                if (errorCampos != null) { aviso(errorCampos); return@setOnClickListener }
                val errorFechas = FechasEleccion.errorPeriodo(eleccion, existente)
                if (errorFechas != null) {
                    if (errorFechas.contains("inicio") && !errorFechas.contains("término")) campos.campoFechaInicio.error = errorFechas
                    else campos.campoFechaTermino.error = errorFechas
                    aviso(errorFechas); return@setOnClickListener
                }
                ejecutar("Guardando elección…", { RepositorioDatos.guardarEleccion(eleccion, existente == null) }) {
                    dialogo.dismiss()
                    aviso(if (existente == null) "Elección creada correctamente." else "Elección actualizada.")
                    actualizarLista()
                }
            }
        }
        dialogo.show()
    }

    private fun elegirFecha(actual: String, minima: String, titulo: String, etiqueta: String, alElegir: (String) -> Unit) {
        if (supportFragmentManager.findFragmentByTag(etiqueta) != null) return
        val desde = requireNotNull(FechasEleccion.milis(minima))
        val seleccion = maxOf(FechasEleccion.milis(actual) ?: desde, desde)
        val restricciones = CalendarConstraints.Builder()
            .setStart(desde).setEnd(requireNotNull(FechasEleccion.milis("31-12-2100")))
            .setOpenAt(seleccion).setValidator(DateValidatorPointForward.from(desde)).build()
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(titulo).setSelection(seleccion)
            .setCalendarConstraints(restricciones)
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setPositiveButtonText("Seleccionar").setNegativeButtonText("Cancelar")
            .build().apply {
                addOnPositiveButtonClickListener { alElegir(FechasEleccion.desdeMilis(it)) }
            }.show(supportFragmentManager, etiqueta)
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
