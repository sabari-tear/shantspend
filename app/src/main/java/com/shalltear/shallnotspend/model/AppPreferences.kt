package com.shalltear.shallnotspend.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemePalette {
    NEON_MINT,
    SUNSET_CORAL,
    OCEAN_BLUE
}

enum class CurrencyType {
    USD,
    EUR,
    GBP,
    INR
}

object AppPreferences {
    private const val PREFS_NAME = "shallnotspend_settings"
    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_PROFILE_BIO = "profile_bio"
    private const val KEY_PROFILE_EMAIL_LEGACY = "profile_email"
    private const val KEY_THEME = "theme"
    private const val KEY_CURRENCY = "currency"

    private lateinit var prefs: SharedPreferences

    var profileName by mutableStateOf("Your Name")
        private set

    var profileBio by mutableStateOf("Tell something about yourself.")
        private set

    var selectedTheme by mutableStateOf(ThemePalette.NEON_MINT)
        private set

    var selectedCurrency by mutableStateOf(CurrencyType.USD)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        profileName = prefs.getString(KEY_PROFILE_NAME, "Your Name") ?: "Your Name"
        profileBio = prefs.getString(
            KEY_PROFILE_BIO,
            prefs.getString(KEY_PROFILE_EMAIL_LEGACY, "Tell something about yourself.")
        ) ?: "Tell something about yourself."

        val themeName = prefs.getString(KEY_THEME, ThemePalette.NEON_MINT.name) ?: ThemePalette.NEON_MINT.name
        selectedTheme = ThemePalette.entries.find { it.name == themeName } ?: ThemePalette.NEON_MINT

        val currencyName = prefs.getString(KEY_CURRENCY, CurrencyType.USD.name) ?: CurrencyType.USD.name
        selectedCurrency = CurrencyType.entries.find { it.name == currencyName } ?: CurrencyType.USD
    }

    fun updateProfile(name: String, bio: String) {
        profileName = name
        profileBio = bio
        prefs.edit()
            .putString(KEY_PROFILE_NAME, profileName)
            .putString(KEY_PROFILE_BIO, profileBio)
            .apply()
    }

    fun setTheme(themePalette: ThemePalette) {
        selectedTheme = themePalette
        prefs.edit().putString(KEY_THEME, themePalette.name).apply()
    }

    fun setCurrency(currencyType: CurrencyType) {
        selectedCurrency = currencyType
        prefs.edit().putString(KEY_CURRENCY, currencyType.name).apply()
    }
}
