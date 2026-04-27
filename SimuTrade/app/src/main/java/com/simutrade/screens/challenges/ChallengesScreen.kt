package com.simutrade.screens.challenges

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simutrade.data.model.Challenge
import com.simutrade.screens.theme.positive
import com.simutrade.screens.theme.positiveContainer
import com.simutrade.screens.user.UserViewModel
import kotlinx.coroutines.delay

@Composable
fun ChallengesScreen(
    viewModel: ChallengesViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {

    val challengesData by viewModel.challengesData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val millisUntilReset by viewModel.millisUntilReset.collectAsStateWithLifecycle()

    val challenges = viewModel.getChallengesOfDay()

    val completed = challenges.count { it.id in challengesData.completedChallenges }
    val total = challenges.size
    val allCompleted = total > 0 && completed == total

    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var dialogSuccess by remember { mutableStateOf(false) }

    var remainingTime by remember { mutableStateOf(millisUntilReset) }

    LaunchedEffect(millisUntilReset) {
        remainingTime = millisUntilReset
        while (remainingTime > 0) {
            delay(1000)
            remainingTime -= 1000
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadChallenges()
    }

    fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00:00"
        val h = ms / (1000 * 60 * 60)
        val m = (ms % (1000 * 60 * 60)) / (1000 * 60)
        val s = (ms % (1000 * 60)) / 1000
        return "%02d:%02d:%02d".format(h, m, s)
    }

    // ================= DIALOG =================

    dialogMessage?.let {
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = {
                Text(if (dialogSuccess) "Reto completado" else "No completado")
            },
            text = { Text(it, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = { dialogMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dialogSuccess)
                            MaterialTheme.colorScheme.positive
                        else
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // ================= UI =================

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ===== TÍTULO =====
        item {
            Text(
                "Retos diarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ===== RACHA =====
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {
                        Text("Racha", fontWeight = FontWeight.Bold)

                        Text("${challengesData.currentStreak} días seguidos")

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Completa todos los retos para subir tu racha",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            "${challengesData.currentStreak}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ===== PROGRESO =====
        item {

            val progress by animateFloatAsState(
                targetValue = if (total == 0) 0f else completed.toFloat() / total,
                label = ""
            )

            Card {
                Column(Modifier.padding(16.dp)) {

                    Text(
                        "$completed de $total retos completados",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                }
            }
        }

        // ===== TODOS COMPLETADOS =====
        if (allCompleted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Todos completados", fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(6.dp))

                        Text("Nuevos retos en")

                        Text(
                            formatTime(remainingTime),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ===== LISTA =====
        if (isLoading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(challenges) { challenge ->

                val isDone = challenge.id in challengesData.completedChallenges

                ChallengeCard(
                    challenge = challenge,
                    completed = isDone,
                    onComplete = {
                        viewModel.completeChallenge(
                            challenge.id,
                            challenge.reward
                        ) { success, msg ->
                            dialogSuccess = success
                            dialogMessage = msg
                            if (success) userViewModel.loadData()
                        }
                    }
                )
            }
        }
    }
}

//////////////////////////////////////////////////////
// CARD
//////////////////////////////////////////////////////

@Composable
fun ChallengeCard(
    challenge: Challenge,
    completed: Boolean,
    onComplete: () -> Unit
) {

    val bg by animateColorAsState(
        if (completed)
            MaterialTheme.colorScheme.positiveContainer
        else
            MaterialTheme.colorScheme.surface,
        label = ""
    )

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(challenge.title, fontWeight = FontWeight.Bold)

                Text(
                    "+${"%.2f".format(challenge.reward)}€",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.positive
                )
            }

            Text(
                challenge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (completed) {
                Text(
                    "Completado",
                    color = MaterialTheme.colorScheme.positive,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Completar")
                }
            }
        }
    }
}