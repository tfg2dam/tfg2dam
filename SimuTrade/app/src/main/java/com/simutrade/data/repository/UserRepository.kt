package com.simutrade.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simutrade.data.model.*
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val uid get() = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

    suspend fun getUserData(): UserData {
        val doc = firestore.collection("Usuarios").document(uid).get().await()
        return UserData(
            idUsuario     = doc.getString("id_usuario") ?: "",
            nombreUsuario = doc.getString("nombre_usuario") ?: "",
            email         = doc.getString("email") ?: "",
            saldo         = doc.getDouble("saldo") ?: 100.0,
            saldoInicial  = doc.getDouble("saldo_inicial") ?: 100.0,
            idRango       = doc.getString("id_rango") ?: "bronce",
            creadoEn      = doc.getLong("creado_en") ?: 0L,
            ultimoLogin   = doc.getLong("ultimo_login") ?: 0L
        )
    }

    suspend fun updateSaldo(nuevoSaldo: Double) {
        firestore.collection("Usuarios").document(uid)
            .update("saldo", nuevoSaldo).await()
    }

    suspend fun updateRango(idRango: String) {
        firestore.collection("Usuarios").document(uid)
            .update("id_rango", idRango).await()
    }

    suspend fun getCartera(): List<PortfolioHolding> {
        val snapshot = firestore.collection("Cartera")
            .whereEqualTo("id_usuario", uid)
            .get().await()
        return snapshot.documents.map { doc ->
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
    }

    suspend fun upsertCartera(holding: PortfolioHolding) {
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
        firestore.collection("Cartera").document(docId).set(data).await()
    }

    suspend fun deleteCartera(assetId: String) {
        firestore.collection("Cartera").document("${uid}_${assetId}").delete().await()
    }

    suspend fun getTransacciones(): List<Transaction> {
        val snapshot = firestore.collection("Transacciones")
            .whereEqualTo("id_usuario", uid)
            .orderBy("ejecutado_en", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.map { doc ->
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
    }

    suspend fun addTransaccion(transaction: Transaction) {
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
        firestore.collection("Transacciones").add(data).await()
    }

    suspend fun getLeaderboard(): List<LeaderboardEntry> {
        val snapshot = firestore.collection("Usuarios").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val saldo = doc.getDouble("saldo") ?: return@mapNotNull null
            val saldoInicial = doc.getDouble("saldo_inicial") ?: 100.0
            val nombre = doc.getString("nombre_usuario") ?: return@mapNotNull null
            val idRango = doc.getString("id_rango") ?: "bronce"
            val beneficio = saldo - saldoInicial
            LeaderboardEntry(
                id = doc.id,
                username = nombre,
                profit = beneficio,
                rank = idRango.replaceFirstChar { it.uppercaseChar() },
                portfolioValue = saldo
            )
        }.sortedByDescending { it.profit }
    }

    fun calcularValorCartera(cartera: List<PortfolioHolding>): Double =
        cartera.sumOf { it.quantity * it.currentPrice }

    fun calcularValorTotal(saldo: Double, valorCartera: Double): Double =
        saldo + valorCartera

    fun calcularBeneficio(valorTotal: Double, saldoInicial: Double): Double =
        valorTotal - saldoInicial

    fun calcularBeneficioPct(valorTotal: Double, saldoInicial: Double): Double =
        ((valorTotal - saldoInicial) / saldoInicial) * 100
}
