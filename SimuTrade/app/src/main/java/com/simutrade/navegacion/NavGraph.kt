package com.simutrade.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.simutrade.ui.autenticacion.LoginScreen
import com.simutrade.ui.autenticacion.RegistroScreen
import com.simutrade.ui.main.MainScreen

// Rutas de navegación de la app
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val PRINCIPAL = "principal"
}

@Composable
fun NavGraph(controladorNavegacion: NavHostController) {

    val autenticacion = FirebaseAuth.getInstance()

    // Estado de sesión reactivo
    var sesionIniciada by remember { mutableStateOf(autenticacion.currentUser != null) }

    // Escucha cambios de sesión en Firebase y actualiza el estado
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            sesionIniciada = it.currentUser != null
        }
        autenticacion.addAuthStateListener(listener)
        onDispose { autenticacion.removeAuthStateListener(listener) }
    }

    // Decide la pantalla inicial según si hay sesión activa
    val destinoInicial = if (sesionIniciada) Rutas.PRINCIPAL else Rutas.LOGIN

    NavHost(navController = controladorNavegacion, startDestination = destinoInicial) {

        // Pantalla de login
        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    controladorNavegacion.navigate(Rutas.PRINCIPAL) {
                        popUpTo(controladorNavegacion.graph.startDestinationId) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    controladorNavegacion.navigate(Rutas.REGISTRO)
                }
            )
        }

        // Pantalla de registro
        composable(Rutas.REGISTRO) {
            RegistroScreen(
                onRegisterSuccess = {
                    controladorNavegacion.navigate(Rutas.PRINCIPAL) {
                        popUpTo(controladorNavegacion.graph.startDestinationId) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    controladorNavegacion.popBackStack()
                }
            )
        }

        // Pantalla principal tras iniciar sesión
        composable(Rutas.PRINCIPAL) {
            MainScreen(
                onLogout = {
                    controladorNavegacion.navigate(Rutas.LOGIN) {
                        popUpTo(controladorNavegacion.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }
    }
}