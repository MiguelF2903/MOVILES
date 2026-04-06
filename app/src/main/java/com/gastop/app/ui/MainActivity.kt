package com.gastop.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.gastop.app.R
import com.gastop.app.data.local.GastopDatabase
import com.gastop.app.data.repository.GastopRepository
import com.gastop.app.databinding.ActivityMainBinding
import com.gastop.app.ui.viewmodel.GastopViewModel

class MainActivity : AppCompatActivity() {

    lateinit var viewModel: GastopViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        // Inicializar ViewModel con Factory
        val database = GastopDatabase.getDatabase(applicationContext)
        val repository = GastopRepository(database.gastopDao())
        val factory = GastopViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[GastopViewModel::class.java]

        // Configurar Navigation con BottomNavigationView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)
    }
}
