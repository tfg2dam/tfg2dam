package com.simutrade.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.simutrade.data.model.*
import kotlinx.coroutines.tasks.await
import kotlin.math.round

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

    companion object {
        const val USERS = "Usuarios"
        const val CARTERA = "Cartera"
        const val TRANSACCIONES = "Transacciones"
        const val RETOS = "Retos"
    }

// ================= UTILS =================

    private fun redondear(valor: Double): Double {
        return round(valor * 100) / 100
    }

// ================= USER =================

    suspend fun getUserData(): UserData {
        return try {
            val doc = firestore.collection(USERS).document(uid).get().await()
            UserData(
                idUsuario     = doc.getString("id_usuario") ?: "",
                nombreUsuario = doc.getString("nombre_usuario") ?: "",
                email         = doc.getString("email") ?: "",
                saldo         = doc.getDouble("saldo") ?: 100.0,
                saldoInicial  = doc.getDouble("saldo_inicial") ?: 100.0,
                idRango       = doc.getString("id_rango") ?: "bronce",
                creadoEn      = doc.getLong("creado_en") ?: 0L,
                ultimoLogin   = doc.getLong("ultimo_login") ?: 0L
            )
        } catch (e: Exception) {
            println("Error getUserData: ${e.message}")
            UserData()
        }
    }

    suspend fun updateSaldo(nuevoSaldo: Double) {
        try {
            firestore.collection(USERS).document(uid)
                .update("saldo", redondear(nuevoSaldo)).await()
        } catch (e: Exception) {
            println("Error updateSaldo: ${e.message}")
        }
    }

    suspend fun updateRango(idRango: String) {
        try {
            firestore.collection(USERS).document(uid)
                .update("id_rango", idRango).await()
        } catch (e: Exception) {
            println("Error updateRango: ${e.message}")
        }
    }

    suspend fun updateUserStats(totalValue: Double, profit: Double) {
        try {
            firestore.collection(USERS)
                .document(uid)
                .update(
                    mapOf(
                        "portfolio_value" to redondear(totalValue),
                        "profit" to redondear(profit)
                    )
                ).await()
        } catch (e: Exception) {
            println("Error updateUserStats: ${e.message}")
        }
    }

// ================= CARTERA =================

    suspend fun getCartera(): List<PortfolioHolding> {
        return try {
            val snapshot = firestore.collection(CARTERA)
                .whereEqualTo("id_usuario", uid)
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
            println("Error getCartera: ${e.message}")
            emptyList()
        }
    }

    suspend fun upsertCartera(holding: PortfolioHolding) {
        try {
            val docId = "${uid}_${holding.assetId}"

            val data = hashMapOf(
                "id_usuario"      to uid,
                "id_activo"       to holding.assetId,
                "simbolo"         to holding.symbol,
                "nombre"          to holding.name,
                "tipo"            to holding.type.name,
                "cantidad"        to holding.quantity,
                "precio_promedio" to holding.averagePrice,
                "precio_actual"   to holding.currentPrice,
                "actualizado_en"  to System.currentTimeMillis()
            )

            firestore.collection(CARTERA).document(docId).set(data).await()

        } catch (e: Exception) {
            println("Error upsertCartera: ${e.message}")
        }
    }

    suspend fun deleteCartera(assetId: String) {
        try {
            firestore.collection(CARTERA)
                .document("${uid}_${assetId}")
                .delete().await()
        } catch (e: Exception) {
            println("Error deleteCartera: ${e.message}")
        }
    }

// ================= TRANSACCIONES =================

    suspend fun getTransacciones(): List<Transaction> {
        return try {
            val snapshot = firestore.collection(TRANSACCIONES)
                .whereEqualTo("id_usuario", uid)
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
            println("Error getTransacciones: ${e.message}")
            emptyList()
        }
    }

    suspend fun addTransaccion(transaction: Transaction) {
        try {
            val data = hashMapOf(
                "id_usuario"   to uid,
                "id_activo"    to transaction.assetId,
                "simbolo"      to transaction.symbol,
                "tipo"         to transaction.type.name,
                "cantidad"     to transaction.quantity,
                "precio"       to transaction.price,
                "total"        to transaction.total,
                "ejecutado_en" to transaction.date
            )

            firestore.collection(TRANSACCIONES).add(data).await()

        } catch (e: Exception) {
            println("Error addTransaccion: ${e.message}")
        }
    }

// ================= LEADERBOARD =================

    suspend fun getLeaderboard(): List<LeaderboardEntry> {
        return try {
            val snapshot = firestore.collection(USERS)
                .orderBy("profit", Query.Direction.DESCENDING)
                .get().await()

            snapshot.documents.mapNotNull { doc ->
                val nombre = doc.getString("nombre_usuario") ?: return@mapNotNull null
                val idRango = doc.getString("id_rango") ?: "bronce"

                LeaderboardEntry(
                    id = doc.id,
                    username = nombre,
                    profit = doc.getDouble("profit") ?: 0.0,
                    rank = idRango.replaceFirstChar { it.uppercaseChar() },
                    portfolioValue = doc.getDouble("portfolio_value") ?: 0.0
                )
            }
        } catch (e: Exception) {
            println("Error leaderboard: ${e.message}")
            emptyList()
        }
    }

// ================= RETOS =================

    suspend fun getRetosData(): RetosData {
        return try {
            val doc = firestore.collection(RETOS).document(uid).get().await()

            RetosData(
                rachaActual = doc.getLong("racha_actual")?.toInt() ?: 0,
                rachaMaxima = doc.getLong("racha_maxima")?.toInt() ?: 0,
                ultimaVez = doc.getLong("ultima_vez") ?: 0L,
                retosCompletados = (doc.get("retos_completados") as? List<String>) ?: emptyList(),
                diaActual = doc.getLong("dia_actual")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            println("Error getRetosData: ${e.message}")
            RetosData()
        }
    }

    suspend fun saveRetosData(retosData: RetosData) {
        try {
            val data = hashMapOf(
                "racha_actual" to retosData.rachaActual,
                "racha_maxima" to retosData.rachaMaxima,
                "ultima_vez" to retosData.ultimaVez,
                "retos_completados" to retosData.retosCompletados,
                "dia_actual" to retosData.diaActual
            )

            firestore.collection(RETOS).document(uid).set(data).await()

        } catch (e: Exception) {
            println("Error saveRetosData: ${e.message}")
        }
    }

// ================= CALCULOS =================

    fun calcularValorCartera(cartera: List<PortfolioHolding>): Double =
        cartera.sumOf { it.quantity * it.currentPrice }

    fun calcularValorTotal(saldo: Double, valorCartera: Double): Double =
        saldo + valorCartera

    fun calcularBeneficio(valorTotal: Double, saldoInicial: Double): Double =
        valorTotal - saldoInicial

    fun calcularBeneficioPct(valorTotal: Double, saldoInicial: Double): Double =
        ((valorTotal - saldoInicial) / saldoInicial) * 100

}