package com.yassine.inventory

import android.content.Context

data class ShopSettings(
    val shopName: String,
    val shopAddress: String,
    val shopPhone: String,
    val shopRC: String,
    val shopICE: String,
    val vatPercent: Double,
)

object ShopSettingsStore {
    private const val PREFS = "shop_settings"

    fun read(context: Context): ShopSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val appName = context.getString(R.string.app_name)
        return ShopSettings(
            shopName = prefs.getString("shop_name", appName) ?: appName,
            shopAddress = prefs.getString("shop_address", "") ?: "",
            shopPhone = prefs.getString("shop_phone", "") ?: "",
            shopRC = prefs.getString("shop_rc", "") ?: "",
            shopICE = prefs.getString("shop_ice", "") ?: "",
            vatPercent = prefs.getFloat("vat_percent", 20f).toDouble(),
        )
    }

    fun write(context: Context, settings: ShopSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("shop_name", settings.shopName)
            .putString("shop_address", settings.shopAddress)
            .putString("shop_phone", settings.shopPhone)
            .putString("shop_rc", settings.shopRC)
            .putString("shop_ice", settings.shopICE)
            .putFloat("vat_percent", settings.vatPercent.toFloat())
            .apply()
    }
}