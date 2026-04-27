package com.simutrade.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.simutrade.screens.auth.LoginScreen
import com.simutrade.screens.auth.RegisterScreen
import com.simutrade.screens.main.MainScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
}

@Composable
fun NavGraph(navController: NavHostController) {

    // 🔥 estado reactivo (no solo lectura puntual)
    val auth = FirebaseAuth.getInstance()
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    // 🔁 listener de Firebase (clave)
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            isLoggedIn = it.currentUser != null
        }
        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val startDestination = if (isLoggedIn) Routes.MAIN else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // ================= LOGIN =================

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) // 🔥 limpia TODO el backstack
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        // ================= REGISTER =================

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0)
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ================= MAIN =================

        composable(Routes.MAIN) {
            MainScreen(
                onLogout = {
                    auth.signOut()

                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}