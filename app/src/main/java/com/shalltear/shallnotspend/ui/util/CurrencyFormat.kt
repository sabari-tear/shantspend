package com.shalltear.shallnotspend.ui.util

import com.shalltear.shallnotspend.model.AppPreferences
import com.shalltear.shallnotspend.model.CurrencyType

fun currencySymbol(): String {
    return when (AppPreferences.selectedCurrency) {
        CurrencyType.USD -> "$"
        CurrencyType.EUR -> "€"
        CurrencyType.GBP -> "£"
        CurrencyType.INR -> "₹"
    }
}

fun formatCurrency(amount: Double): String {
    return "${currencySymbol()}${String.format("%.2f", amount)}"
}

fun formatSignedCurrency(amount: Double, sign: String): String {
    return "$sign${currencySymbol()}${String.format("%.2f", amount)}"
}

fun formatAmountInputLabel(): String {
    return "Amount (${currencySymbol().trim()})"
}