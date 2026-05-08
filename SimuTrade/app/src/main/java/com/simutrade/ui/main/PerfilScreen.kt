package com.simutrade.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simutrade.datos.modelo.DatosUsuario
import com.simutrade.datos.modelo.Rango
import com.simutrade.ui.autenticacion.AutenticacionViewModel
import com.simutrade.ui.tema.ColorBronce
import com.simutrade.ui.tema.ColorDiamante
import com.simutrade.ui.tema.ColorOro
import com.simutrade.ui.tema.ColorPlata
import com.simutrade.ui.tema.ColorPlatino
import com.simutrade.ui.tema.ColorPrincipiante

@Composable
fun PerfilScreen(
    datosUsuario: DatosUsuario,
    rangoActual: Rango?,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    autenticacionViewModel: AutenticacionViewModel
) {
    val context = LocalContext.current
    val nombreUsuario = datosUsuario.nombreUsuario
    val inicial = nombreUsuario.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val nombreRango = rangoActual?.nombre ?: ""
    val iconoRango = if (rangoActual != null) obtenerIconoRango(nombreRango) else null
    val colorRango = if (rangoActual != null) obtenerColorRango(nombreRango) else null

    var mostrarConfirmacionSalir by remember { mutableStateOf(false) }

    // ================= CONFIRMAR LOGOUT =================

    if (mostrarConfirmacionSalir) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionSalir = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres salir?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionSalir = false
                        autenticacionViewModel.cerrarSesion(context)
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionSalir = false }) { Text("Cancelar") }
            }
        )
    }

    // ================= DIÁLOGO PRINCIPAL =================

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = inicial, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column {
                        Text(text = nombreUsuario.ifBlank { "Usuario" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = datosUsuario.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (datosUsuario.codigoUsuario.isNotBlank()) {
                            Text(text = "#${datosUsuario.codigoUsuario}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                HorizontalDivider()

                FilaPerfil(icono = Icons.Default.AccountBalanceWallet, etiqueta = "Saldo disponible", valor = "€${"%.2f".format(datosUsuario.saldo)}")
                FilaPerfil(icono = Icons.Default.Savings, etiqueta = "Saldo inicial", valor = "€${"%.2f".format(datosUsuario.saldoInicial)}")
                FilaPerfil(icono = Icons.Default.Star, etiqueta = "Bonus", valor = "€${"%.2f".format(datosUsuario.saldoBonus)}")

                if (rangoActual != null && iconoRango != null && colorRango != null) {
                    FilaPerfilConIcono(
                        icono = Icons.Default.EmojiEvents,
                        etiqueta = "Rango actual",
                        valor = nombreRango,
                        iconoValor = iconoRango,
                        colorValor = colorRango
                    )
                }

                HorizontalDivider()

                TextButton(
                    onClick = { mostrarConfirmacionSalir = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
            }
        },
        confirmButton = {}
    )
}

// ================= FILA NORMAL =================

@Composable
fun FilaPerfil(icono: ImageVector, etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = valor, fontWeight = FontWeight.Medium)
    }
}

// ================= FILA CON ICONO DE VALOR =================

@Composable
fun FilaPerfilConIcono(icono: ImageVector, etiqueta: String, valor: String, iconoValor: ImageVector, colorValor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = iconoValor, contentDescription = null, modifier = Modifier.size(18.dp), tint = colorValor)
            Text(text = valor, fontWeight = FontWeight.Medium)
        }
    }
}

// ================= HELPERS =================

fun obtenerIconoRango(nombreRango: String): ImageVector {
    return when (nombreRango.lowercase()) {
        "principiante" -> Icons.Default.Star
        "bronce" -> Icons.Default.MilitaryTech
        "plata" -> Icons.Default.MilitaryTech
        "oro" -> Icons.Default.EmojiEvents
        "platino" -> Icons.Default.WorkspacePremium
        "diamante" -> Icons.Default.Diamond
        else -> Icons.Default.EmojiEvents
    }
}

@Composable
fun obtenerColorRango(nombreRango: String): Color {
    return when (nombreRango.lowercase()) {
        "principiante" -> ColorPrincipiante
        "bronce" -> ColorBronce
        "plata" -> ColorPlata
        "oro" -> ColorOro
        "platino" -> ColorPlatino
        "diamante" -> ColorDiamante
        else -> MaterialTheme.colorScheme.primary
    }
}