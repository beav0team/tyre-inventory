package com.yassine.inventory.data

import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val dao: ItemDao,
    private val invoiceDao: InvoiceDao,
    private val clientDao: ClientDao,
    private val movementDao: MovementDao,
) {

    fun observeAll(): Flow<List<Item>> = dao.getAll()

    fun observeByRim(rim: Int): Flow<List<Item>> = dao.getByRim(rim)

    fun observeByRimAndSubCategory(rim: Int, sizeSpec: String): Flow<List<Item>> =
        dao.getByRimAndSubCategory(rim, sizeSpec)

    fun observeByRimSubCatBrand(rim: Int, sizeSpec: String, brand: String): Flow<List<Item>> =
        dao.getByRimSubCatBrand(rim, sizeSpec, brand)

    fun observeRims(): Flow<List<Int>> = dao.getDistinctRimDiameters()

    fun observeSubCategories(rim: Int): Flow<List<String>> = dao.getDistinctSubCategories(rim)

    fun observeBrands(rim: Int, sizeSpec: String): Flow<List<String>> =
        dao.getDistinctBrandsForSubCategory(rim, sizeSpec)

    fun observeLowStock(): Flow<List<Item>> = dao.getLowStockItems()

    fun observeInvoices(): Flow<List<InvoiceEntity>> = invoiceDao.getAll()

    fun observeInvoiceLines(invoiceId: Long): Flow<List<InvoiceLineEntity>> =
        invoiceDao.getLines(invoiceId)

    fun observeClients(): Flow<List<ClientEntity>> = clientDao.getAll()

    fun observeMovements(): Flow<List<MovementEntity>> = movementDao.getAll()

    fun observeRevenue(start: Long, end: Long): Flow<Double> = invoiceDao.sumBetween(start, end)

    fun observeProfit(start: Long, end: Long): Flow<Double> = invoiceDao.profitBetween(start, end)

    suspend fun getBySku(sku: String): Item? = dao.getBySku(sku)

    suspend fun getById(id: Long): Item? = dao.getById(id)

    suspend fun getInvoice(id: Long): InvoiceEntity? = invoiceDao.getById(id)

    suspend fun getInvoiceLines(invoiceId: Long): List<InvoiceLineEntity> =
        invoiceDao.getLinesOnce(invoiceId)

    suspend fun upsert(item: Item) {
        if (item.id == 0L) {
            dao.insert(item)
        } else {
            dao.update(item)
        }
    }

    suspend fun delete(item: Item) {
        dao.delete(item)
        logMovement(
            itemId = item.id,
            itemName = item.saleTitle,
            delta = -item.quantity,
            type = MovementType.DELETE,
            note = "deleted",
        )
    }

    suspend fun insertMovement(movement: MovementEntity) {
        movementDao.insert(movement)
    }

    suspend fun logMovement(
        itemId: Long,
        itemName: String,
        delta: Int,
        type: MovementType,
        note: String = "",
    ) {
        movementDao.insert(
            MovementEntity(
                itemId = itemId,
                itemName = itemName,
                delta = delta,
                type = type,
                note = note,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun saveInvoice(
        invoice: InvoiceEntity,
        lines: List<InvoiceLineEntity>,
    ): Long = invoiceDao.insert(invoice).also { id ->
        if (lines.isNotEmpty()) {
            invoiceDao.insertLines(lines.map { it.copy(invoiceId = id) })
        }
    }

    suspend fun upsertClient(name: String, phone: String): Long? {
        if (name.isBlank()) return null
        val existing = clientDao.getByNameAndPhone(name.trim(), phone.trim())
        return if (existing != null) existing.id
        else clientDao.insert(ClientEntity(name = name.trim(), phone = phone.trim()))
    }

    suspend fun updateInvoicePayment(id: Long, paid: Double, status: PaymentStatus) {
        invoiceDao.updatePayment(id, paid, status.name)
    }
}