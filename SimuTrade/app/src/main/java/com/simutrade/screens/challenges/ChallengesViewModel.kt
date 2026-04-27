package com.simutrade.screens.challenges

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.model.*
import com.simutrade.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class ChallengeValidation(
    val completed: Boolean,
    val message: String
)

class ChallengesViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _challengesData = MutableStateFlow(ChallengesData())
    val challengesData: StateFlow<ChallengesData> = _challengesData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _millisUntilReset = MutableStateFlow(0L)
    val millisUntilReset: StateFlow<Long> = _millisUntilReset.asStateFlow()

    fun loadChallenges() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = repository.getChallengesData()
                val updated = checkDailyReset(data)
                _challengesData.value = updated
                calculateResetTime()
            } catch (e: Exception) {
                Log.e("ChallengesVM", "Error en retos", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ================= RESET DIARIO =================

    private suspend fun checkDailyReset(data: ChallengesData): ChallengesData {

        val todayStart = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (data.currentDay == 0L) {
            val initial = data.copy(
                currentDay = todayStart,
                dailyChallenges = generateRandomChallenges()
            )
            repository.saveChallengesData(initial)
            return initial
        }

        if (data.currentDay != todayStart) {

            val yesterday = todayStart - (24 * 60 * 60 * 1000)

            // 🔥 IMPORTANTE: ya NO sumamos aquí
            val newStreak =
                if (data.currentDay == yesterday && allChallengesCompleted(data))
                    data.currentStreak
                else
                    0

            val reset = data.copy(
                currentStreak = newStreak,
                maxStreak = maxOf(data.maxStreak, newStreak),
                completedChallenges = emptyList(),
                dailyChallenges = generateRandomChallenges(),
                currentDay = todayStart
            )

            repository.saveChallengesData(reset)
            return reset
        }

        return data
    }

    private fun calculateResetTime() {
        val now = Calendar.getInstance(TimeZone.getDefault())

        val tomorrow = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        _millisUntilReset.value = tomorrow.timeInMillis - now.timeInMillis
    }

    // ================= GENERADOR =================

    private fun generateRandomChallenges(): List<String> {
        val pool = listOf(
            "operation",
            "diversify",
            "trader",
            "multimarket",
            "profit"
        )
        return pool.shuffled().take(3)
    }

    // ================= RETOS DEL DÍA =================

    fun getChallengesOfDay(): List<Challenge> {

        val data = _challengesData.value

        return data.dailyChallenges.mapIndexed { index, type ->

            val id = "challenge_${type}_${data.currentDay}_${index + 1}"

            when (type) {

                "operation" -> Challenge(
                    id,
                    "Haz tu primera operación",
                    "Compra o vende cualquier activo hoy",
                    "operation",
                    1.0
                )

                "diversify" -> Challenge(
                    id,
                    "Diversifica tu cartera",
                    "Ten al menos 2 inversiones diferentes",
                    "diversify",
                    1.5
                )

                "multimarket" -> Challenge(
                    id,
                    "Invierte en varios mercados",
                    "Invierte en acciones y criptomonedas",
                    "multimarket",
                    2.0
                )

                "trader" -> Challenge(
                    id,
                    "Actividad alta",
                    "Realiza 3 operaciones hoy",
                    "trader",
                    2.5
                )

                "profit" -> Challenge(
                    id,
                    "Consigue beneficios",
                    "Haz que tu cartera esté en positivo",
                    "profit",
                    3.0
                )

                else -> Challenge(
                    "error",
                    "Error",
                    "No se pudo cargar el reto",
                    "error",
                    0.0
                )
            }
        }
    }

    private fun allChallengesCompleted(data: ChallengesData): Boolean {
        val ids = data.dailyChallenges.mapIndexed { index, type ->
            "challenge_${type}_${data.currentDay}_${index + 1}"
        }
        return ids.all { it in data.completedChallenges }
    }

    // ================= VALIDACIÓN =================

    suspend fun validateChallenge(id: String): ChallengeValidation {

        val type = getTypeFromId(id)

        val portfolio = repository.getPortfolio()
        val transactions = repository.getTransactions()
        val user = repository.getUserData()

        val todayStart = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayTx = transactions.filter { it.date >= todayStart }

        return when (type) {

            "operation" ->
                if (todayTx.isNotEmpty())
                    ChallengeValidation(true, "Ya has hecho una operación hoy")
                else
                    ChallengeValidation(false, "Haz una compra o una venta")

            "trader" -> {
                val remaining = 3 - todayTx.size
                if (todayTx.size >= 3)
                    ChallengeValidation(true, "Has completado las 3 operaciones")
                else
                    ChallengeValidation(false, "Te faltan $remaining operaciones")
            }

            "diversify" -> {
                val distinct = portfolio.map { it.assetId }.distinct().size
                val remaining = 2 - distinct

                if (distinct >= 2)
                    ChallengeValidation(true, "Ya tienes varias inversiones")
                else
                    ChallengeValidation(false, "Te falta ${remaining} inversión más")
            }

            "multimarket" -> {
                val stock = portfolio.any { it.type == AssetType.STOCK }
                val crypto = portfolio.any { it.type == AssetType.CRYPTO }

                when {
                    stock && crypto ->
                        ChallengeValidation(true, "Ya inviertes en ambos mercados")

                    !stock && !crypto ->
                        ChallengeValidation(false, "Empieza invirtiendo en cualquier activo")

                    !stock ->
                        ChallengeValidation(false, "Te falta invertir en acciones")

                    else ->
                        ChallengeValidation(false, "Te falta invertir en criptomonedas")
                }
            }

            "profit" -> {
                val portfolioValue = repository.calculatePortfolioValue(portfolio)
                val total = repository.calculateTotalValue(user.balance, portfolioValue)
                val profit = repository.calculateProfit(total, user.initialBalance)

                if (profit > 0)
                    ChallengeValidation(true, "Vas ganando €${"%.2f".format(profit)}")
                else
                    ChallengeValidation(false, "Tu cartera aún no está en positivo")
            }

            else -> ChallengeValidation(false, "Reto desconocido")
        }
    }

    private fun getTypeFromId(id: String): String {
        return when {
            id.contains("operation") -> "operation"
            id.contains("diversify") -> "diversify"
            id.contains("trader") -> "trader"
            id.contains("multimarket") -> "multimarket"
            id.contains("profit") -> "profit"
            else -> ""
        }
    }

    // ================= COMPLETAR (🔥 NUEVO SISTEMA) =================

    fun completeChallenge(
        id: String,
        reward: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {

            val validation = validateChallenge(id)

            if (!validation.completed) {
                onResult(false, validation.message)
                return@launch
            }

            val data = _challengesData.value

            if (id in data.completedChallenges) {
                onResult(false, "Ya has completado este reto")
                return@launch
            }

            val alreadyCompleted = data.completedChallenges
            val totalChallenges = data.dailyChallenges.size

            // 🔥 detectar último reto
            val willCompleteAll = (alreadyCompleted.size + 1) == totalChallenges

            val newStreak =
                if (willCompleteAll) data.currentStreak + 1
                else data.currentStreak

            repository.updateBonusBalance(reward)

            val updated = data.copy(
                completedChallenges = data.completedChallenges + id,
                currentStreak = newStreak,
                maxStreak = maxOf(data.maxStreak, newStreak)
            )

            repository.saveChallengesData(updated)
            _challengesData.value = updated

            calculateResetTime()

            val message =
                if (willCompleteAll)
                    "🔥 Racha +1 · +${"%.2f".format(reward)}€"
                else
                    "+${"%.2f".format(reward)}€ añadidos"

            onResult(true, message)
        }
    }
}