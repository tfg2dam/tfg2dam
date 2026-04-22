package com.simutrade.screens.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Asset
import com.simutrade.data.model.AssetType
import com.simutrade.screens.theme.positive
import com.simutrade.screens.main.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun MarketScreen(
    mainViewModel: MainViewModel,
    marketViewModel: MarketViewModel = viewModel()
) {
    var chartAsset by remember { mutableStateOf<Asset?>(null) }
    val uiState by marketViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    chartAsset?.let { asset ->
        LaunchedEffect(asset, uiState.selectedPeriod) {
            marketViewModel.loadPriceHistory(asset, uiState.selectedPeriod)
        }
        PriceChartDialog(
            asset = asset,
            priceHistory = uiState.priceHistory,
            isLoading = uiState.isLoadingHistory,
            selectedPeriod = uiState.selectedPeriod,
            onDismiss = { chartAsset = null },
            onPeriodChange = { period ->
                marketViewModel.loadPriceHistory(asset, period)
            }
        )
    }

    val filters = listOf("Todos", "Acciones", "Cripto")
    val isSearching = searchQuery.isNotEmpty()

    val displayedAssets = when {
        isSearching -> uiState.searchResults
        selectedFilter == "Acciones" -> uiState.stocks
        selectedFilter == "Cripto" -> uiState.cryptos
        else -> uiState.stocks + uiState.cryptos
    }

    // El botón de refresh solo se activa si han pasado 60 segundos
    val puedeRefrescar = uiState.lastUpdated == 0L ||
            (currentTime - uiState.lastUpdated) >= MarketViewModel.MIN_REFRESH_MS

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            "Hace ${((currentTime - uiState.lastUpdated) / 1000).toInt()}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = { marketViewModel.loadMarketData() },
                        enabled = puedeRefrescar
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = if (puedeRefrescar)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // BUSCADOR
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    marketViewModel.search(it)
                },
                placeholder = { Text("Buscar acciones o criptos...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    when {
                        uiState.isSearching -> CircularProgressIndicator(
                            Modifier.size(20.dp), strokeWidth = 2.dp
                        )
                        searchQuery.isNotEmpty() -> IconButton(onClick = {
                            searchQuery = ""
                            marketViewModel.search("")
                        }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // FILTROS
        if (!isSearching) {
            item {
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
        }

        // LOADING
        if (uiState.isLoading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Cargando mercado...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ERROR
        uiState.error?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // CONTADOR
        if (!uiState.isLoading) {
            item {
                Text(
                    "${displayedAssets.size} activos${if (isSearching) " encontrados" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // VACÍO
        if (displayedAssets.isEmpty() && !uiState.isLoading && uiState.lastUpdated > 0) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isSearching) "Sin resultados para \"$searchQuery\""
                        else "No hay datos disponibles",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedAssets) { asset ->
                AssetCard(
                    asset = asset,
                    onClick = { mainViewModel.selectAsset(asset) },
                    onChartClick = { chartAsset = asset }
                )
            }
        }
    }
}

@Composable
fun AssetCard(
    asset: Asset,
    onClick: () -> Unit,
    onChartClick: () -> Unit
) {
    val isPositive = asset.priceChangePercent24h >= 0
    val changeColor = if (isPositive) MaterialTheme.colorScheme.positive
    else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (asset.type == AssetType.STOCK)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (asset.type == AssetType.STOCK) "S" else "C")
                    }
                }
                Column {
                    Text(asset.symbol, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Text(asset.name, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "€${String.format("%.2f", asset.currentPrice)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.TrendingUp
                        else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${if (isPositive) "+" else ""}${String.format("%.2f", asset.priceChangePercent24h)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = changeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(
                    onClick = onChartClick,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(Icons.Default.ShowChart, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Gráfico", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}