package com.gastop.app.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.gastop.app.MyApplication
import com.gastop.app.R
import com.gastop.app.data.repository.AuthRepository
import com.gastop.app.databinding.FragmentLoginBinding
import com.gastop.app.ui.viewmodel.AuthViewModel
import com.gastop.app.ui.viewmodel.AuthViewModelFactory
import com.gastop.app.data.local.GastopDatabase
import com.gastop.app.data.repository.GastopRepository
import com.gastop.app.ui.viewmodel.GastopViewModel
import com.gastop.app.ui.viewmodel.GastopViewModelFactory
import androidx.fragment.app.activityViewModels

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuthViewModel
    private val gastopViewModel: GastopViewModel by activityViewModels {
        val db = GastopDatabase.getDatabase(requireActivity().application)
        GastopViewModelFactory(GastopRepository(db.gastopDao()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MyApplication
        val repository = AuthRepository(app.auth)
        val factory = AuthViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Log.i("Auth", "Sesión activa: ${user.email}")
                // Re-inicializar sync y categorías para el nuevo usuario
                gastopViewModel.reiniciarSync()
                // Navegar a la pantalla principal si el fragmento actual es LoginFragment
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
            } else {
                Log.i("Auth", "Sin sesión")
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            if (email.isNotBlank() && password.isNotBlank()) {
                viewModel.register(email, password)
            } else {
                Toast.makeText(requireContext(), "Rellena los campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            if (email.isNotBlank() && password.isNotBlank()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Rellena los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
