package com.simutrade.screens.trading

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.*
import com.simutrade.screens.main.MainViewModel
import com.simutrade.screens.main.Screen
import com.simutrade.screens.user.UserViewModel
import kotlinx.coroutines.launch

@Composable
fun TradingScreen(
    mainViewModel: MainViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {

    val selectedAsset by mainViewModel.selectedAsset.collectAsStateWithLifecycle()
    val userData by userViewModel.userData.collectAsStateWithLifecycle()
    val portfolio by userViewModel.portfolio.collectAsStateWithLifecycle()

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
                onNavigateToMarket = {
                    mainViewModel.clearSelectedAsset()
                    mainViewModel.navigateTo(Screen.Market)
                }
            )

        } else {

            val asset = selectedAsset ?: return@Scaffold
            val currentHolding = portfolio.find { it.assetId == asset.id }

            val quantityDouble = parseQuantity(quantity)
            val total = calculateTotal(asset.currentPrice, quantityDouble)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ================= HEADER =================
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {
                        Text(
                            "Operar",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Compra y vende activos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(onClick = {
                        mainViewModel.clearSelectedAsset()
                        mainViewModel.navigateTo(Screen.Market)
                    }) {
                        Text("Volver")
                    }
                }

                // ================= INFO ACTIVO =================
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {

                        Text(asset.symbol, fontWeight = FontWeight.Bold)
                        Text(asset.name)

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "€${"%.2f".format(asset.currentPrice)}",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        currentHolding?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("Tienes: ${it.quantity}")
                        }
                    }
                }

                // ================= TABS =================
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

                // ================= FORMULARIOS =================
                when (selectedTab) {

                    // ===== COMPRAR =====
                    0 -> BuyForm(
                        asset = asset,
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        total = total,
                        balance = userData.balance,
                        enabled = canBuy(quantityDouble, asset.currentPrice, userData.balance),
                        onBuy = {
                            if (quantityDouble <= 0) return@BuyForm

                            userViewModel.buyAsset(asset, quantityDouble) { result ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        when (result) {
                                            is OperationResult.Success -> "Compra realizada"
                                            is OperationResult.Error -> result.message
                                        }
                                    )
                                    quantity = ""
                                }
                            }
                        }
                    )

                    // ===== VENDER =====
                    1 -> SellForm(
                        asset = asset,
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        total = total,
                        currentHolding = currentHolding,
                        enabled = canSell(quantityDouble, currentHolding?.quantity),
                        onSell = {
                            if (quantityDouble <= 0) return@SellForm

                            userViewModel.sellAsset(
                                asset.id,
                                quantityDouble,
                                asset.currentPrice
                            ) { result ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        when (result) {
                                            is OperationResult.Success -> "Venta realizada"
                                            is OperationResult.Error -> result.message
                                        }
                                    )
                                    quantity = ""
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

//////////////////////////////////////////////////////
// EMPTY
//////////////////////////////////////////////////////

@Composable
fun NoAssetSelected(
    modifier: Modifier = Modifier,
    onNavigateToMarket: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            Icons.Default.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text("Selecciona un activo", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(8.dp))

        Text("Ve al mercado para empezar")

        Spacer(Modifier.height(24.dp))

        Button(onClick = onNavigateToMarket) {
            Text("Ir al mercado")
        }
    }
}

//////////////////////////////////////////////////////
// BUY
//////////////////////////////////////////////////////

@Composable
fun BuyForm(
    asset: Asset,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    total: Double,
    balance: Double,
    enabled: Boolean,
    onBuy: () -> Unit
) {
    val quantityDouble = parseQuantity(quantity)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = quantity.isNotEmpty() && quantityDouble <= 0
        )

        Text(
            "Disponible: €${"%.2f".format(balance)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            "Total: €${"%.2f".format(total)}",
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onBuy,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && quantity.isNotBlank()
        ) {
            Text("Comprar ${asset.symbol}")
        }
    }
}

//////////////////////////////////////////////////////
// SELL
//////////////////////////////////////////////////////

@Composable
fun SellForm(
    asset: Asset,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    total: Double,
    currentHolding: PortfolioHolding?,
    enabled: Boolean,
    onSell: () -> Unit
) {
    val quantityDouble = parseQuantity(quantity)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = quantity.isNotEmpty() && quantityDouble <= 0
        )

        Text(
            "Recibirás: €${"%.2f".format(total)}",
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onSell,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && quantity.isNotBlank()
        ) {
            Text("Vender ${asset.symbol}")
        }
    }
}

//////////////////////////////////////////////////////
// HELPERS
//////////////////////////////////////////////////////

private fun parseQuantity(input: String): Double =
    input
        .replace(",", ".")
        .filter { it.isDigit() || it == '.' }
        .toDoubleOrNull() ?: 0.0

private fun calculateTotal(price: Double, quantity: Double): Double =
    price * quantity

private fun canBuy(quantity: Double, price: Double, balance: Double): Boolean =
    quantity > 0 && (quantity * price) <= balance

private fun canSell(quantity: Double, holdingQuantity: Double?): Boolean =
    holdingQuantity != null && quantity > 0 && quantity <= holdingQuantity