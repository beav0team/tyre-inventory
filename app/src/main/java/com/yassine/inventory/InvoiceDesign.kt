package com.yassine.inventory

import android.content.Context
import java.io.File
import java.io.InputStream

data class InvoiceDesign(
    val logoEnabled: Boolean = false,
    val accentColor: Long = InvoiceDesignStore.DEFAULT_ACCENT,
    val showAmountWords: Boolean = true,
    val showLegalStrip: Boolean = true,
    val showVatBreakdown: Boolean = true,
    val showPaymentStatus: Boolean = true,
    val showFooter: Boolean = true,
)

object InvoiceDesignStore {
    private const val PREFS = "invoice_design"
    private const val LOGO_FILE = "invoice_logo.png"

    const val DEFAULT_ACCENT = 0xFFC4540FL

    val PRESETS: List<Long> = listOf(
        0xFFC4540FL,
        0xFFB02020L,
        0xFFED5E00L,
        0xFF1E3A5FL,
        0xFF00695CL,
        0xFF2E7D32L,
        0xFF5E35B1L,
        0xFF5D4037L,
    )

    fun read(context: Context): InvoiceDesign {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return InvoiceDesign(
            logoEnabled = prefs.getBoolean("logo_enabled", false) && logoFile(context).exists(),
            accentColor = prefs.getLong("accent_color", DEFAULT_ACCENT),
            showAmountWords = prefs.getBoolean("show_amount_words", true),
            showLegalStrip = prefs.getBoolean("show_legal_strip", true),
            showVatBreakdown = prefs.getBoolean("show_vat_breakdown", true),
            showPaymentStatus = prefs.getBoolean("show_payment_status", true),
            showFooter = prefs.getBoolean("show_footer", true),
        )
    }

    fun write(context: Context, design: InvoiceDesign) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("logo_enabled", design.logoEnabled)
            .putLong("accent_color", design.accentColor)
            .putBoolean("show_amount_words", design.showAmountWords)
            .putBoolean("show_legal_strip", design.showLegalStrip)
            .putBoolean("show_vat_breakdown", design.showVatBreakdown)
            .putBoolean("show_payment_status", design.showPaymentStatus)
            .putBoolean("show_footer", design.showFooter)
            .apply()
    }

    fun logoFile(context: Context): File = File(context.filesDir, LOGO_FILE)

    fun hasLogo(context: Context): Boolean = logoFile(context).exists()

    fun saveLogo(context: Context, input: InputStream): Boolean = runCatching {
        logoFile(context).outputStream().use { out -> input.copyTo(out) }
        write(context, read(context).copy(logoEnabled = true))
        true
    }.getOrDefault(false)

    fun removeLogo(context: Context) {
        logoFile(context).delete()
        write(context, read(context).copy(logoEnabled = false))
    }
}