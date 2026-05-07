package com.gastop.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gastop.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        // Si ya había sesión iniciada, la recuperamos
        _user.value = repository.currentUser()
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            repository.register(email, password)
                .onSuccess { _user.postValue(it) }
                .onFailure { _error.postValue(it.message) }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            repository.login(email, password)
                .onSuccess { _user.postValue(it) }
                .onFailure { _error.postValue(it.message) }
        }
    }

    fun logout() {
        repository.logout()
        _user.value = null
    }
}
