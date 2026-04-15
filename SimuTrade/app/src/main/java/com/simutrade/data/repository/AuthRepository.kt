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
            val user = result.user!!

            // Actualizar ultimo_login
            firestore.collection("Usuarios").document(user.uid)
                .update("ultimo_login", System.currentTimeMillis()).await()

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Error al iniciar sesión")
        }
    }

    suspend fun register(email: String, password: String, username: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            val userData = hashMapOf(
                "id_usuario" to user.uid,
                "nombre_usuario" to username,
                "email" to email,
                "saldo" to 100.0,
                "saldo_inicial" to 100.0,
                "id_rango" to "bronce",
                "creado_en" to System.currentTimeMillis(),
                "ultimo_login" to System.currentTimeMillis(),

                "portfolio_value" to 100.0,
                "profit" to 0.0
            )

            firestore.collection("Usuarios").document(user.uid).set(userData).await()

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Error al registrarse")
        }
    }

    fun logout() {
        auth.signOut()
    }
}