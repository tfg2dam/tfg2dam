package com.simutrade.screens.rankings

import com.simutrade.data.model.Rank

// 🔥 ICONOS DE RANGO (SOLO LÓGICA)
enum class RankIcon {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND;

    // 👉 Nombre visible para UI (evita "Bronze Bronce")
    fun displayName(): String = when (this) {
        BRONZE -> "Bronce"
        SILVER -> "Plata"
        GOLD -> "Oro"
        PLATINUM -> "Platino"
        DIAMOND -> "Diamante"
    }
}

object RankUtils {

    val ranks = listOf(

        Rank(
            name = "Bronce",
            minProfit = 0.0,
            color = "#CD7F32",
            icon = RankIcon.BRONZE,
            description = "Estás empezando"
        ),

        Rank(
            name = "Plata",
            minProfit = 50.0,
            color = "#C0C0C0",
            icon = RankIcon.SILVER,
            description = "Vas por buen camino"
        ),

        Rank(
            name = "Oro",
            minProfit = 150.0,
            color = "#FFD700",
            icon = RankIcon.GOLD,
            description = "Buen nivel de trading"
        ),

        Rank(
            name = "Platino",
            minProfit = 300.0,
            color = "#E5E4E2",
            icon = RankIcon.PLATINUM,
            description = "Nivel avanzado"
        ),

        Rank(
            name = "Diamante",
            minProfit = 500.0,
            color = "#B9F2FF",
            icon = RankIcon.DIAMOND,
            description = "Nivel experto"
        )
    )

    // 🔥 OBTENER RANGO ACTUAL
    fun getRankFromProfit(profit: Double): Rank {
        return ranks
            .sortedBy { it.minProfit }
            .lastOrNull { profit >= it.minProfit }
            ?: ranks.first()
    }

    // 🔥 SIGUIENTE RANGO (para progreso)
    fun getNextRank(current: Rank): Rank? {
        val index = ranks.indexOfFirst { it.name == current.name }
        return if (index != -1 && index < ranks.size - 1) {
            ranks[index + 1]
        } else null
    }

    // 🔥 PROGRESO ENTRE RANGOS (0f - 1f)
    fun getProgressToNextRank(
        profit: Double,
        current: Rank,
        next: Rank?
    ): Float {

        if (next == null) return 1f

        val progress = (profit - current.minProfit) /
                (next.minProfit - current.minProfit)

        return progress.coerceIn(0.0, 1.0).toFloat()
    }

    // 🔥 DINERO QUE FALTA PARA SUBIR
    fun getRemainingToNextRank(
        profit: Double,
        next: Rank?
    ): Double {
        return if (next == null) 0.0
        else (next.minProfit - profit).coerceAtLeast(0.0)
    }
}