package com.gastop.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {

    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth
                .createUserWithEmailAndPassword(email, password)
                .await()
            Log.i("Auth", "Usuario registrado: ${result.user?.email}")
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e("Auth", "Error al registrar", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth
                .signInWithEmailAndPassword(email, password)
                .await()
            Log.i("Auth", "Sesión iniciada: ${result.user?.email}")
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e("Auth", "Error al iniciar sesión", e)
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        Log.i("Auth", "Sesión cerrada")
    }

    fun currentUser(): FirebaseUser? = auth.currentUser
}
