package com.simutrade.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    val currentPage by mainViewModel.currentPage.collectAsState()

    val userData by userViewModel.userData.collectAsStateWithLifecycle()
    val currentRank = userViewModel.getCurrentRank()

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

                "dashboard" ->
                    DashboardScreen()

                "market" ->
                    MarketScreen(mainViewModel)

                "trading" ->
                    TradingScreen(mainViewModel)

                "rankings" ->
                    RankingsScreen()

                "challenges" ->
                    ChallengesScreen()
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)