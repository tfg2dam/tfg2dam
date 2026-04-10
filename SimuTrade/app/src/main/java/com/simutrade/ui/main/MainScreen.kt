package com.simutrade.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.ui.screens.*
import com.simutrade.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val currentPage by viewModel.currentPage.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val currentRank = viewModel.getCurrentRank()

    val navigationItems = listOf(
        NavigationItem("dashboard", "Panel", Icons.Default.Dashboard),
        NavigationItem("market", "Mercado", Icons.Default.TrendingUp),
        NavigationItem("rankings", "Rankings", Icons.Default.EmojiEvents),
        NavigationItem("educational", "Aprender", Icons.Default.School)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SimuTrade", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "€${String.format("%.2f", userData.saldo)} • ${currentRank.icon} ${currentRank.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                navigationItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentPage == item.route,
                        onClick = { viewModel.navigateTo(item.route) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentPage) {
                "dashboard"   -> DashboardScreen(viewModel)
                "market"      -> MarketScreen(viewModel)
                "trading"     -> TradingScreen(viewModel)
                "rankings"    -> RankingsScreen(viewModel)
                "educational" -> EducationalScreen(viewModel)
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)