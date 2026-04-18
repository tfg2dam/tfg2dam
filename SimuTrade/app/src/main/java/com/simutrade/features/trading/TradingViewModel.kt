package com.simutrade.features.trading

class TradingViewModel {

    fun parseQuantity(input: String): Double {
        return input.toDoubleOrNull() ?: 0.0
    }

    fun calculateTotal(price: Double, quantity: Double): Double {
        return price * quantity
    }

    fun canBuy(quantity: Double, price: Double, balance: Double): Boolean {
        return quantity > 0 && (quantity * price) <= balance
    }

    fun canSell(quantity: Double, holdingQuantity: Double?): Boolean {
        return holdingQuantity != null &&
                quantity > 0 &&
                quantity <= holdingQuantity
    }
}