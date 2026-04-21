package com.simutrade.screens.rankings

import com.simutrade.data.model.Rank

object RankUtils {

    // ================= RANKS =================

    val ranks = listOf(
        Rank(
            "Bronce",
            Double.NEGATIVE_INFINITY,
            "#CD7F32",
            "🥉",
            "Novato en el mundo de las inversiones"
        ),
        Rank("Plata", 50.0, "#C0C0C0", "🥈", "Inversor con conocimientos básicos"),
        Rank("Oro", 150.0, "#FFD700", "🥇", "Trader experimentado"),
        Rank("Platino", 300.0, "#E5E4E2", "💎", "Experto en mercados financieros"),
        Rank("Diamante", 500.0, "#B9F2FF", "👑", "Maestro de las inversiones")
    )

    fun getRankFromProfit(profit: Double): Rank {
        return ranks.lastOrNull { profit >= it.minProfit } ?: ranks.first()
    }
}