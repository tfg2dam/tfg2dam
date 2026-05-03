package com.simutrade.ui.autenticacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.datos.modelo.ResultadoAutenticacion
import com.simutrade.datos.repositorio.RepositorioAutenticacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la pantalla de autenticación
data class EstadoUiAutenticacion(
    val cargando: Boolean = false,
    val error: String? = null,
    val exito: Boolean = false
)

class AutenticacionViewModel : ViewModel() {

    private val repositorio = RepositorioAutenticacion()

    private val _estadoUi = MutableStateFlow(EstadoUiAutenticacion())
    val estadoUi: StateFlow<EstadoUiAutenticacion> = _estadoUi.asStateFlow()

    // ================= LOGIN =================

    // Valida los campos e inicia sesión en Firebase
    fun iniciarSesion(email: String, password: String) {
        if (_estadoUi.value.cargando) return

        if (email.isBlank() || password.isBlank()) {
            _estadoUi.update { it.copy(error = "Completa todos los campos") }
            return
        }

        if (!esEmailValido(email)) {
            _estadoUi.update { it.copy(error = "El email no es válido") }
            return
        }

        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, error = null, exito = false) }
            when (val resultado = repositorio.iniciarSesion(
                email = email.trim().lowercase(),
                password = password
            )) {
                is ResultadoAutenticacion.Exito ->
                    _estadoUi.value = EstadoUiAutenticacion(cargando = false, exito = true)
                is ResultadoAutenticacion.Error ->
                    _estadoUi.value = EstadoUiAutenticacion(cargando = false, error = resultado.mensaje)
            }
        }
    }

    // ================= REGISTRO =================

    // Valida los campos y registra un nuevo usuario en Firebase
    fun registrarUsuario(
        email: String,
        password: String,
        confirmarPassword: String,
        nombreUsuario: String
    ) {
        if (_estadoUi.value.cargando) return

        val nombre = nombreUsuario.trim()

        if (email.isBlank() || password.isBlank() || nombre.isBlank()) {
            _estadoUi.update { it.copy(error = "Completa todos los campos") }
            return
        }
        if (!esEmailValido(email)) {
            _estadoUi.update { it.copy(error = "El email no es válido") }
            return
        }
        if (nombre.length < 3) {
            _estadoUi.update { it.copy(error = "El nombre debe tener al menos 3 caracteres") }
            return
        }
        if (nombre.length > 20) {
            _estadoUi.update { it.copy(error = "El nombre no puede tener más de 20 caracteres") }
            return
        }
        if (!esNombreValido(nombre)) {
            _estadoUi.update { it.copy(error = "El nombre solo puede contener letras, números y guiones bajos") }
            return
        }
        if (password.length < 6) {
            _estadoUi.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
            return
        }
        if (password != confirmarPassword) {
            _estadoUi.update { it.copy(error = "Las contraseñas no coinciden") }
            return
        }

        viewModelScope.launch {
            _estadoUi.update { it.copy(cargando = true, error = null, exito = false) }
            when (val resultado = repositorio.registrarUsuario(
                email = email.trim().lowercase(),
                password = password,
                nombreUsuario = nombre
            )) {
                is ResultadoAutenticacion.Exito ->
                    _estadoUi.value = EstadoUiAutenticacion(cargando = false, exito = true)
                is ResultadoAutenticacion.Error ->
                    _estadoUi.value = EstadoUiAutenticacion(cargando = false, error = resultado.mensaje)
            }
        }
    }

    // ================= HELPERS =================

    // Limpia el error de la UI
    fun limpiarError() {
        _estadoUi.update { it.copy(error = null) }
    }

    // Cierra la sesión y resetea el estado
    fun cerrarSesion() {
        if (_estadoUi.value.cargando) return
        repositorio.cerrarSesion()
        _estadoUi.value = EstadoUiAutenticacion()
    }

    // Valida el formato del email
    private fun esEmailValido(email: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return regex.matches(email.trim())
    }

    // Valida que el nombre solo contenga letras, números y guiones bajos
    private fun esNombreValido(nombre: String): Boolean {
        val regex = Regex("^[A-Za-z0-9_áéíóúÁÉÍÓÚñÑ]+$")
        return regex.matches(nombre)
    }
}