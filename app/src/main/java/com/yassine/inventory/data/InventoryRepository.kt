package com.yassine.inventory.data

import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val dao: ItemDao) {

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

    suspend fun getBySku(sku: String): Item? = dao.getBySku(sku)

    suspend fun getById(id: Long): Item? = dao.getById(id)

    suspend fun upsert(item: Item) {
        if (item.id == 0L) {
            dao.insert(item)
        } else {
            dao.update(item)
        }
    }

    suspend fun delete(item: Item) {
        dao.delete(item)
    }
}