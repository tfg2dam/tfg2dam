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

    companion object {
        private const val SALDO_INICIAL = 100.0
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Usuario no válido")

            val now = System.currentTimeMillis()

            firestore.collection("Usuarios")
                .document(user.uid)
                .update("ultimo_login", now)
                .await()

            AuthResult.Success(user)

        } catch (e: Exception) {
            AuthResult.Error("Error al iniciar sesión. Inténtalo de nuevo")
        }
    }

    suspend fun register(email: String, password: String, username: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Error al crear usuario")

            val now = System.currentTimeMillis()

            val userData = hashMapOf(
                "id_usuario"      to user.uid,
                "nombre_usuario"  to username,
                "email"           to email,
                "saldo"           to SALDO_INICIAL,
                "saldo_inicial"   to SALDO_INICIAL,
                "saldo_bonus"     to 0.0,
                "id_rango"        to "bronce",
                "creado_en"       to now,
                "ultimo_login"    to now,
                "portfolio_value" to SALDO_INICIAL,
                "profit"          to 0.0
            )

            firestore.collection("Usuarios")
                .document(user.uid)
                .set(userData)
                .await()

            AuthResult.Success(user)

        } catch (e: Exception) {
            AuthResult.Error("Error al registrarse. Inténtalo de nuevo")
        }
    }

    fun logout() {
        auth.signOut()
    }
}