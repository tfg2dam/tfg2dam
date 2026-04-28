package com.simutrade.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.repository.RepositorioAutenticacion
import com.simutrade.data.repository.ResultadoAutenticacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoUiAutenticacion(
    val cargando: Boolean = false,
    val error: String? = null,
    val exito: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val repositorio = RepositorioAutenticacion()

    private val _uiState =
        MutableStateFlow(EstadoUiAutenticacion())

    val uiState: StateFlow<EstadoUiAutenticacion> =
        _uiState.asStateFlow()

    // ================= ESTADO =================

    private fun actualizarEstado(
        cargando: Boolean? = null,
        error: String? = null,
        exito: Boolean? = null
    ) {
        _uiState.update { actual ->
            actual.copy(
                cargando = cargando ?: actual.cargando,
                error = error,
                exito = exito ?: actual.exito
            )
        }
    }

    // ================= LOGIN =================

    fun iniciarSesion(
        email: String,
        password: String
    ) {

        if (_uiState.value.cargando) return

        if (
            email.isBlank() ||
            password.isBlank()
        ) {
            actualizarEstado(
                error = "Completa todos los campos"
            )
            return
        }

        viewModelScope.launch {

            actualizarEstado(
                cargando = true,
                error = null,
                exito = false
            )

            when (
                val resultado = repositorio.iniciarSesion(
                    email = email.trim(),
                    password = password
                )
            ) {

                is ResultadoAutenticacion.Exito -> {
                    _uiState.value =
                        EstadoUiAutenticacion(
                            cargando = false,
                            exito = true
                        )
                }

                is ResultadoAutenticacion.Error -> {
                    _uiState.value =
                        EstadoUiAutenticacion(
                            cargando = false,
                            error = resultado.mensaje
                        )
                }
            }
        }
    }

    // ================= REGISTRO =================

    fun registrarUsuario(
        email: String,
        password: String,
        nombreUsuario: String
    ) {

        if (_uiState.value.cargando) return

        if (
            email.isBlank() ||
            password.isBlank() ||
            nombreUsuario.isBlank()
        ) {
            actualizarEstado(
                error = "Completa todos los campos"
            )
            return
        }

        if (password.length < 6) {
            actualizarEstado(
                error = "La contraseña debe tener al menos 6 caracteres"
            )
            return
        }

        viewModelScope.launch {

            actualizarEstado(
                cargando = true,
                error = null,
                exito = false
            )

            when (
                val resultado = repositorio.registrarUsuario(
                    email = email.trim(),
                    password = password,
                    nombreUsuario = nombreUsuario.trim()
                )
            ) {

                is ResultadoAutenticacion.Exito -> {
                    _uiState.value =
                        EstadoUiAutenticacion(
                            cargando = false,
                            exito = true
                        )
                }

                is ResultadoAutenticacion.Error -> {
                    _uiState.value =
                        EstadoUiAutenticacion(
                            cargando = false,
                            error = resultado.mensaje
                        )
                }
            }
        }
    }

    // ================= HELPERS =================

    fun limpiarError() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    fun limpiarExito() {
        _uiState.update {
            it.copy(exito = false)
        }
    }

    fun cerrarSesion() {
        repositorio.cerrarSesion()
        _uiState.value =
            EstadoUiAutenticacion()
    }
}