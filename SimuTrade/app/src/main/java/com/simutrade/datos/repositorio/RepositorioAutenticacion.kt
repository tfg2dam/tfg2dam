package com.simutrade.datos.repositorio

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.simutrade.datos.modelo.ResultadoAutenticacion
import kotlinx.coroutines.tasks.await

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

    // Genera un código único de 8 caracteres para el usuario
    private fun generarCodigoUsuario(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { caracteres.random() }.joinToString("")
    }

    // Traduce los errores de Firebase a mensajes legibles
    private fun traducirErrorAuth(e: Exception): String {
        return when (e) {
            is FirebaseAuthUserCollisionException -> "Este email ya está registrado. Inicia sesión."
            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil"
            is FirebaseAuthInvalidCredentialsException -> "Email o contraseña incorrectos"
            is FirebaseAuthInvalidUserException -> "Email o contraseña incorrectos"
            else -> {
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

                    mensaje.contains("weak-password", ignoreCase = true) -> "La contraseña es demasiado débil"
                    mensaje.contains("invalid-email", ignoreCase = true) -> "El email no es válido"
                    mensaje.contains("network-request-failed", ignoreCase = true) ||
                            mensaje.contains("network", ignoreCase = true) -> "Sin conexión a internet"
                    mensaje.contains("too-many-requests", ignoreCase = true) -> "Demasiados intentos. Espera un momento"
                    else -> "Error inesperado. Inténtalo de nuevo"
                }
            }
        }
    }

    // ================= LOGIN =================

    // Inicia sesión y actualiza el último login en Firestore
    suspend fun iniciarSesion(email: String, password: String): ResultadoAutenticacion {
        return try {
            val resultado = autenticacion
                .signInWithEmailAndPassword(email, password)
                .await()

            val usuario = resultado.user
                ?: return ResultadoAutenticacion.Error("Usuario no válido")

            firestore.collection(USUARIOS)
                .document(usuario.uid)
                .update("ultimo_login", System.currentTimeMillis())
                .await()

            ResultadoAutenticacion.Exito(usuario)

        } catch (e: Exception) {
            ResultadoAutenticacion.Error(traducirErrorAuth(e))
        }
    }

    // ================= LOGIN CON GOOGLE =================

    // Autentica con el token de Google y crea el usuario si es la primera vez
    suspend fun iniciarSesionConGoogle(idToken: String): ResultadoAutenticacion {
        return try {
            val credencial = GoogleAuthProvider.getCredential(idToken, null)
            val resultado = autenticacion.signInWithCredential(credencial).await()
            val usuario = resultado.user
                ?: return ResultadoAutenticacion.Error("Usuario no válido")

            val ahora = System.currentTimeMillis()
            val referenciaUsuario = firestore.collection(USUARIOS).document(usuario.uid)
            val doc = referenciaUsuario.get().await()

            if (!doc.exists()) {
                val nombreUsuario = usuario.displayName ?: "Usuario"
                referenciaUsuario.set(
                    mapOf(
                        "nombre_usuario" to nombreUsuario,
                        "email" to (usuario.email ?: ""),
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

                referenciaUsuario.collection(RETOS).document(DOCUMENTO_RETOS)
                    .set(
                        mapOf(
                            "racha_actual" to 0,
                            "racha_maxima" to 0,
                            "ultima_vez" to 0L,
                            "retos_completados" to emptyList<String>(),
                            "retos_del_dia" to emptyList<String>(),
                            "dia_actual" to 0L
                        )
                    ).await()
            } else {
                referenciaUsuario.update("ultimo_login", ahora).await()
            }

            ResultadoAutenticacion.Exito(usuario)

        } catch (e: Exception) {
            ResultadoAutenticacion.Error(traducirErrorAuth(e))
        }
    }

    // ================= REGISTRO =================

    // Crea el usuario en Firebase Auth y su documento en Firestore
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
            val referenciaUsuario = firestore.collection(USUARIOS).document(usuario.uid)

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

            referenciaUsuario.collection(RETOS).document(DOCUMENTO_RETOS)
                .set(
                    mapOf(
                        "racha_actual" to 0,
                        "racha_maxima" to 0,
                        "ultima_vez" to 0L,
                        "retos_completados" to emptyList<String>(),
                        "retos_del_dia" to emptyList<String>(),
                        "dia_actual" to 0L
                    )
                ).await()

            ResultadoAutenticacion.Exito(usuario)

        } catch (e: Exception) {
            ResultadoAutenticacion.Error(traducirErrorAuth(e))
        }
    }

    // ================= LOGOUT =================

    // Cierra la sesión de Firebase y también del cliente de Google
    fun cerrarSesion(context: Context) {
        autenticacion.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
    }
}