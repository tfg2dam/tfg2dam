package com.simutrade.data.repository

import com.google.firebase.auth.FirebaseAuth
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

        // ================= DOCUMENTOS =================

        private const val DOCUMENTO_INICIAL = "inicial"
        private const val DOCUMENTO_RETOS = "datos"

        // ================= SUBCOLECCIONES =================

        private const val CARTERA = "cartera"
        private const val TRANSACCIONES = "transacciones"
        private const val RETOS = "retos"
    }

    // Eliminado warning:
    // val usuarioActual no se usaba en ningún sitio

    // ================= LOGIN =================

    suspend fun iniciarSesion(
        email: String,
        password: String
    ): ResultadoAutenticacion {

        return try {

            val resultado = autenticacion
                .signInWithEmailAndPassword(
                    email,
                    password
                )
                .await()

            val usuario = resultado.user
                ?: return ResultadoAutenticacion.Error(
                    "Usuario no válido"
                )

            firestore
                .collection(USUARIOS)
                .document(usuario.uid)
                .update(
                    "ultimo_login",
                    System.currentTimeMillis()
                )
                .await()

            ResultadoAutenticacion.Exito(usuario)

        } catch (e: Exception) {
            ResultadoAutenticacion.Error(
                e.message ?: "Error al iniciar sesión"
            )
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
                .createUserWithEmailAndPassword(
                    email,
                    password
                )
                .await()

            val usuario = resultado.user
                ?: return ResultadoAutenticacion.Error(
                    "Error al crear usuario"
                )

            val ahora = System.currentTimeMillis()

            val referenciaUsuario = firestore
                .collection(USUARIOS)
                .document(usuario.uid)

            // ================= DATOS BASE =================

            val datosUsuario = mapOf(
                "nombre_usuario" to nombreUsuario,
                "email" to email,

                "saldo" to SALDO_INICIAL,
                "saldo_inicial" to SALDO_INICIAL,
                "saldo_bonus" to 0.0,

                "id_rango" to "bronce",

                "creado_en" to ahora,
                "ultimo_login" to ahora,

                "valor_cartera" to 0.0,
                "beneficio" to 0.0
            )

            referenciaUsuario
                .set(datosUsuario)
                .await()

            // ================= CARTERA =================

            referenciaUsuario
                .collection(CARTERA)
                .document(DOCUMENTO_INICIAL)
                .set(
                    mapOf(
                        "inicial" to true
                    )
                )
                .await()

            // ================= TRANSACCIONES =================

            referenciaUsuario
                .collection(TRANSACCIONES)
                .document(DOCUMENTO_INICIAL)
                .set(
                    mapOf(
                        "inicial" to true
                    )
                )
                .await()

            // ================= RETOS =================

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
            ResultadoAutenticacion.Error(
                e.message ?: "Error al registrarse"
            )
        }
    }

    // ================= LOGOUT =================

    fun cerrarSesion() {
        autenticacion.signOut()
    }
}