package com.gastop.app.ui.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.gastop.app.R
import com.gastop.app.data.model.Categoria
import com.gastop.app.databinding.FragmentAddMovementBinding
import com.gastop.app.ui.viewmodel.GastopViewModel

class AddMovementFragment : Fragment() {

    private val viewModel: GastopViewModel by activityViewModels()
    private var _binding: FragmentAddMovementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_movement, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sincronizar EditText con el ViewModel LiveData
        binding.etMonto.addTextChangedListener(simpleWatcher { viewModel.formMonto.value = it })
        binding.etConcepto.addTextChangedListener(simpleWatcher { viewModel.formConcepto.value = it })

        // Configurar selector de categorías
        viewModel.categorias.observe(viewLifecycleOwner) { categorias ->
            if (categorias.isEmpty()) {
                viewModel.seedCategorias()
            } else {
                val adapter = CategoriaAdapter(requireContext(), categorias)
                binding.actvCategoria.setAdapter(adapter)
            }
        }

        binding.actvCategoria.setOnItemClickListener { parent, _, position, _ ->
            val selectedCategoria = parent.getItemAtPosition(position) as Categoria
            viewModel.formCategoriaId.value = selectedCategoria.id.toString()
            binding.actvCategoria.setText(selectedCategoria.nombre, false)
        }

        // Tipo selección
        actualizarBotonesTipo()

        binding.btnGasto.setOnClickListener {
            viewModel.formTipo.value = "Gasto"
            actualizarBotonesTipo()
        }

        binding.btnIngreso.setOnClickListener {
            viewModel.formTipo.value = "Ingreso"
            actualizarBotonesTipo()
        }

        // Observar validez del formulario
        viewModel.formValido.observe(viewLifecycleOwner) { valido ->
            binding.btnGuardar.isEnabled = valido
        }

        // Guardar transacción
        binding.btnGuardar.setOnClickListener {
            viewModel.addTransaccion()
            // Navegar de vuelta al Home
            findNavController().navigate(R.id.homeFragment)
        }

        // OCR placeholder
        binding.btnEscanear.setOnClickListener {
            // Placeholder para funcionalidad OCR futura
        }
    }

    private fun actualizarBotonesTipo() {
        val esGasto = viewModel.formTipo.value == "Gasto"

        if (esGasto) {
            binding.btnGasto.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.btnGasto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnIngreso.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            binding.btnIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        } else {
            binding.btnIngreso.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.btnIngreso.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnGasto.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            binding.btnGasto.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        }
    }

    private fun simpleWatcher(onChanged: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onChanged(s?.toString() ?: "")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class CategoriaAdapter(context: Context, categorias: List<Categoria>) :
        ArrayAdapter<Categoria>(context, R.layout.item_categoria, categorias) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createViewFromResource(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createViewFromResource(position, convertView, parent)
        }

        private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_categoria, parent, false)
            val categoria = getItem(position)

            val tvNombre = view.findViewById<TextView>(R.id.tvNombre)
            val viewColor = view.findViewById<View>(R.id.viewColor)

            tvNombre.text = categoria?.nombre
            
            val color = try {
                Color.parseColor(categoria?.color ?: "#808080")
            } catch (e: Exception) {
                Color.GRAY
            }
            
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(color)
            viewColor.background = drawable

            return view
        }
    }
}
