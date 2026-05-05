package com.gastop.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gastop.app.data.repository.GastopRepository

class GastopViewModelFactory(private val repository: GastopRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GastopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GastopViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
