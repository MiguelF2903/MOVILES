package com.gastop.app.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gastop.app.R
import com.gastop.app.data.model.TransaccionConCategoria
import com.gastop.app.databinding.ItemTransaccionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransaccionAdapter(
    private val onClick: (TransaccionConCategoria) -> Unit,
    private val onLongClick: (TransaccionConCategoria) -> Unit
) : ListAdapter<TransaccionConCategoria, TransaccionAdapter.TransaccionViewHolder>(TransaccionDiffCallback()) {

    var moneda: String = "€"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaccionViewHolder {
        val binding = ItemTransaccionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransaccionViewHolder(binding, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: TransaccionViewHolder, position: Int) {
        holder.bind(getItem(position), moneda)
    }

    class TransaccionViewHolder(
        private val binding: ItemTransaccionBinding,
        private val onClick: (TransaccionConCategoria) -> Unit,
        private val onLongClick: (TransaccionConCategoria) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(item: TransaccionConCategoria, moneda: String) {
            val t = item.transaccion
            val cat = item.categoria

            binding.btnEditItem.setOnClickListener {
                onClick(item)
            }

            binding.btnDeleteItem.setOnClickListener {
                onLongClick(item)
            }

            val context = binding.root.context
            val isGasto = t.tipo == "Gasto"
            val colorMonto = if (isGasto) R.color.error else R.color.primary
            val signo = if (isGasto) "-" else "+"

            binding.tvConcepto.text = t.concepto
            binding.tvFecha.text = dateFormat.format(Date(t.fecha))
            binding.tvMonto.text = String.format("%s%s%.2f", signo, moneda, t.monto)
            binding.tvMonto.setTextColor(ContextCompat.getColor(context, colorMonto))

            // Configurar indicador de categoría circular
            if (cat != null) {
                binding.vCategoryIndicator.visibility = View.VISIBLE

                val catColor = try {
                    Color.parseColor(cat.color)
                } catch (e: Exception) {
                    ContextCompat.getColor(context, R.color.gray)
                }

                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(catColor)
                binding.vCategoryIndicator.background = drawable
            } else {
                binding.vCategoryIndicator.visibility = View.GONE
            }
        }
    }

    class TransaccionDiffCallback : DiffUtil.ItemCallback<TransaccionConCategoria>() {
        override fun areItemsTheSame(oldItem: TransaccionConCategoria, newItem: TransaccionConCategoria): Boolean {
            return oldItem.transaccion.id == newItem.transaccion.id
        }

        override fun areContentsTheSame(oldItem: TransaccionConCategoria, newItem: TransaccionConCategoria): Boolean {
            return oldItem == newItem
        }
    }
}
