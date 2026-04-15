package com.simutrade.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.ui.auth.AuthViewModel
import com.simutrade.ui.challenges.EducationalScreen
import com.simutrade.ui.dashboard.DashboardScreen
import com.simutrade.ui.market.MarketScreen
import com.simutrade.ui.rankings.RankingsScreen
import com.simutrade.ui.trading.TradingScreen
import com.simutrade.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    mainViewModel: MainViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val currentPage by mainViewModel.currentPage.collectAsState()
    val userData by mainViewModel.userData.collectAsState()
    val currentRank = mainViewModel.getCurrentRank()
    var showProfileDialog by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        NavigationItem("dashboard", "Panel", Icons.Default.Dashboard),
        NavigationItem("market", "Mercado", Icons.Default.TrendingUp),
        NavigationItem("rankings", "Rankings", Icons.Default.EmojiEvents),
        NavigationItem("educational", "Retos", Icons.Default.Star)
    )

    if (showProfileDialog) {
        ProfileDialog(
            userData = userData,
            onDismiss = { showProfileDialog = false },
            onLogout = {
                showProfileDialog = false
                authViewModel.logout()
                onLogout()
            }
        )
    }

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
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
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
                        onClick = { mainViewModel.navigateTo(item.route) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentPage) {
                "dashboard"   -> DashboardScreen(mainViewModel)
                "market"      -> MarketScreen(mainViewModel)
                "trading"     -> TradingScreen(mainViewModel)
                "rankings"    -> RankingsScreen(mainViewModel)
                "educational" -> EducationalScreen(mainViewModel)
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)