package com.yassine.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImporterTest {

    @Test
    fun parsesQuotedFieldsWithCommas() {
        val text = "name,brand\n\"Michelin, Primacy 4\",Michelin\n"
        val items = CsvImporter.parse(text)
        assertEquals(1, items.size)
        assertEquals("Michelin, Primacy 4", items[0].name)
        assertEquals("Michelin", items[0].brand)
    }

    @Test
    fun skipsBlankNameRows() {
        val text = "name,price\n,100\n\n\rbalo,50\n"
        val items = CsvImporter.parse(text)
        assertEquals(1, items.size)
        assertEquals("balo", items[0].name)
        assertEquals(50.0, items[0].price, 0.001)
    }

    @Test
    fun guessesSubCategoryFromName() {
        val text = "name,price\n\"Continental 205/55R16\",400\n"
        val item = CsvImporter.parse(text).first()
        assertEquals("205/55 R16", item.subCategory)
        assertEquals(16, item.rimDiameter)
        assertEquals(205, item.width)
        assertEquals(55, item.profile)
        assertEquals("16\"", item.category)
    }

    @Test
    fun readsStandardColumns() {
        val text = "sku,name,brand,model,quantity,min_quantity,price,cost_price,supplier,notes\n" +
            "T001,Sport 550,Michelin,Primacy,6,2,650,500,DistriSud,fast mover\n"
        val item = CsvImporter.parse(text).first()
        assertEquals("T001", item.sku)
        assertEquals("Michelin", item.brand)
        assertEquals("Primacy", item.model)
        assertEquals(6, item.quantity)
        assertEquals(2, item.minQuantity)
        assertEquals(650.0, item.price, 0.001)
        assertEquals(500.0, item.costPrice, 0.001)
        assertEquals("DistriSud", item.supplier)
        assertEquals("fast mover", item.notes)
    }

    @Test
    fun handlesUtf8Bom() {
        val text = "\uFEFFname,price\nX,100\n"
        assertEquals("X", CsvImporter.parse(text).first().name)
    }

    @Test
    fun handlesEmbeddedNewlinesInQuotes() {
        val text = "name,notes\nTyre,\"line one\nline two\"\n"
        val item = CsvImporter.parse(text).first()
        assertEquals("line one\nline two", item.notes)
    }

    @Test
    fun emptyTextGivesEmptyList() {
        assertTrue(CsvImporter.parse("").isEmpty())
        assertTrue(CsvImporter.parse("name,price\n").isEmpty())
    }
}