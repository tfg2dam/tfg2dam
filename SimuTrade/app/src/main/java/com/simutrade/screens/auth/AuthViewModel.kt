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

    private val _uiState = MutableStateFlow(EstadoUiAutenticacion())
    val uiState: StateFlow<EstadoUiAutenticacion> = _uiState.asStateFlow()

    // ================= LOGIN =================

    fun iniciarSesion(email: String, password: String) {
        if (_uiState.value.cargando) return

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Completa todos los campos") }
            return
        }

        if (!esEmailValido(email)) {
            _uiState.update { it.copy(error = "El email no es válido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, exito = false) }

            when (val resultado = repositorio.iniciarSesion(
                email = email.trim().lowercase(),
                password = password
            )) {
                is ResultadoAutenticacion.Exito -> {
                    _uiState.value = EstadoUiAutenticacion(cargando = false, exito = true)
                }
                is ResultadoAutenticacion.Error -> {
                    _uiState.value = EstadoUiAutenticacion(
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
        confirmarPassword: String,
        nombreUsuario: String
    ) {
        if (_uiState.value.cargando) return

        val nombre = nombreUsuario.trim()

        if (email.isBlank() || password.isBlank() || nombre.isBlank()) {
            _uiState.update { it.copy(error = "Completa todos los campos") }
            return
        }

        if (!esEmailValido(email)) {
            _uiState.update { it.copy(error = "El email no es válido") }
            return
        }

        if (nombre.length < 3) {
            _uiState.update { it.copy(error = "El nombre debe tener al menos 3 caracteres") }
            return
        }

        if (nombre.length > 20) {
            _uiState.update { it.copy(error = "El nombre no puede tener más de 20 caracteres") }
            return
        }

        if (!esNombreValido(nombre)) {
            _uiState.update { it.copy(error = "El nombre solo puede contener letras, números y guiones bajos") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
            return
        }

        if (password != confirmarPassword) {
            _uiState.update { it.copy(error = "Las contraseñas no coinciden") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, exito = false) }

            when (val resultado = repositorio.registrarUsuario(
                email = email.trim().lowercase(),
                password = password,
                nombreUsuario = nombre
            )) {
                is ResultadoAutenticacion.Exito -> {
                    _uiState.value = EstadoUiAutenticacion(cargando = false, exito = true)
                }
                is ResultadoAutenticacion.Error -> {
                    _uiState.value = EstadoUiAutenticacion(
                        cargando = false,
                        error = resultado.mensaje
                    )
                }
            }
        }
    }

    // ================= HELPERS =================

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }

    fun cerrarSesion() {
        if (_uiState.value.cargando) return
        repositorio.cerrarSesion()
        _uiState.value = EstadoUiAutenticacion()
    }

    private fun esEmailValido(email: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return regex.matches(email.trim())
    }

    private fun esNombreValido(nombre: String): Boolean {
        val regex = Regex("^[A-Za-z0-9_áéíóúÁÉÍÓÚñÑ]+$")
        return regex.matches(nombre)
    }
}