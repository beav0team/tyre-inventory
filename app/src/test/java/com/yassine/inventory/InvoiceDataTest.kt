package com.yassine.inventory

import com.yassine.inventory.data.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceDataTest {

    private fun line(quantity: Int, price: Double) = InvoiceLine(
        item = Item(name = "Tyre"),
        quantity = quantity,
        unitPrice = price,
    )

    private fun data(lines: List<InvoiceLine>, discount: Int, vat: Double = 20.0) =
        InvoiceData(
            number = "INV-20260910-0001",
            date = "10/09/2026",
            client = "",
            phone = "",
            lines = lines,
            discountPercent = discount,
            vatPercent = vat,
        )

    @Test
    fun subtotalIsSumOfLineTotals() {
        val d = data(listOf(line(2, 300.0), line(4, 150.0)), 0)
        assertEquals(1200.0, d.subtotal, 0.001)
    }

    @Test
    fun discountReducesAfterDiscount() {
        val d = data(listOf(line(2, 300.0)), 10)
        assertEquals(60.0, d.discountAmount, 0.001)
        assertEquals(540.0, d.afterDiscount, 0.001)
    }

    @Test
    fun vatAppliedAfterDiscount() {
        val d = data(listOf(line(2, 300.0)), 10, 20.0)
        assertEquals(108.0, d.vatAmount, 0.001)
        assertEquals(648.0, d.total, 0.001)
    }

    @Test
    fun zeroVatShippingPro() {
        val d = data(listOf(line(1, 500.0)), 0, 0.0)
        assertEquals(500.0, d.total, 0.001)
    }

    @Test
    fun discountClampedToBoundsByConstruction() {
        val d = data(listOf(line(1, 100.0)), 100)
        assertEquals(0.0, d.total, 0.001)
        assertTrue(d.discountAmount <= d.subtotal)
    }

    @Test
    fun lineDescriptionBuildsFromComponents() {
        val item = Item(
            name = "Fallback",
            brand = "Michelin",
            model = "Primacy 4",
            width = 205,
            profile = 55,
            rimDiameter = 16,
            season = "Summer",
        )
        val line = InvoiceLine(item = item, quantity = 4, unitPrice = 600.0)
        assertEquals("Michelin Primacy 4  ·  205/55 R16 · Summer", line.description)
    }

    @Test
    fun emptyLinesGiveZeroTotals() {
        val d = data(emptyList(), 5, 20.0)
        assertEquals(0.0, d.total, 0.001)
    }
}