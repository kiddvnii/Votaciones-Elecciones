package com.daniel.votacionesyelecciones

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.daniel.votacionesyelecciones.adaptadores.VotoAdapter
import com.daniel.votacionesyelecciones.adaptadores.VotoUi
import com.daniel.votacionesyelecciones.databinding.ActivityVotacionesBinding
import com.daniel.votacionesyelecciones.databinding.DialogResultadosBinding
import com.daniel.votacionesyelecciones.databinding.DialogVotoBinding
import com.daniel.votacionesyelecciones.databinding.ItemResultadoVotoBinding
import com.daniel.votacionesyelecciones.modelo.CandidatoModelo
import com.daniel.votacionesyelecciones.modelo.Eleccion
import com.daniel.votacionesyelecciones.modelo.RegistroVoto
import com.daniel.votacionesyelecciones.modelo.RepositorioDatos
import com.google.android.gms.tasks.Task

class Votaciones : PantallaConSesion() {
    private lateinit var binding: ActivityVotacionesBinding
    private lateinit var adaptador: VotoAdapter
    private val exportarPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) guardarReporte(uri)
    }

    private data class Datos(
        val elecciones: List<Eleccion>, val candidatos: List<CandidatoModelo>, val votos: List<RegistroVoto>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVotacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAtras.setOnClickListener { finish() }
        binding.btnResultados.setOnClickListener { mostrarResultados() }
        binding.btnExportarPdf.setOnClickListener { exportarPdf.launch("reporte-votaciones.pdf") }
        if (intent.getBooleanExtra("exportar_pdf", false)) {
            binding.root.post {
                exportarPdf.launch("reporte-votaciones.pdf")
            }
        }
        adaptador = VotoAdapter(emptyList(), ::confirmarEliminarVoto)
        binding.recyclerVotos.layoutManager = LinearLayoutManager(this)
        binding.recyclerVotos.adapter = adaptador
        binding.fabVotar.setOnClickListener {
            ejecutar("Consultando elecciones y candidatos…", ::cargarDatos) { datos ->
                val activas = datos.elecciones.filter { it.estado == "Activa" }
                if (activas.isEmpty()) aviso("Primero crea una elección activa.")
                else mostrarDialogoVoto(activas, datos.candidatos)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarLista()
    }

    private fun cargarDatos(): Task<Datos> =
        RepositorioDatos.obtenerElecciones().onSuccessTask { elecciones ->
            RepositorioDatos.obtenerCandidatos().onSuccessTask { candidatos ->
                RepositorioDatos.obtenerVotos().continueWith { votos -> Datos(elecciones, candidatos, votos.result) }
            }
        }

    private fun actualizarLista() {
        ejecutar("Consultando votos…", ::cargarDatos, ::actualizarLista) { datos ->
            val lista = datos.votos.map { voto ->
                VotoUi(voto, datos.candidatos.find { it.id == voto.candidatoId }?.nombre ?: "Candidato no disponible",
                    datos.elecciones.find { it.id == voto.eleccionId }?.nombre ?: "Elección no disponible")
            }
            adaptador.actualizarDatos(lista)
            binding.txtTotalVotos.text = getString(R.string.total_votos, lista.size)
            binding.recyclerVotos.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
            binding.estadoVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun mostrarDialogoVoto(elecciones: List<Eleccion>, candidatos: List<CandidatoModelo>) {
        val campos = DialogVotoBinding.inflate(layoutInflater)
        val id = RepositorioDatos.nuevoId("votos")
        var candidatosActuales = emptyList<CandidatoModelo>()
        campos.spinnerEleccionVoto.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, elecciones.map { it.nombre })
        campos.spinnerEleccionVoto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, rowId: Long) {
                val eleccion = elecciones.getOrNull(position) ?: return
                candidatosActuales = candidatos.filter { it.eleccionId == eleccion.id }
                campos.spinnerCandidatoVoto.adapter = ArrayAdapter(this@Votaciones,
                    android.R.layout.simple_spinner_dropdown_item,
                    candidatosActuales.map { it.nombre + " (" + it.lista + ")" })
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val dialogo = AlertDialog.Builder(this).setTitle(R.string.registrar_voto)
            .setView(campos.root).setPositiveButton(R.string.registrar_voto, null)
            .setNegativeButton(R.string.accion_cancelar, null).create()
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (ocupada) return@setOnClickListener
                val eleccion = elecciones.getOrNull(campos.spinnerEleccionVoto.selectedItemPosition)
                val candidato = candidatosActuales.getOrNull(campos.spinnerCandidatoVoto.selectedItemPosition)
                if (eleccion == null || candidato == null || candidato.eleccionId != eleccion.id) {
                    aviso("Selecciona una elección con candidatos disponibles.")
                    return@setOnClickListener
                }
                ejecutar("Registrando voto…", { RepositorioDatos.registrarVoto(id, eleccion.id, candidato.id) }) {
                    dialogo.dismiss()
                    aviso("Voto registrado correctamente.")
                    actualizarLista()
                }
            }
        }
        dialogo.show()
    }

    private fun confirmarEliminarVoto(voto: RegistroVoto) {
        if (ocupada) return
        AlertDialog.Builder(this).setTitle(R.string.confirmar_eliminar_titulo)
            .setMessage(R.string.confirmar_eliminar_voto).setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_eliminar) { _, _ ->
                ejecutar("Eliminando voto…", { RepositorioDatos.eliminarVoto(voto.id) }) {
                    aviso("Voto eliminado.")
                    actualizarLista()
                }
            }.show()
    }

    private fun guardarReporte(uri: Uri) {
        ejecutar("Preparando reporte PDF…", ::cargarDatos) { datos ->
            lifecycleScope.launch {
                binding.btnExportarPdf.isEnabled = false
                try {
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.use { salida ->
                            ReportePdf.escribir(salida, datos.elecciones, datos.candidatos, datos.votos)
                        } ?: error("No se pudo abrir el documento")
                    }
                    aviso("Reporte PDF guardado.")
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) { aviso("No se pudo generar el reporte PDF. El archivo puede haber quedado incompleto.") }
                finally { binding.btnExportarPdf.isEnabled = true }
            }
        }
    }

    private fun mostrarResultados() {
        ejecutar("Calculando resultados…", ::cargarDatos) { datos ->
            if (datos.candidatos.isEmpty()) { aviso(getString(R.string.lista_vacia_candidatos)); return@ejecutar }
            val filas = datos.candidatos.map { candidato ->
                candidato to datos.votos.count { it.candidatoId == candidato.id }
            }.sortedByDescending { it.second }
            val maximo = (filas.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
            val contenido = DialogResultadosBinding.inflate(layoutInflater)
            filas.forEach { (candidato, cantidad) ->
                val fila = ItemResultadoVotoBinding.inflate(
                    LayoutInflater.from(this), contenido.contenedorResultados, false)
                fila.txtNombreResultado.text = candidato.nombre
                fila.txtListaResultado.text = candidato.lista + " · " +
                    (datos.elecciones.find { it.id == candidato.eleccionId }?.nombre ?: "Elección no disponible")
                fila.txtVotosResultado.text = cantidad.toString()
                fila.barraProgresoResultado.max = maximo
                fila.barraProgresoResultado.progress = cantidad
                contenido.contenedorResultados.addView(fila.root)
            }
            AlertDialog.Builder(this).setTitle(R.string.resultados).setView(contenido.root)
                .setPositiveButton(android.R.string.ok, null).show()
        }
    }
}