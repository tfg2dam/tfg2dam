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
    val currentRank by userViewModel.currentRank.collectAsStateWithLifecycle()

    var showProfileDialog by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        NavigationItem(Screen.Dashboard, "Panel", Icons.Default.Dashboard),
        NavigationItem(Screen.Market, "Mercado", Icons.Default.TrendingUp),
        NavigationItem(Screen.Rankings, "Ranking", Icons.Default.EmojiEvents),
        NavigationItem(Screen.Challenges, "Retos", Icons.Default.Star)
    )

    // ================= PERFIL =================

    if (showProfileDialog) {
        ProfileDialog(
            userData = userData,
            currentRank = currentRank,
            onDismiss = { showProfileDialog = false },
            onLogout = {
                showProfileDialog = false
                authViewModel.logout()
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
                        Text("SimuTrade", style = MaterialTheme.typography.titleLarge)

                        Text(
                            "€${"%.2f".format(userData.balance)} • ${
                                currentRank?.name ?: ""
                            }",
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
                        selected = currentPage == item.screen,
                        onClick = { mainViewModel.navigateTo(item.screen) }
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

                Screen.Dashboard -> DashboardScreen()

                Screen.Market -> MarketScreen(mainViewModel)

                Screen.Trading -> TradingScreen(mainViewModel)

                Screen.Rankings -> RankingsScreen()

                Screen.Challenges -> {
                    LaunchedEffect(Unit) {
                        userViewModel.loadData()
                    }
                    ChallengesScreen(userViewModel = userViewModel)
                }
            }
        }
    }
}

// ================= NAV ITEM =================

data class NavigationItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)