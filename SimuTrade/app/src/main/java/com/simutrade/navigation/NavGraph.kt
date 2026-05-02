package com.simutrade.navigation

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
import com.simutrade.screens.auth.LoginScreen
import com.simutrade.screens.auth.RegisterScreen
import com.simutrade.screens.main.MainScreen

object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val PRINCIPAL = "principal"
}

@Composable
fun GrafoNavegacion(
    controladorNavegacion: NavHostController
) {
    // ================= AUTENTICACIÓN =================

    val autenticacion = FirebaseAuth.getInstance()

    var sesionIniciada by remember {
        mutableStateOf(autenticacion.currentUser != null)
    }

    // ================= LISTENER FIREBASE =================

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            sesionIniciada = it.currentUser != null
        }

        autenticacion.addAuthStateListener(listener)

        onDispose {
            autenticacion.removeAuthStateListener(listener)
        }
    }

    val destinoInicial = if (sesionIniciada) Rutas.PRINCIPAL else Rutas.LOGIN

    NavHost(
        navController = controladorNavegacion,
        startDestination = destinoInicial
    ) {

        // ================= LOGIN =================

        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    controladorNavegacion.navigate(Rutas.PRINCIPAL) {
                        popUpTo(controladorNavegacion.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToRegister = {
                    controladorNavegacion.navigate(Rutas.REGISTRO)
                }
            )
        }

        // ================= REGISTRO =================

        composable(Rutas.REGISTRO) {
            RegisterScreen(
                onRegisterSuccess = {
                    controladorNavegacion.navigate(Rutas.PRINCIPAL) {
                        popUpTo(controladorNavegacion.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLogin = {
                    controladorNavegacion.popBackStack()
                }
            )
        }

        // ================= PRINCIPAL =================

        composable(Rutas.PRINCIPAL) {
            MainScreen(
                onLogout = {
                    controladorNavegacion.navigate(Rutas.LOGIN) {
                        popUpTo(controladorNavegacion.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}