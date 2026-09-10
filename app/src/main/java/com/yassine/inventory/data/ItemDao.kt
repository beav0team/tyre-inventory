package com.yassine.inventory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY rimDiameter ASC, width ASC, profile ASC, brand ASC, name ASC")
    fun getAll(): Flow<List<Item>>

    @Query(
        """SELECT * FROM items
        WHERE name LIKE '%' || :q || '%'
        OR brand LIKE '%' || :q || '%'
        OR model LIKE '%' || :q || '%'
        OR sku LIKE '%' || :q || '%'
        OR category LIKE '%' || :q || '%'
        OR subCategory LIKE '%' || :q || '%'
        ORDER BY rimDiameter ASC, width ASC, profile ASC, brand ASC, name ASC"""
    )
    fun search(q: String): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE rimDiameter = :rim ORDER BY width ASC, profile ASC, brand ASC")
    fun getByRim(rim: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE rimDiameter = :rim AND subCategory = :sizeSpec ORDER BY brand ASC, name ASC")
    fun getByRimAndSubCategory(rim: Int, sizeSpec: String): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE rimDiameter = :rim AND subCategory = :sizeSpec AND brand = :brand ORDER BY name ASC")
    fun getByRimSubCatBrand(rim: Int, sizeSpec: String, brand: String): Flow<List<Item>>

    @Query("SELECT DISTINCT rimDiameter FROM items ORDER BY rimDiameter ASC")
    fun getDistinctRimDiameters(): Flow<List<Int>>

    @Query("SELECT DISTINCT subCategory FROM items WHERE rimDiameter = :rim AND subCategory != '' ORDER BY subCategory ASC")
    fun getDistinctSubCategories(rim: Int): Flow<List<String>>

    @Query("SELECT DISTINCT brand FROM items WHERE brand != '' ORDER BY brand ASC")
    fun getDistinctBrands(): Flow<List<String>>

    @Query("SELECT DISTINCT brand FROM items WHERE rimDiameter = :rim AND brand != '' ORDER BY brand ASC")
    fun getDistinctBrandsForRim(rim: Int): Flow<List<String>>

    @Query("SELECT DISTINCT brand FROM items WHERE rimDiameter = :rim AND subCategory = :sizeSpec AND brand != '' ORDER BY brand ASC")
    fun getDistinctBrandsForSubCategory(rim: Int, sizeSpec: String): Flow<List<String>>

    @Query("SELECT * FROM items WHERE sku != '' AND sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): Item?

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("SELECT * FROM items WHERE minQuantity > 0 AND quantity <= minQuantity ORDER BY rimDiameter ASC, width ASC")
    fun getLowStockItems(): Flow<List<Item>>
}
