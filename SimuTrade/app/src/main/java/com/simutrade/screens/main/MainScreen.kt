package com.simutrade.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.screens.auth.AuthViewModel
import com.simutrade.screens.challenges.ChallengesScreen
import com.simutrade.screens.dashboard.DashboardScreen
import com.simutrade.screens.market.MarketScreen
import com.simutrade.screens.rankings.RankingsScreen
import com.simutrade.screens.trading.TradingScreen
import com.simutrade.screens.user.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    mainViewModel: MainViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {

    // ================= ESTADO PRINCIPAL =================

    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val pantallaActual =
        mainUiState.pantallaActual

    // ================= ESTADO USUARIO =================

    val userUiState by userViewModel.uiState.collectAsStateWithLifecycle()

    val usuario =
        userUiState.usuario

    val rangoActual =
        userUiState.rangoActual

    var mostrarDialogoPerfil by remember {
        mutableStateOf(false)
    }

    val itemsNavegacion = listOf(
        ItemNavegacion(
            Pantalla.Inicio,
            "Panel",
            Icons.Default.Dashboard
        ),
        ItemNavegacion(
            Pantalla.Mercado,
            "Mercado",
            Icons.AutoMirrored.Filled.TrendingUp
        ),
        ItemNavegacion(
            Pantalla.Rankings,
            "Ranking",
            Icons.Default.EmojiEvents
        ),
        ItemNavegacion(
            Pantalla.Retos,
            "Retos",
            Icons.Default.Star
        )
    )

    // ================= PERFIL =================

    if (mostrarDialogoPerfil) {
        ProfileDialog(
            datosUsuario = usuario,
            rangoActual = rangoActual,
            onDismiss = {
                mostrarDialogoPerfil = false
            },
            onLogout = {
                mostrarDialogoPerfil = false
                authViewModel.cerrarSesion()
                onLogout()
            }
        )
    }

    // ================= UI =================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SimuTrade",
                            style =
                                MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text =
                                "€${"%.2f".format(usuario.saldo)} • ${rangoActual?.nombre ?: ""}",

                            style =
                                MaterialTheme.typography.bodySmall,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                },

                actions = {
                    IconButton(
                        onClick = {
                            mostrarDialogoPerfil = true
                        }
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.AccountCircle,

                            contentDescription =
                                "Perfil"
                        )
                    }
                }
            )
        },

        bottomBar = {
            NavigationBar {
                itemsNavegacion.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icono,
                                contentDescription = item.etiqueta
                            )
                        },

                        label = {
                            Text(item.etiqueta)
                        },

                        selected =
                            pantallaActual == item.pantalla,

                        onClick = {
                            mainViewModel.navegarA(
                                item.pantalla
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when (pantallaActual) {

                Pantalla.Inicio -> {
                    DashboardScreen()
                }

                Pantalla.Mercado -> {
                    MarketScreen(
                        mainViewModel = mainViewModel
                    )
                }

                Pantalla.Trading -> {
                    TradingScreen(
                        mainViewModel = mainViewModel
                    )
                }

                Pantalla.Rankings -> {
                    RankingsScreen()
                }

                Pantalla.Retos -> {
                    LaunchedEffect(Unit) {
                        userViewModel.cargarDatos()
                    }

                    ChallengesScreen(
                        userViewModel = userViewModel
                    )
                }
            }
        }
    }
}

//////////////////////////////////////////////////////
// ITEM NAVEGACIÓN
//////////////////////////////////////////////////////

data class ItemNavegacion(
    val pantalla: Pantalla,
    val etiqueta: String,
    val icono: ImageVector
)