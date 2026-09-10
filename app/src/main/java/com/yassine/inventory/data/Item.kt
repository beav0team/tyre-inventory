package com.yassine.inventory.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String = "",
    val rimDiameter: Int = 0,
    val width: Int = 0,
    val profile: Int = 0,
    val brand: String = "",
    val model: String = "",
    val season: String = "",
    val loadIndex: String = "",
    val speedIndex: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 0,
    val price: Double = 0.0,
    val notes: String = "",
    val category: String = "",
    val subCategory: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val rimLabel: String
        get() = if (rimDiameter > 0) "${rimDiameter}\"" else ""

    val sizeSpec: String
        get() = if (width > 0 && profile > 0 && rimDiameter > 0) {
            "$width/$profile R$rimDiameter"
        } else ""

    val specLabel: String
        get() = buildString {
            if (rimDiameter > 0) append("${rimDiameter}\"")
            if (sizeSpec.isNotBlank()) {
                if (isNotEmpty()) append("  ·  ")
                append(sizeSpec)
            }
        }

    val weightLabel: String
        get() = buildString {
            if (brand.isNotBlank()) append(brand)
            if (model.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(model)
            }
            if (season.isNotBlank()) {
                if (isNotEmpty()) append("  ·  ")
                append(season)
            }
            if (loadIndex.isNotBlank()) {
                if (isNotEmpty()) append("  ·  ")
                append("Load $loadIndex")
            }
            if (speedIndex.isNotBlank()) {
                if (isNotEmpty()) append("  ·  ")
                append("Speed $speedIndex")
            }
        }

    val isLowStock: Boolean
        get() = minQuantity > 0 && quantity <= minQuantity

    val saleTitle: String
        get() = buildString {
            if (brand.isNotBlank()) append(brand)
            if (model.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(model)
            }
            if (isBlank()) append(name)
        }

    val saleSubtitle: String
        get() = buildString {
            if (sizeSpec.isNotBlank()) append(sizeSpec)
            if (season.isNotBlank()) {
                if (isNotEmpty()) append("  ·  ")
                append(season)
            }
        }

    val saleMeta: String
        get() = buildString {
            if (sku.isNotBlank()) {
                append("SKU $sku")
                if (quantity > 0) append("   ")
            }
            if (quantity > 0) append("${quantity} in stock")
            if (isLowStock) append("   below min ${minQuantity}")
        }
}
