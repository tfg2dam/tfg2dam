package com.simutrade.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.simutrade.data.model.*
import kotlinx.coroutines.tasks.await
import kotlin.math.round

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid

    companion object {
        const val USERS = "Usuarios"
        const val PORTFOLIO = "Cartera"
        const val TRANSACTIONS = "Transacciones"
        const val CHALLENGES = "Retos"

        private const val TAG = "UserRepository"
    }

    // ================= UTILS =================

    private fun roundTo2Decimals(value: Double): Double {
        return round(value * 100) / 100
    }

    // ================= USER =================

    suspend fun getUserData(): UserData {
        val userId = uid ?: return UserData()

        return try {
            val doc = firestore.collection(USERS).document(userId).get().await()

            UserData(
                userId        = doc.getString("id_usuario") ?: "",
                username      = doc.getString("nombre_usuario") ?: "",
                email         = doc.getString("email") ?: "",
                balance       = doc.getDouble("saldo") ?: 100.0,
                initialBalance= doc.getDouble("saldo_inicial") ?: 100.0,
                bonusBalance  = doc.getDouble("saldo_bonus") ?: 0.0,
                rankId        = doc.getString("id_rango") ?: "bronce",
                createdAt     = doc.getLong("creado_en") ?: 0L,
                lastLogin     = doc.getLong("ultimo_login") ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getUserData", e)
            UserData()
        }
    }

    suspend fun updateBalance(newBalance: Double) {
        val userId = uid ?: return

        try {
            firestore.collection(USERS).document(userId)
                .update("saldo", roundTo2Decimals(newBalance)).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updateBalance", e)
        }
    }

    // recompensa de retos
    suspend fun updateBonusBalance(increment: Double) {
        val userId = uid ?: return

        try {
            val doc = firestore.collection(USERS).document(userId).get().await()
            val currentBalance = doc.getDouble("saldo") ?: 0.0
            val currentBonus = doc.getDouble("saldo_bonus") ?: 0.0

            firestore.collection(USERS).document(userId)
                .update(
                    mapOf(
                        "saldo"       to roundTo2Decimals(currentBalance + increment),
                        "saldo_bonus" to roundTo2Decimals(currentBonus + increment)
                    )
                ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updateBonusBalance", e)
        }
    }

    suspend fun updateRank(rankId: String) {
        val userId = uid ?: return

        try {
            firestore.collection(USERS).document(userId)
                .update("id_rango", rankId).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updateRank", e)
        }
    }

    suspend fun updateUserStats(totalValue: Double, profit: Double) {
        val userId = uid ?: return

        try {
            firestore.collection(USERS)
                .document(userId)
                .update(
                    mapOf(
                        "portfolio_value" to roundTo2Decimals(totalValue),
                        "profit"          to roundTo2Decimals(profit)
                    )
                ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updateUserStats", e)
        }
    }

    // ================= PORTFOLIO =================

    suspend fun getPortfolio(): List<PortfolioHolding> {
        val userId = uid ?: return emptyList()

        return try {
            val snapshot = firestore.collection(PORTFOLIO)
                .whereEqualTo("id_usuario", userId)
                .get().await()

            snapshot.documents.map { doc ->
                PortfolioHolding(
                    assetId      = doc.getString("id_activo") ?: "",
                    symbol       = doc.getString("simbolo") ?: "",
                    name         = doc.getString("nombre") ?: "",
                    type         = AssetType.valueOf(doc.getString("tipo") ?: "STOCK"),
                    quantity     = doc.getDouble("cantidad") ?: 0.0,
                    averagePrice = doc.getDouble("precio_promedio") ?: 0.0,
                    currentPrice = doc.getDouble("precio_actual") ?: 0.0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getPortfolio", e)
            emptyList()
        }
    }

    suspend fun upsertPortfolio(holding: PortfolioHolding) {
        val userId = uid ?: return

        try {
            val docId = "${userId}_${holding.assetId}"

            val data = hashMapOf(
                "id_usuario"      to userId,
                "id_activo"       to holding.assetId,
                "simbolo"         to holding.symbol,
                "nombre"          to holding.name,
                "tipo"            to holding.type.name,
                "cantidad"        to holding.quantity,
                "precio_promedio" to holding.averagePrice,
                "precio_actual"   to holding.currentPrice,
                "actualizado_en"  to System.currentTimeMillis()
            )

            firestore.collection(PORTFOLIO).document(docId).set(data).await()

        } catch (e: Exception) {
            Log.e(TAG, "Error upsertPortfolio", e)
        }
    }

    suspend fun deletePortfolio(assetId: String) {
        val userId = uid ?: return

        try {
            firestore.collection(PORTFOLIO)
                .document("${userId}_${assetId}")
                .delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deletePortfolio", e)
        }
    }

    // ================= TRANSACTIONS =================

    suspend fun getTransactions(): List<Transaction> {
        val userId = uid ?: return emptyList()

        return try {
            val snapshot = firestore.collection(TRANSACTIONS)
                .whereEqualTo("id_usuario", userId)
                .orderBy("ejecutado_en", Query.Direction.DESCENDING)
                .get().await()

            snapshot.documents.map { doc ->
                Transaction(
                    id       = doc.id,
                    date     = doc.getLong("ejecutado_en") ?: 0L,
                    type     = TransactionType.valueOf(doc.getString("tipo") ?: "BUY"),
                    assetId  = doc.getString("id_activo") ?: "",
                    symbol   = doc.getString("simbolo") ?: "",
                    quantity = doc.getDouble("cantidad") ?: 0.0,
                    price    = doc.getDouble("precio") ?: 0.0,
                    total    = doc.getDouble("total") ?: 0.0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getTransactions", e)
            emptyList()
        }
    }

    suspend fun addTransaction(transaction: Transaction) {
        val userId = uid ?: return

        try {
            val data = hashMapOf(
                "id_usuario"   to userId,
                "id_activo"    to transaction.assetId,
                "simbolo"      to transaction.symbol,
                "tipo"         to transaction.type.name,
                "cantidad"     to transaction.quantity,
                "precio"       to transaction.price,
                "total"        to transaction.total,
                "ejecutado_en" to transaction.date
            )

            firestore.collection(TRANSACTIONS).add(data).await()

        } catch (e: Exception) {
            Log.e(TAG, "Error addTransaction", e)
        }
    }

    // ================= LEADERBOARD =================

    suspend fun getLeaderboard(): List<LeaderboardEntry> {
        return try {
            val snapshot = firestore.collection(USERS)
                .orderBy("profit", Query.Direction.DESCENDING)
                .get().await()

            snapshot.documents.mapNotNull { doc ->
                val username = doc.getString("nombre_usuario") ?: return@mapNotNull null
                val rankId = doc.getString("id_rango") ?: "bronce"

                LeaderboardEntry(
                    id             = doc.id,
                    username       = username,
                    profit         = doc.getDouble("profit") ?: 0.0,
                    rank           = rankId.replaceFirstChar { it.uppercaseChar() },
                    portfolioValue = doc.getDouble("portfolio_value") ?: 0.0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leaderboard", e)
            emptyList()
        }
    }

    // ================= CHALLENGES =================

    suspend fun getChallengesData(): ChallengesData {
        val userId = uid ?: return ChallengesData()

        return try {
            val doc = firestore.collection(CHALLENGES).document(userId).get().await()

            ChallengesData(
                currentStreak      = doc.getLong("racha_actual")?.toInt() ?: 0,
                maxStreak          = doc.getLong("racha_maxima")?.toInt() ?: 0,
                lastTime           = doc.getLong("ultima_vez") ?: 0L,
                completedChallenges= (doc.get("retos_completados") as? List<String>) ?: emptyList(),
                dailyChallenges    = (doc.get("retos_del_dia") as? List<String>) ?: emptyList(),
                currentDay         = doc.getLong("dia_actual") ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getChallengesData", e)
            ChallengesData()
        }
    }

    suspend fun saveChallengesData(challengesData: ChallengesData) {
        val userId = uid ?: return

        try {
            val data = hashMapOf(
                "racha_actual"      to challengesData.currentStreak,
                "racha_maxima"      to challengesData.maxStreak,
                "ultima_vez"        to challengesData.lastTime,
                "retos_completados" to challengesData.completedChallenges,
                "retos_del_dia"     to challengesData.dailyChallenges,
                "dia_actual"        to challengesData.currentDay
            )

            firestore.collection(CHALLENGES).document(userId).set(data).await()

        } catch (e: Exception) {
            Log.e(TAG, "Error saveChallengesData", e)
        }
    }

    // ================= CALCULATIONS =================

    fun calculatePortfolioValue(portfolio: List<PortfolioHolding>): Double =
        portfolio.sumOf { it.quantity * it.currentPrice }

    fun calculateTotalValue(balance: Double, portfolioValue: Double): Double =
        balance + portfolioValue

    fun calculateProfit(totalValue: Double, initialBalance: Double): Double =
        totalValue - initialBalance

    fun calculateProfitPercentage(totalValue: Double, initialBalance: Double): Double =
        ((totalValue - initialBalance) / initialBalance) * 100
}