package com.yassine.inventory

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.yassine.inventory.data.PaymentStatus
import java.io.File
import java.util.Locale
import kotlin.math.min

object InvoicePdf {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40

    fun createAndShare(context: Context, data: InvoiceData): Boolean = runCatching {
        val file = createPdf(context, data) ?: return@runCatching false
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "invoice", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.invoice_share_title)))
        true
    }.getOrDefault(false)

    fun createPdf(context: Context, data: InvoiceData): File? =
        runCatching {
            val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val file = File(
                dir,
                "${context.getString(R.string.invoice_filename_prefix)}_${data.number}.pdf"
            )
            file.outputStream().use { out ->
                val doc = PdfDocument()
                val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
                draw(context, page.canvas, data)
                doc.finishPage(page)
                doc.writeTo(out)
                doc.close()
            }
            file
        }.getOrNull()

    private fun draw(context: Context, canvas: Canvas, data: InvoiceData) {
        val shop = ShopSettingsStore.read(context)
        val design = InvoiceDesignStore.read(context)
        val accent = design.accentColor.toInt()
        val dark = 0xFF211A17.toInt()
        val muted = 0xFF6B5D54.toInt()
        val rowFill = 0xFFF7EFE9.toInt()
        val hairline = 0xFFE3D6CB.toInt()
        val dueRed = 0xFFC62828.toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }
        val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 9f
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dark
            textSize = 10.5f
        }
        val wordsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dark
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val thPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dark
            textSize = 10f
        }
        val moneyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dark
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        val totalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dark
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        val duePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dueRed
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rowFill }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = hairline
            strokeWidth = 1f
        }

        val contentW = PAGE_W - 2 * MARGIN

        // Header band with logo
        val bandTop = 40f
        val bandBottom = 92f
        canvas.drawRect(0f, bandTop, PAGE_W.toFloat(), bandBottom, bandPaint)

        var titleX = MARGIN.toFloat()
        val logo = loadLogo(context, design)
        if (logo != null) {
            val box = 44f
            val scale = min(box / logo.height, box / logo.width)
            val w = logo.width * scale
            val h = logo.height * scale
            val logoY = (bandTop + bandBottom) / 2f - h / 2f
            canvas.drawBitmap(
                logo, null,
                RectF(MARGIN.toFloat(), logoY, MARGIN + w, logoY + h), null,
            )
            titleX = MARGIN + w + 14f
        }
        val titleWidth = (PAGE_W - MARGIN).toInt() - titleX.toInt() - 220
        drawText(canvas, shop.shopName, titleX, 52f, titlePaint, titleWidth)
        drawText(
            canvas,
            "${context.getString(R.string.invoice_number)} ${data.number}",
            (PAGE_W - MARGIN - 220).toFloat(),
            56f,
            numPaint,
            220,
            Layout.Alignment.ALIGN_OPPOSITE,
        )

        // Shop legal strip
        val shopInfo = if (design.showLegalStrip) {
            listOf(shop.shopAddress, shop.shopPhone, shop.shopRC, shop.shopICE)
                .filter { it.isNotBlank() }
                .joinToString("   ·   ")
        } else {
            ""
        }
        if (shopInfo.isNotBlank()) {
            drawText(canvas, shopInfo, MARGIN.toFloat(), 100f, labelPaint, contentW)
        }

        // Meta block
        var y = if (shopInfo.isNotBlank()) 118f else 112f
        val metaCol = 300
        val dateLabel = context.getString(R.string.invoice_date)
        drawText(canvas, dateLabel, MARGIN.toFloat(), y, labelPaint, metaCol)
        drawText(
            canvas,
            data.date,
            MARGIN + 90f,
            y,
            bodyPaint,
            metaCol - 90,
            Layout.Alignment.ALIGN_OPPOSITE,
        )
        y += 16f

        if (data.client.isNotBlank()) {
            drawText(canvas, context.getString(R.string.invoice_client_label), MARGIN.toFloat(), y, labelPaint, metaCol)
            drawText(
                canvas,
                data.client,
                MARGIN + 90f,
                y,
                bodyPaint,
                metaCol - 90,
                Layout.Alignment.ALIGN_OPPOSITE,
            )
            y += 16f
        }
        if (data.phone.isNotBlank()) {
            drawText(canvas, context.getString(R.string.invoice_phone_label), MARGIN.toFloat(), y, labelPaint, metaCol)
            drawText(
                canvas,
                data.phone,
                MARGIN + 90f,
                y,
                bodyPaint,
                metaCol - 90,
                Layout.Alignment.ALIGN_OPPOSITE,
            )
            y += 16f
        }

        // Items table
        y += 10f
        val colQty = 36
        val colUnit = 84
        val colTotal = 84
        val colDesc = contentW - colQty - colUnit - colTotal - 32
        val headerH = 22
        canvas.drawRect(MARGIN.toFloat(), y, (PAGE_W - MARGIN).toFloat(), y + headerH, bandPaint)
        drawText(canvas, "Qty", MARGIN + 4f, y + 7f, thPaint, colQty)
        drawText(canvas, "Description", MARGIN + colQty + 4f, y + 7f, thPaint, colDesc)
        drawText(
            canvas, "Unit (DH)", MARGIN + colQty + colDesc + 8f, y + 7f, thPaint, colUnit,
            Layout.Alignment.ALIGN_OPPOSITE,
        )
        drawText(
            canvas, "Total (DH)", MARGIN + colQty + colDesc + colUnit + 12f, y + 7f, thPaint, colTotal,
            Layout.Alignment.ALIGN_OPPOSITE,
        )
        y += headerH

        data.lines.forEachIndexed { index, line ->
            if (index % 2 == 0) {
                canvas.drawRect(
                    MARGIN.toFloat(), y, (PAGE_W - MARGIN).toFloat(), y + 34f, rowPaint,
                )
            }
            drawText(canvas, line.quantity.toString(), MARGIN + 4f, y + 6f, cellPaint, colQty)
            drawText(canvas, line.description, MARGIN + colQty + 4f, y + 6f, cellPaint, colDesc, maxLines = 2)
            drawText(
                canvas, money(line.unitPrice),
                MARGIN + colQty + colDesc + 8f, y + 6f, moneyPaint, colUnit,
                Layout.Alignment.ALIGN_OPPOSITE,
            )
            drawText(
                canvas, money(line.total),
                MARGIN + colQty + colDesc + colUnit + 12f, y + 6f, moneyPaint, colTotal,
                Layout.Alignment.ALIGN_OPPOSITE,
            )
            y += 36f
        }

        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_W - MARGIN).toFloat(), y, linePaint)

        // Totals
        y += 16f
        val totalCol = 220
        val totalLeft = (PAGE_W - MARGIN - totalCol).toFloat()
        val totalValueX = (PAGE_W - MARGIN - 110).toFloat()
        val breakdown = design.showVatBreakdown

        val subtotalLabel = if (breakdown) {
            context.getString(R.string.invoice_total_ht)
        } else {
            context.getString(R.string.invoice_subtotal)
        }
        drawText(
            canvas, "$subtotalLabel:",
            totalLeft, y, bodyPaint, totalCol - 110, Layout.Alignment.ALIGN_OPPOSITE,
        )
        drawText(
            canvas, if (breakdown) money(data.afterDiscount) else money(data.subtotal),
            totalValueX, y, moneyPaint, 110, Layout.Alignment.ALIGN_OPPOSITE,
        )
        y += 18f

        if (data.discountPercent > 0) {
            drawText(
                canvas,
                "${context.getString(R.string.invoice_discount)} ${data.discountPercent}%:",
                totalLeft, y, bodyPaint, totalCol - 110, Layout.Alignment.ALIGN_OPPOSITE,
            )
            drawText(
                canvas, "-" + money(data.discountAmount),
                totalValueX, y, moneyPaint, 110, Layout.Alignment.ALIGN_OPPOSITE,
            )
            y += 18f
        }

        if (data.vatPercent > 0) {
            drawText(
                canvas,
                "${context.getString(R.string.invoice_vat)} (${"%.2f".format(Locale.US, data.vatPercent)}%):",
                totalLeft, y, bodyPaint, totalCol - 110, Layout.Alignment.ALIGN_OPPOSITE,
            )
            drawText(
                canvas, money(data.vatAmount),
                totalValueX, y, moneyPaint, 110, Layout.Alignment.ALIGN_OPPOSITE,
            )
            y += 18f
        }

        canvas.drawLine(totalLeft, y, (PAGE_W - MARGIN).toFloat(), y, linePaint)
        y += 10f
        val totalLabel = if (breakdown) {
            context.getString(R.string.invoice_total_ttc)
        } else {
            context.getString(R.string.invoice_total)
        }
        drawText(
            canvas, "$totalLabel:",
            totalLeft, y, totalPaint, totalCol - 110, Layout.Alignment.ALIGN_OPPOSITE,
        )
        drawText(
            canvas, money(data.total),
            totalValueX, y, totalPaint, 110, Layout.Alignment.ALIGN_OPPOSITE,
        )

        // Outstanding balance
        if (design.showPaymentStatus && data.paymentStatus != PaymentStatus.CASH) {
            y += 22f
            drawText(
                canvas, "${context.getString(R.string.invoice_due)}:",
                totalLeft, y, duePaint, totalCol - 110, Layout.Alignment.ALIGN_OPPOSITE,
            )
            drawText(
                canvas, money(data.dueAmount),
                totalValueX, y, duePaint, 110, Layout.Alignment.ALIGN_OPPOSITE,
            )
        }

        // Amount in letters
        if (design.showAmountWords) {
            y += 26f
            drawText(
                canvas, AmountWords.sentence(data.total),
                MARGIN.toFloat(), y.coerceAtMost(PAGE_H - 120f), wordsPaint, contentW, maxLines = 3,
            )
        }

        // Footer
        val paymentLabel = when (data.paymentStatus) {
            PaymentStatus.CASH -> context.getString(R.string.invoice_payment_cash)
            PaymentStatus.PARTIAL -> context.getString(R.string.status_partial)
            PaymentStatus.CREDIT -> context.getString(R.string.status_credit)
        }
        val footer = "${context.getString(R.string.invoice_payment)}: $paymentLabel"
        drawText(
            canvas, footer,
            MARGIN.toFloat(), PAGE_H - 44f, labelPaint, contentW,
            Layout.Alignment.ALIGN_CENTER,
        )
        if (design.showFooter) {
            drawText(
                canvas, context.getString(R.string.invoice_thanks),
                MARGIN.toFloat(), PAGE_H - 26f, bodyPaint, contentW,
                Layout.Alignment.ALIGN_CENTER,
            )
        }
    }

    private fun loadLogo(context: Context, design: InvoiceDesign): Bitmap? {
        if (!design.logoEnabled) return null
        val file = InvoiceDesignStore.logoFile(context)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.path)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: TextPaint,
        width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(align)
            .setTextDirection(TextDirectionHeuristics.LOCALE)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun money(value: Double): String =
        String.format(Locale.FRANCE, "%,.2f", value) + " DH"
}