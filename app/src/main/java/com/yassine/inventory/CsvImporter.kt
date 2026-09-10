package com.yassine.inventory

import com.yassine.inventory.data.Item

object CsvImporter {

    fun parse(text: String): List<Item> {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return emptyList()

        val header = rows.first().map { it.trim().lowercase() }

        fun colIndex(vararg names: String): Int? =
            names.firstNotNullOfOrNull { n -> header.indexOf(n).takeIf { it >= 0 } }

        val iName = colIndex("name", "product", "tyre")
        val iSku = colIndex("sku", "barcode", "code")
        val iBrand = colIndex("brand", "marque")
        val iModel = colIndex("model", "pattern", "modele")
        val iRim = colIndex("rim_size", "diameter", "rim", "rimsize")
        val iWidth = colIndex("width", "largeur")
        val iProfile = colIndex("profile", "aspect_ratio", "aspectratio", "shape")
        val iSeason = colIndex("season", "type")
        val iLoad = colIndex("load_index", "loadindex")
        val iSpeed = colIndex("speed_index", "speedindex")
        val iCategory = colIndex("category")
        val iSub = colIndex("sub_category", "subcategory")
        val iQty = colIndex("quantity", "qty", "stock", "count")
        val iMin = colIndex("min_quantity", "min", "min_qty", "threshold")
        val iPrice = colIndex("price", "prix")
        val iCost = colIndex("cost_price", "cost", "costprice", "prix_achat", "prixachat")
        val iSupplier = colIndex("supplier", "fournisseur", "provider", "vendor")
        val iNotes = colIndex("notes", "remark", "note")

        fun cell(row: List<String>, index: Int?): String {
            if (index == null) return ""
            return if (index in row.indices) row[index].trim() else ""
        }

        return buildList {
            for (row in rows.drop(1)) {
                val name = cell(row, iName)
                if (name.isBlank()) continue

                val rim = cell(row, iRim).toIntOrNull() ?: extractRimFromName(name)
                val width = cell(row, iWidth).toIntOrNull() ?: extractWidthFromName(name)
                val profile = cell(row, iProfile).toIntOrNull() ?: extractProfileFromName(name)
                val subCategory = cell(row, iSub).ifBlank { guessSubCategory(name, width, profile, rim) }
                val category = cell(row, iCategory).ifBlank {
                    if (rim > 0) "${rim}\"" else ""
                }

                add(
                    Item(
                        name = name,
                        sku = cell(row, iSku),
                        rimDiameter = rim,
                        width = width,
                        profile = profile,
                        brand = cell(row, iBrand),
                        model = cell(row, iModel),
                        season = cell(row, iSeason),
                        loadIndex = cell(row, iLoad),
                        speedIndex = cell(row, iSpeed),
                        category = category,
                        subCategory = subCategory,
                        quantity = cell(row, iQty).toIntOrNull() ?: 0,
                        minQuantity = cell(row, iMin).toIntOrNull() ?: 0,
                        price = cell(row, iPrice).toDoubleOrNull() ?: 0.0,
                        costPrice = cell(row, iCost).toDoubleOrNull() ?: 0.0,
                        supplier = cell(row, iSupplier),
                        notes = cell(row, iNotes),
                    )
                )
            }
        }
    }

    private fun extractRimFromName(name: String): Int {
        val raw = name.replace(" ", "")
        val match = Regex("""(\d{2})R(\d{1,2})""").find(raw)
        return match?.groupValues?.get(2)?.toIntOrNull() ?: 0
    }

    private fun extractWidthFromName(name: String): Int {
        val raw = name.replace(" ", "")
        val match = Regex("""(\d{2,3})/(\d{2})R?""").find(raw)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun extractProfileFromName(name: String): Int {
        val raw = name.replace(" ", "")
        val match = Regex("""(\d{2,3})/(\d{2})R?""").find(raw)
        return match?.groupValues?.get(2)?.toIntOrNull() ?: 0
    }

    private fun guessSubCategory(name: String, width: Int, profile: Int, rim: Int): String {
        var w = width
        var p = profile
        var r = rim
        val raw = name.replace(" ", "")
        val full = Regex("""(\d{2,3})/(\d{2})R(\d{1,2})""").find(raw)
        if (full != null) {
            w = full.groupValues[1].toIntOrNull() ?: w
            p = full.groupValues[2].toIntOrNull() ?: p
            r = full.groupValues[3].toIntOrNull() ?: r
        }
        if (w > 0 && p > 0 && r > 0) return "$w/$p R$r"
        return name
    }

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var field = StringBuilder()
        val row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        val s = text.removePrefix("\uFEFF")

        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes -> when (c) {
                    '"' -> if (i + 1 < s.length && s[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }

                    else -> field.append(c)
                }

                c == '"' -> inQuotes = true
                c == ',' -> {
                    row.add(field.toString())
                    field = StringBuilder()
                }

                c == '\n' || c == '\r' -> {
                    row.add(field.toString())
                    field = StringBuilder()
                    if (row.any { it.isNotEmpty() }) rows.add(row.toList())
                    row.clear()
                }

                else -> field.append(c)
            }
            i++
        }

        field.toString().let { last ->
            if (last.isNotEmpty()) row.add(last)
        }
        if (row.isNotEmpty() && row != listOf("")) rows.add(row.toList())
        return rows
    }
}