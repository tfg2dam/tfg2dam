package com.simutrade.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ================= RESULTADO AUTENTICACIÓN =================

sealed class ResultadoAutenticacion {

    data class Exito(
        val usuario: FirebaseUser
    ) : ResultadoAutenticacion()

    data class Error(
        val mensaje: String
    ) : ResultadoAutenticacion()
}

class RepositorioAutenticacion {

    private val autenticacion = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val SALDO_INICIAL = 100.0
        private const val USUARIOS = "Usuarios"
        private const val RETOS = "retos"
        private const val DOCUMENTO_RETOS = "datos"
    }

    // ================= UTILIDADES =================

    private fun generarCodigoUsuario(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { caracteres.random() }.joinToString("")
    }

    private fun traducirErrorAuth(e: Exception): String {
        // ✅ Primero comprobamos el tipo de excepción — más fiable que el mensaje
        return when (e) {
            is FirebaseAuthUserCollisionException ->
                "Este email ya está registrado. Inicia sesión."

            is FirebaseAuthWeakPasswordException ->
                "La contraseña es demasiado débil"

            is FirebaseAuthInvalidCredentialsException ->
                "Email o contraseña incorrectos"

            is FirebaseAuthInvalidUserException ->
                "Email o contraseña incorrectos"

            else -> {
                // ✅ Como fallback miramos el mensaje por si acaso
                val mensaje = e.message ?: ""
                when {
                    mensaje.contains("email-already-in-use", ignoreCase = true) ||
                            mensaje.contains("EMAIL_EXISTS", ignoreCase = true) ||
                            mensaje.contains("already in use", ignoreCase = true) ->
                        "Este email ya está registrado. Inicia sesión."

                    mensaje.contains("no user record", ignoreCase = true) ||
                            mensaje.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                            mensaje.contains("user-not-found", ignoreCase = true) ||
                            mensaje.contains("wrong-password", ignoreCase = true) ||
                            mensaje.contains("invalid-credential", ignoreCase = true) ->
                        "Email o contraseña incorrectos"

                    mensaje.contains("weak-password", ignoreCase = true) ->
                        "La contraseña es demasiado débil"

                    mensaje.contains("invalid-email", ignoreCase = true) ->
                        "El email no es válido"

                    mensaje.contains("network-request-failed", ignoreCase = true) ||
                            mensaje.contains("network", ignoreCase = true) ->
                        "Sin conexión a internet"

                    mensaje.contains("too-many-requests", ignoreCase = true) ->
                        "Demasiados intentos. Espera un momento"

                    else -> "Error inesperado. Inténtalo de nuevo"
                }
            }
        }
    }

    // ================= LOGIN =================

    suspend fun iniciarSesion(
        email: String,
        password: String
    ): ResultadoAutenticacion {
        return try {
            val resultado = autenticacion
                .signInWithEmailAndPassword(email, password)
                .await()

            val usuario = resultado.user
                ?: return ResultadoAutenticacion.Error("Usuario no válido")

            firestore
                .collection(USUARIOS)
                .document(usuario.uid)
                .update("ultimo_login", System.currentTimeMillis())
                .await()

            ResultadoAutenticacion.Exito(usuario)

        } catch (e: Exception) {
            ResultadoAutenticacion.Error(traducirErrorAuth(e))
        }
    }

    // ================= REGISTRO =================

    suspend fun registrarUsuario(
        email: String,
        password: String,
        nombreUsuario: String
    ): ResultadoAutenticacion {
        return try {
            val resultado = autenticacion
                .createUserWithEmailAndPassword(email, password)
                .await()

            val usuario = resultado.user
                ?: return ResultadoAutenticacion.Error("Error al crear usuario")

            val ahora = System.currentTimeMillis()

            val referenciaUsuario = firestore
                .collection(USUARIOS)
                .document(usuario.uid)

            referenciaUsuario.set(
                mapOf(
                    "nombre_usuario" to nombreUsuario,
                    "email" to email,
                    "codigo_usuario" to generarCodigoUsuario(),
                    "saldo" to SALDO_INICIAL,
                    "saldo_inicial" to SALDO_INICIAL,
                    "saldo_bonus" to 0.0,
                    "creado_en" to ahora,
                    "ultimo_login" to ahora,
                    "valor_cartera" to 0.0,
                    "beneficio" to 0.0,
                    "mis_ligas" to emptyList<String>()
                )
            ).await()

            referenciaUsuario
                .collection(RETOS)
                .document(DOCUMENTO_RETOS)
                .set(
                    mapOf(
                        "racha_actual" to 0,
                        "racha_maxima" to 0,
                        "ultima_vez" to 0L,
                        "retos_completados" to emptyList<String>(),
                        "retos_del_dia" to emptyList<String>(),
                        "dia_actual" to 0L
                    )
                )
                .await()

            ResultadoAutenticacion.Exito(usuario)

        } catch (e: Exception) {
            ResultadoAutenticacion.Error(traducirErrorAuth(e))
        }
    }

    // ================= LOGOUT =================

    fun cerrarSesion() {
        autenticacion.signOut()
    }
}