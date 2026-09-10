package com.yassine.inventory

import android.content.Context
import com.yassine.inventory.data.Item
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InvoiceLine(
    val item: Item,
    val quantity: Int,
    val unitPrice: Double,
    val descriptionOverride: String? = null,
) {
    val total: Double get() = quantity * unitPrice

    val description: String
        get() = descriptionOverride ?: buildString {
            if (item.brand.isNotBlank()) append(item.brand)
            if (item.model.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(item.model)
            }
            if (item.sizeSpec.isNotBlank()) {
                if (isNotEmpty()) append("  ·  ")
                append(item.sizeSpec)
            }
            if (item.brand.isNotBlank() || item.model.isNotBlank()) {
                if (item.season.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(item.season)
                }
            }
            if (isBlank()) append(item.name)
        }
}

data class InvoiceData(
    val number: String,
    val date: String,
    val client: String,
    val phone: String,
    val lines: List<InvoiceLine>,
    val discountPercent: Int,
    val vatPercent: Double = 0.0,
) {
    val subtotal: Double get() = lines.sumOf { it.total }
    val discountAmount: Double get() = subtotal * discountPercent / 100.0
    val afterDiscount: Double get() = subtotal - discountAmount
    val vatAmount: Double get() = afterDiscount * vatPercent / 100.0
    val total: Double get() = afterDiscount + vatAmount
}

object InvoiceNumber {
    fun next(context: Context): String {
        val prefs = context.getSharedPreferences("invoices", Context.MODE_PRIVATE)
        val n = prefs.getInt("next", 1)
        prefs.edit().putInt("next", n + 1).apply()
        val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return "INV-$stamp-${String.format(Locale.US, "%04d", n)}"
    }
}