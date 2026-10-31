package com.daniel.votacioneselecciones.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daniel.votacioneselecciones.databinding.ItemEleccionBinding
import com.daniel.votacioneselecciones.modelo.Eleccion
import androidx.core.content.ContextCompat
import com.daniel.votacioneselecciones.R

class EleccionAdapter(
    private var datos: List<Eleccion>,
    private val onEditar: (Eleccion) -> Unit,
    private val onEliminar: (Eleccion) -> Unit
) : RecyclerView.Adapter<EleccionAdapter.EleccionViewHolder>() {

    inner class EleccionViewHolder(val binding: ItemEleccionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EleccionViewHolder {
        val binding = ItemEleccionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EleccionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EleccionViewHolder, position: Int) {
        val eleccion = datos[position]
        holder.binding.txtNombreEleccion.text = eleccion.nombre
        holder.binding.txtFechaEleccion.text = eleccion.fecha + "\nLugar: " + eleccion.lugar +
            "\nMesas: " + eleccion.mesas
        holder.binding.txtEstadoEleccion.text = eleccion.estado

        val colorPill = if (eleccion.estado.equals("Activa", ignoreCase = true)) {
            R.color.verde_exito
        } else {
            R.color.texto_secundario
        }
        holder.binding.txtEstadoEleccion.background.mutate().setTint(
            ContextCompat.getColor(holder.binding.root.context, colorPill)
        )

        holder.binding.btnEditarEleccion.setOnClickListener { onEditar(eleccion) }
        holder.binding.btnEliminarEleccion.setOnClickListener { onEliminar(eleccion) }
    }

    override fun getItemCount(): Int = datos.size

    fun actualizarDatos(nuevaLista: List<Eleccion>) {
        datos = nuevaLista
        notifyDataSetChanged()
    }
}
