package com.daniel.votacionesyelecciones.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daniel.votacionesyelecciones.databinding.ItemVotoBinding
import com.daniel.votacionesyelecciones.modelo.RegistroVoto

data class VotoUi(
    val registro: RegistroVoto,
    val nombreCandidato: String,
    val nombreEleccion: String
)

class VotoAdapter(
    private var datos: List<VotoUi>,
    private val onEliminar: (RegistroVoto) -> Unit
) : RecyclerView.Adapter<VotoAdapter.VotoViewHolder>() {

    inner class VotoViewHolder(val binding: ItemVotoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VotoViewHolder {
        val binding = ItemVotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VotoViewHolder, position: Int) {
        val item = datos[position]
        holder.binding.txtNombreVoto.text = item.nombreCandidato
        holder.binding.txtDetalleVoto.text =
            "${item.nombreEleccion} · ${item.registro.fechaHora}"

        holder.binding.btnEliminarVoto.setOnClickListener { onEliminar(item.registro) }
    }

    override fun getItemCount(): Int = datos.size

    fun actualizarDatos(nuevaLista: List<VotoUi>) {
        datos = nuevaLista
        notifyDataSetChanged()
    }
}
