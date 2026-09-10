package com.yassine.inventory

import com.yassine.inventory.data.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemTest {

    @Test
    fun sizeSpecFormatsPadded() {
        val item = Item(name = "X", width = 205, profile = 55, rimDiameter = 16)
        assertEquals("205/55 R16", item.sizeSpec)
        assertEquals("16\"  ·  205/55 R16", item.specLabel)
    }

    @Test
    fun sizeSpecRequiresAllDimensions() {
        assertEquals("", Item(name = "X", width = 205, profile = 55, rimDiameter = 0).sizeSpec)
        assertEquals("", Item(name = "X", width = 205, profile = 0, rimDiameter = 16).sizeSpec)
    }

    @Test
    fun lowStockUsesMinimum() {
        assertTrue(Item(name = "X", quantity = 2, minQuantity = 2).isLowStock)
        assertTrue(Item(name = "X", quantity = 1, minQuantity = 2).isLowStock)
        assertFalse(Item(name = "X", quantity = 3, minQuantity = 2).isLowStock)
        assertFalse(Item(name = "X", quantity = 0, minQuantity = 0).isLowStock)
    }

    @Test
    fun marginOnlyWhenCostPositiveAndLower() {
        assertTrue(Item(name = "X", price = 800.0, costPrice = 600.0).hasMargin)
        assertFalse(Item(name = "X", price = 800.0, costPrice = 0.0).hasMargin)
        assertFalse(Item(name = "X", price = 600.0, costPrice = 800.0).hasMargin)
    }

    @Test
    fun saleTitleFallsBackToName() {
        assertEquals("Michelin Primacy", Item(name = "X", brand = "Michelin", model = "Primacy").saleTitle)
        assertEquals("X", Item(name = "X").saleTitle)
    }
}