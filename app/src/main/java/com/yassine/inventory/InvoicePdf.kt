package com.yassine.inventory

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

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
        val accent = 0xFFC4540F.toInt()
        val dark = 0xFF211A17.toInt()
        val muted = 0xFF6B5D54.toInt()
        val rowFill = 0xFFF7EFE9.toInt()
        val hairline = 0xFFE3D6CB.toInt()

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
        val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rowFill }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = hairline
            strokeWidth = 1f
        }

        val contentW = PAGE_W - 2 * MARGIN

        // Header band
        canvas.drawRect(0f, 40f, PAGE_W.toFloat(), 92f, bandPaint)
        drawText(canvas, shop.shopName, MARGIN.toFloat(), 52f, titlePaint, 320)
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
        val shopInfo = listOf(shop.shopAddress, shop.shopPhone, shop.shopRC, shop.shopICE)
            .filter { it.isNotBlank() }
            .joinToString("   ·   ")
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
        drawText(
            canvas, "${context.getString(R.string.invoice_subtotal)}:",
            totalLeft, y, bodyPaint, totalCol - 110, Layout.Alignment.ALIGN_OPPOSITE,
        )
        drawText(
            canvas, money(data.subtotal),
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
        drawText(
            canvas, "${context.getString(R.string.invoice_total)}:",
            totalLeft, y, totalPaint, totalCol - 110, Layout.Alignment.ALIGN_OPPOSITE,
        )
        drawText(
            canvas, money(data.total),
            totalValueX, y, totalPaint, 110, Layout.Alignment.ALIGN_OPPOSITE,
        )

        // Footer
        val footer = "${context.getString(R.string.invoice_payment)}: ${context.getString(R.string.invoice_payment_cash)}"
        drawText(
            canvas, footer,
            MARGIN.toFloat(), PAGE_H - 44f, labelPaint, contentW,
            Layout.Alignment.ALIGN_CENTER,
        )
        drawText(
            canvas, context.getString(R.string.invoice_thanks),
            MARGIN.toFloat(), PAGE_H - 26f, bodyPaint, contentW,
            Layout.Alignment.ALIGN_CENTER,
        )
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