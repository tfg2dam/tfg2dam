package com.simutrade.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.ui.amigos.AmigosScreen
import com.simutrade.ui.autenticacion.AutenticacionViewModel
import com.simutrade.ui.retos.RetosScreen
import com.simutrade.ui.dashboard.DashboardScreen
import com.simutrade.ui.ligas.LigasScreen
import com.simutrade.ui.mercado.MercadoScreen
import com.simutrade.ui.ranking.RankingScreen
import com.simutrade.ui.operaciones.OperacionesScreen
import com.simutrade.ui.usuario.UsuarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    mainViewModel: MainViewModel = viewModel(),
    usuarioViewModel: UsuarioViewModel = viewModel(),
    autenticacionViewModel: AutenticacionViewModel = viewModel()
) {
    val estadoUiMain by mainViewModel.estadoUi.collectAsStateWithLifecycle()
    val pantallaActual = estadoUiMain.pantallaActual

    val estadoUiUsuario by usuarioViewModel.estadoUi.collectAsStateWithLifecycle()
    val usuario = estadoUiUsuario.usuario
    val rangoActual = estadoUiUsuario.rangoActual

    var mostrarPerfilScreen by remember { mutableStateOf(false) }

    val itemsNavegacion = listOf(
        ItemNavegacion(Pantalla.Inicio,   "Panel",   Icons.Default.Dashboard),
        ItemNavegacion(Pantalla.Mercado,  "Mercado", Icons.AutoMirrored.Filled.TrendingUp),
        ItemNavegacion(Pantalla.Ranking, "Ranking", Icons.Default.EmojiEvents),
        ItemNavegacion(Pantalla.Retos,    "Retos",   Icons.Default.Star),
        ItemNavegacion(Pantalla.Amigos,   "Amigos",  Icons.Default.People),
        ItemNavegacion(Pantalla.Ligas,    "Ligas",   Icons.Default.Groups)
    )

    // Muestra el diálogo de perfil cuando el usuario pulsa el avatar
    if (mostrarPerfilScreen) {
        PerfilScreen(
            datosUsuario = usuario,
            rangoActual = rangoActual,
            onDismiss = { mostrarPerfilScreen = false },
            onLogout = {
                mostrarPerfilScreen = false
                onLogout()
            },
            autenticacionViewModel = autenticacionViewModel
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "SimuTrade", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (estadoUiUsuario.cargando && rangoActual == null) "Cargando..."
                            else "€${"%.2f".format(usuario.saldo)} • ${rangoActual?.nombre ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarPerfilScreen = true }) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Perfil")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                itemsNavegacion.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = item.icono, contentDescription = item.etiqueta) },
                        label = { Text(item.etiqueta) },
                        selected = pantallaActual == item.pantalla,
                        onClick = { mainViewModel.navegarA(item.pantalla) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (pantallaActual) {
                Pantalla.Inicio ->
                    DashboardScreen(usuarioViewModel = usuarioViewModel)

                Pantalla.Mercado ->
                    MercadoScreen(mainViewModel = mainViewModel)

                Pantalla.Trading ->
                    OperacionesScreen(
                        mainViewModel = mainViewModel,
                        usuarioViewModel = usuarioViewModel
                    )

                Pantalla.Ranking ->
                    RankingScreen(usuarioViewModel = usuarioViewModel)

                Pantalla.Retos ->
                    RetosScreen(usuarioViewModel = usuarioViewModel)

                Pantalla.Amigos ->
                    AmigosScreen()

                Pantalla.Ligas ->
                    LigasScreen(usuarioViewModel = usuarioViewModel)
            }
        }
    }
}

// ================= ITEM NAVEGACIÓN =================

data class ItemNavegacion(
    val pantalla: Pantalla,
    val etiqueta: String,
    val icono: ImageVector
)