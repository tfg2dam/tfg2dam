package com.simutrade.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Activo
import com.simutrade.data.model.ActivoEnCartera
import com.simutrade.data.model.Rango
import com.simutrade.data.model.TipoTransaccion
import com.simutrade.data.model.Transaccion
import com.simutrade.screens.main.MainViewModel
import com.simutrade.screens.theme.positive
import com.simutrade.screens.user.UserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()

    val usuario = uiState.usuario
    val cartera = uiState.cartera
    val transacciones = uiState.transacciones
    val rangoActual = uiState.rangoActual

    val valorCartera = cartera.sumOf {
        it.cantidad * it.precioActual
    }

    val valorTotal = usuario.saldo + valorCartera

    val beneficio = valorTotal - usuario.saldoInicial

    val porcentajeBeneficio =
        if (usuario.saldoInicial > 0) {
            (beneficio / usuario.saldoInicial) * 100
        } else {
            0.0
        }

    val porcentajeEfectivo =
        if (valorTotal > 0) {
            (usuario.saldo / valorTotal) * 100
        } else {
            0.0
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = "Resumen",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TarjetaResumen(
                    titulo = "Total",
                    valor = "€${"%.2f".format(valorTotal)}",
                    subtitulo =
                        "Ganancia " +
                                if (beneficio >= 0) "+" else "-" +
                                        "€${"%.2f".format(abs(beneficio))} (" +
                                        "%.0f".format(porcentajeBeneficio) +
                                        "%)",
                    icono = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )

                TarjetaResumen(
                    titulo = "Efectivo",
                    valor = "€${"%.2f".format(usuario.saldo)}",
                    subtitulo =
                        "Disponible (" +
                                "%.0f".format(porcentajeEfectivo) +
                                "%)",
                    icono = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TarjetaResumen(
                    titulo = "Cartera",
                    valor = "€${"%.2f".format(valorCartera)}",
                    subtitulo = "${cartera.size} activos",
                    icono = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                rangoActual?.let {
                    TarjetaResumen(
                        titulo = "Rango",
                        valor = it.nombre,
                        subtitulo = "Nivel actual",
                        icono = Icons.Default.EmojiEvents,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Text(
                text = "Mi cartera",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (cartera.isEmpty()) {
            item {
                TarjetaVacia(
                    texto = "Aún no tienes inversiones"
                )
            }
        } else {
            items(cartera) { activo ->
                TarjetaActivoCartera(
                    activo = activo,
                    onClick = {
                        mainViewModel.seleccionarActivo(
                            activo.toActivo()
                        )
                    }
                )
            }
        }

        item {
            Text(
                text = "Últimas transacciones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (transacciones.isEmpty()) {
            item {
                TarjetaVacia(
                    texto = "Aún no hay movimientos"
                )
            }
        } else {
            items(transacciones.take(10)) { transaccion ->
                TarjetaTransaccion(
                    transaccion = transaccion
                )
            }
        }
    }
}

@Composable
fun TarjetaActivoCartera(
    activo: ActivoEnCartera,
    onClick: () -> Unit
) {
    val valorActual =
        activo.cantidad * activo.precioActual

    val invertido =
        activo.cantidad * activo.precioPromedio

    val beneficio =
        valorActual - invertido

    val porcentaje =
        if (invertido > 0) {
            (beneficio / invertido) * 100
        } else {
            0.0
        }

    val color =
        if (beneficio >= 0) {
            MaterialTheme.colorScheme.positive
        } else {
            MaterialTheme.colorScheme.error
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = activo.simbolo,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "€${"%.2f".format(valorActual)}"
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Invertido: €${"%.2f".format(invertido)}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    (if (beneficio >= 0) "+" else "-") +
                            "€${"%.2f".format(abs(beneficio))} (" +
                            "%.2f".format(porcentaje) +
                            "%)",
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TarjetaTransaccion(
    transaccion: Transaccion
) {
    val esCompra =
        transaccion.tipo == TipoTransaccion.COMPRA

    val color =
        if (esCompra) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.positive
        }

    val formatoFecha = SimpleDateFormat(
        "dd MMM · HH:mm",
        Locale.getDefault()
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transaccion.simbolo,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        (if (esCompra) "-" else "+") +
                                "€${"%.2f".format(transaccion.total)}",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    (if (esCompra) "Compra" else "Venta") +
                            " · " +
                            formatoFecha.format(
                                Date(transaccion.fecha)
                            ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun ActivoEnCartera.toActivo(): Activo {
    return Activo(
        id = idActivo,
        simbolo = simbolo,
        nombre = nombre,
        tipo = tipo,
        precioActual = precioActual,
        cambioPrecio24h = 0.0,
        cambioPorcentaje24h = 0.0
    )
}

@Composable
fun TarjetaVacia(
    texto: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(texto)
        }
    }
}

@Composable
fun TarjetaResumen(
    titulo: String,
    valor: String,
    subtitulo: String,
    icono: ImageVector,
    modifier: Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(titulo)

                Icon(
                    imageVector = icono,
                    contentDescription = null
                )
            }

            Text(
                text = valor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Suppress("unused")
@Composable
fun TarjetaProgresoRango(
    siguienteRango: Rango,
    beneficioActual: Double,
    progreso: Float
) {
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        label = ""
    )

    val restante =
        (siguienteRango.beneficioMinimo - beneficioActual)
            .coerceAtLeast(0.0)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Siguiente rango: ${siguienteRango.nombre}",
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LinearProgressIndicator(
                progress = { progresoAnimado },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Te faltan €${"%.2f".format(restante)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}