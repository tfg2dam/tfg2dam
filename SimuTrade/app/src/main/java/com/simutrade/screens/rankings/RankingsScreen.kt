package com.simutrade.screens.rankings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.screens.theme.positive
import com.simutrade.screens.user.UserViewModel
import kotlin.math.round

@Composable
fun RankingsScreen(
    userViewModel: UserViewModel = viewModel(),
    rankingsViewModel: RankingsViewModel = viewModel()
) {
    val userUiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val rankingsUiState by rankingsViewModel.uiState.collectAsStateWithLifecycle()

    val usuario = userUiState.usuario
    val cartera = userUiState.cartera
    val ranking = rankingsUiState.ranking
    val cargando = rankingsUiState.cargando
    val error = rankingsUiState.error

    val valorCartera = cartera.sumOf { it.valorActual }
    val valorTotal = usuario.saldo + valorCartera

    val beneficio = (usuario.saldo - usuario.saldoBonus + valorCartera) - usuario.saldoInicial
    val beneficioRedondeado = round(beneficio * 100) / 100.0

    val colorBeneficio = when {
        beneficioRedondeado > 0 -> MaterialTheme.colorScheme.positive
        beneficioRedondeado < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val posicion = rankingsViewModel.obtenerPosicionUsuario(usuario.idUsuario)

    LaunchedEffect(Unit) {
        userViewModel.cargarDatos()
        rankingsViewModel.cargarRanking()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ================= TÍTULO =================

        item {
            Text(
                text = "Ranking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ================= TARJETA USUARIO =================

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(text = "Tu posición")

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (posicion != -1)
                                "#$posicion · ${usuario.nombreUsuario}"
                            else
                                usuario.nombreUsuario,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = (if (beneficioRedondeado > 0) "+" else "") +
                                    "€${"%.2f".format(beneficioRedondeado)}",
                            fontWeight = FontWeight.Bold,
                            color = colorBeneficio
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "Total: €${"%.2f".format(valorTotal)}")
                    Text(text = "Cartera: €${"%.2f".format(valorCartera)}")
                    Text(text = "Efectivo: €${"%.2f".format(usuario.saldo)}")

                    userUiState.rangoActual?.let { rango ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Rango: ${rango.nombre}")
                    }
                }
            }
        }

        // ================= CABECERA =================

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Top inversores", fontWeight = FontWeight.Bold)

                IconButton(
                    onClick = { rankingsViewModel.cargarRanking() },
                    enabled = !cargando
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar"
                    )
                }
            }
        }

        // ================= ERROR =================

        if (error != null) {
            item {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }

        // ================= CARGANDO =================

        if (cargando) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // ================= LISTA =================

        if (!cargando) {
            itemsIndexed(ranking) { index, entrada ->

                val esMiUsuario = entrada.id == usuario.idUsuario

                val colorEntrada = when {
                    entrada.beneficio > 0 -> MaterialTheme.colorScheme.positive
                    entrada.beneficio < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (esMiUsuario)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entrada.nombreUsuario,
                                    fontWeight = FontWeight.Bold
                                )
                                if (esMiUsuario) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "tú",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = (if (entrada.beneficio > 0) "+" else "") +
                                        "€${"%.2f".format(entrada.beneficio)}",
                                color = colorEntrada,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (esMiUsuario) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Total: €${"%.2f".format(entrada.valorTotal)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Cartera: €${"%.2f".format(entrada.valorCartera)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Efectivo: €${"%.2f".format(entrada.saldo)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}