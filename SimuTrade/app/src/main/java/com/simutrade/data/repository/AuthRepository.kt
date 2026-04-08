package com.simutrade.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(result.user!!)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Error al iniciar sesión")
        }
    }

    suspend fun register(email: String, password: String, username: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            // Crear documento del usuario en Firestore
            val userData = hashMapOf(
                "uid" to user.uid,
                "username" to username,
                "email" to email,
                "balance" to 100.0,
                "initialBalance" to 100.0,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid).set(userData).await()

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Error al registrarse")
        }
    }

    fun logout() {
        auth.signOut()
    }
}