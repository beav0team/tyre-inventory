package com.yassine.inventory

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.yassine.inventory.data.Item
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val CSV_HEADER = listOf(
        "rim_size", "width", "profile", "brand", "model", "season",
        "load_index", "speed_index", "name", "sku", "category", "sub_category",
        "quantity", "min_quantity", "price", "notes",
    )

    fun export(items: List<Item>, context: Context): Uri {
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "tyres_$stamp.csv")
        file.writeText(toCsv(items))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun toCsv(items: List<Item>): String = buildString {
        appendLine(CSV_HEADER.map { esc(it) }.joinToString(","))
        items.forEach { item ->
            appendLine(
                listOf(
                    item.rimDiameter.takeIf { it > 0 }?.toString() ?: "",
                    item.width.takeIf { it > 0 }?.toString() ?: "",
                    item.profile.takeIf { it > 0 }?.toString() ?: "",
                    esc(item.brand),
                    esc(item.model),
                    esc(item.season),
                    esc(item.loadIndex),
                    esc(item.speedIndex),
                    esc(item.name),
                    esc(item.sku),
                    esc(item.category),
                    esc(item.subCategory),
                    item.quantity.toString(),
                    item.minQuantity.toString(),
                    String.format(Locale.US, "%.2f", item.price),
                    esc(item.notes),
                ).joinToString(",")
            )
        }
    }

    private fun esc(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""
}