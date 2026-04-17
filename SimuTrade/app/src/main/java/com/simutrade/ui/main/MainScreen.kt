package com.simutrade.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.UserData
import com.simutrade.data.mock.MockData
import com.simutrade.ui.auth.AuthViewModel
import com.simutrade.ui.challenges.ChallengesScreen
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

    // 🔒 evitar crash inicial
    val userData by mainViewModel.userData.collectAsState(initial = UserData())

    val currentRank = mainViewModel.getCurrentRank() ?: MockData.ranks.first()

    var showProfileDialog by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        NavigationItem("dashboard", "Panel", Icons.Default.Dashboard),
        NavigationItem("market", "Mercado", Icons.Default.TrendingUp),
        NavigationItem("rankings", "Rankings", Icons.Default.EmojiEvents),
        NavigationItem("challenges", "Retos", Icons.Default.Star)
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
                            "€${"%.2f".format(userData.saldo)} • ${currentRank.icon} ${currentRank.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentPage) {
                "dashboard"  -> DashboardScreen(mainViewModel)
                "market"     -> MarketScreen(mainViewModel)
                "trading"    -> TradingScreen(mainViewModel)
                "rankings"   -> RankingsScreen(mainViewModel)
                "challenges" -> ChallengesScreen(mainViewModel)
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)