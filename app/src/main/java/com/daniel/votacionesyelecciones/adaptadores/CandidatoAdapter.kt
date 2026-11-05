
package com.daniel.votacionesyelecciones.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daniel.votacionesyelecciones.databinding.ItemCandidatoBinding
import com.daniel.votacionesyelecciones.modelo.CandidatoModelo

class CandidatoAdapter(
    private var datos: List<CandidatoModelo>,
    private val nombreEleccion: (String) -> String,
    private val onEditar: (CandidatoModelo) -> Unit,
    private val onEliminar: (CandidatoModelo) -> Unit
) : RecyclerView.Adapter<CandidatoAdapter.CandidatoViewHolder>() {

    inner class CandidatoViewHolder(
        val binding: ItemCandidatoBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CandidatoViewHolder {
        val binding = ItemCandidatoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CandidatoViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CandidatoViewHolder,
        position: Int
    ) {
        val candidato = datos[position]

        holder.binding.txtNombreCandidato.text = candidato.nombre
        holder.binding.txtListaCandidato.text = candidato.lista + "\nEdad: " + candidato.edad +
            " años\nCargo: " + candidato.cargo + "\nPropuesta: " + candidato.propuesta
        holder.binding.txtEleccionCandidato.text =
            nombreEleccion(candidato.eleccionId)

        holder.binding.btnEditarCandidato.setOnClickListener {
            onEditar(candidato)
        }

        holder.binding.btnEliminarCandidato.setOnClickListener {
            onEliminar(candidato)
        }
    }

    override fun getItemCount(): Int = datos.size

    fun actualizarDatos(nuevaLista: List<CandidatoModelo>) {
        datos = nuevaLista
        notifyDataSetChanged()
    }
}
