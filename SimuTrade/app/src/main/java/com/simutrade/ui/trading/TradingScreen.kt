package com.simutrade.ui.trading

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButtonDefaults.itemShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simutrade.data.model.*
import com.simutrade.ui.theme.positive
import com.simutrade.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun TradingScreen(viewModel: MainViewModel) {

    val selectedAsset by viewModel.selectedAsset.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val cartera by viewModel.cartera.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var quantity by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (selectedAsset == null) {
            NoAssetSelected(
                modifier = Modifier.padding(padding),
                onNavigateToMarket = { viewModel.navigateTo("market") }
            )
        } else {

            val asset = selectedAsset!!
            val currentHolding = cartera.find { it.assetId == asset.id }
            val quantityDouble = quantity.toDoubleOrNull() ?: 0.0
            val total = quantityDouble * asset.currentPrice

            val positiveColor = MaterialTheme.colorScheme.positive
            val negativeColor = MaterialTheme.colorScheme.error

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // HEADER
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Operar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Compra y vende activos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    OutlinedButton(onClick = {
                        viewModel.clearSelectedAsset()
                        viewModel.navigateTo("market")
                    }) {
                        Text("Volver")
                    }
                }

                // INFO ACTIVO
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(asset.symbol,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)

                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(if (asset.type == AssetType.STOCK) "Acción" else "Cripto")
                                }
                            )
                        }

                        Text(asset.name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.height(16.dp))

                        val isPositive = asset.priceChangePercent24h >= 0
                        val color = if (isPositive) positiveColor else negativeColor

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("€${"%.2f".format(asset.currentPrice)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold)

                            Spacer(Modifier.width(8.dp))

                            Icon(
                                if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = color
                            )

                            Spacer(Modifier.width(4.dp))

                            Text(
                                "${if (isPositive) "+" else ""}${"%.2f".format(asset.priceChangePercent24h)}%",
                                color = color
                            )
                        }

                        if (currentHolding != null) {
                            Spacer(Modifier.height(16.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Posición actual")
                                    Text("${currentHolding.quantity} unidades",
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 🔥 NUEVOS BOTONES (ANTES TABROW)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            quantity = ""
                        },
                        shape = itemShape(0, 2)
                    ) {
                        Text("Comprar")
                    }

                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            quantity = ""
                        },
                        enabled = currentHolding != null,
                        shape = itemShape(1, 2)
                    ) {
                        Text("Vender")
                    }
                }

                // FORMULARIOS
                when (selectedTab) {

                    0 -> BuyForm(
                        asset = asset,
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        total = total,
                        balance = userData.saldo,
                        onBuy = {
                            viewModel.buyAsset(asset, quantityDouble) { result ->
                                scope.launch {
                                    when (result) {
                                        is OperationResult.Success -> {
                                            snackbarHostState.showSnackbar("Compra realizada correctamente")
                                            quantity = ""
                                        }
                                        is OperationResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            }
                        }
                    )

                    1 -> SellForm(
                        asset = asset,
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        total = total,
                        currentHolding = currentHolding,
                        onSell = {
                            viewModel.sellAsset(asset.id, quantityDouble, asset.currentPrice) { result ->
                                scope.launch {
                                    when (result) {
                                        is OperationResult.Success -> {
                                            snackbarHostState.showSnackbar("Venta realizada correctamente")
                                            quantity = ""
                                        }
                                        is OperationResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NoAssetSelected(
    modifier: Modifier = Modifier,
    onNavigateToMarket: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Selecciona un activo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Ve al mercado para empezar")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNavigateToMarket) {
            Text("Ir al Mercado")
        }
    }
}

@Composable
fun BuyForm(
    asset: Asset,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    total: Double,
    balance: Double,
    onBuy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Total: €${"%.2f".format(total)}", fontWeight = FontWeight.Bold)

        Button(
            onClick = onBuy,
            modifier = Modifier.fillMaxWidth(),
            enabled = quantity.toDoubleOrNull()?.let { it > 0 && total <= balance } == true
        ) {
            Text("Comprar ${asset.symbol}")
        }
    }
}

@Composable
fun SellForm(
    asset: Asset,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    total: Double,
    currentHolding: PortfolioHolding?,
    onSell: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Recibirás: €${"%.2f".format(total)}", fontWeight = FontWeight.Bold)

        Button(
            onClick = onSell,
            modifier = Modifier.fillMaxWidth(),
            enabled = currentHolding != null
        ) {
            Text("Vender ${asset.symbol}")
        }
    }
}