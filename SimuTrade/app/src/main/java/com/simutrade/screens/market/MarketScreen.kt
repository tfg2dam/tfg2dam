package com.simutrade.screens.market

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Asset
import com.simutrade.data.model.AssetType
import com.simutrade.screens.main.MainViewModel
import com.simutrade.screens.theme.positive
import kotlinx.coroutines.delay

@Composable
fun MarketScreen(
    mainViewModel: MainViewModel,
    marketViewModel: MarketViewModel = viewModel()
) {

    val uiState by marketViewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // ⏱ TIMER
    LaunchedEffect(uiState.lastUpdated) {
        if (uiState.lastUpdated > 0) {
            while (true) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val filters = listOf("Todos", "Acciones", "Cripto")
    val isSearching = searchQuery.isNotEmpty()

    val displayedAssets = when {
        isSearching -> uiState.searchResults
        selectedFilter == "Acciones" -> uiState.stocks
        selectedFilter == "Cripto" -> uiState.cryptos
        else -> uiState.stocks + uiState.cryptos
    }

    val hasData = uiState.stocks.isNotEmpty() || uiState.cryptos.isNotEmpty()

    // 🔥 ANIMACIÓN SOLO CUANDO REFRESCA
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ================= HEADER =================
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                "Mercado",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {

                if (uiState.lastUpdated > 0) {
                    val seconds =
                        ((currentTime - uiState.lastUpdated) / 1000).toInt()

                    Text(
                        "Hace ${seconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.width(6.dp))
                }

                // 🔥 BOTÓN CON ANIMACIÓN REAL
                IconButton(
                    onClick = {
                        marketViewModel.loadMarketData(force = true)
                    }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        modifier = Modifier.rotate(
                            if (uiState.isRefreshing) rotation else 0f
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ================= BUSCADOR =================
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                marketViewModel.search(it)
            },
            placeholder = { Text("Buscar activos...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // ================= FILTROS =================
        if (!isSearching) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ================= CONTENIDO =================
        Box(modifier = Modifier.fillMaxSize()) {

            // 🔥 LOADING SOLO PRIMERA VEZ
            if (uiState.isInitialLoading && !hasData) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // ❌ ERROR SOLO SIN DATOS
            if (uiState.error != null && !hasData) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 📊 LISTA SIEMPRE
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(displayedAssets) { asset ->
                    AssetCard(
                        asset = asset,
                        onClick = { mainViewModel.selectAsset(asset) }
                    )
                }

                // 🔥 LOADER ABAJO CUANDO REFRESCA
                if (uiState.isRefreshing) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

//////////////////////////////////////////////////////
// CARD
//////////////////////////////////////////////////////

@Composable
fun AssetCard(
    asset: Asset,
    onClick: () -> Unit
) {

    val isPositive = asset.priceChangePercent24h >= 0

    val changeColor =
        if (isPositive) MaterialTheme.colorScheme.positive
        else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,
                    color = if (asset.type == AssetType.STOCK)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = asset.symbol.first().toString(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column {
                    Text(asset.symbol, fontWeight = FontWeight.Bold)

                    Text(
                        asset.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {

                Text(
                    "€${"%.2f".format(asset.currentPrice)}",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${if (isPositive) "+" else ""}${"%.2f".format(asset.priceChangePercent24h)}%",
                    color = changeColor
                )
            }
        }
    }
}