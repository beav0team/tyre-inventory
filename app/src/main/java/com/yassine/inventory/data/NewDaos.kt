package com.yassine.inventory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity): Long

    @Query("SELECT * FROM invoice_lines WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun getLines(invoiceId: Long): Flow<List<InvoiceLineEntity>>

    @Query("SELECT * FROM invoice_lines WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun getLinesOnce(invoiceId: Long): List<InvoiceLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<InvoiceLineEntity>)

    @Query(
        "SELECT COALESCE(SUM(total), 0.0) FROM invoices WHERE createdAt >= :start AND createdAt < :end"
    )
    fun sumBetween(start: Long, end: Long): Flow<Double>

    @Query(
        """SELECT COALESCE(SUM(l.quantity * (l.unitPrice - l.unitCost)), 0.0)
        FROM invoice_lines l INNER JOIN invoices i ON i.id = l.invoiceId
        WHERE i.createdAt >= :start AND i.createdAt < :end"""
    )
    fun profitBetween(start: Long, end: Long): Flow<Double>

    @Query("UPDATE invoices SET paidAmount = :paid, status = :status WHERE id = :id")
    suspend fun updatePayment(id: Long, paid: Double, status: String)
}

@Dao
interface ClientDao {

    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE name = :name AND phone = :phone LIMIT 1")
    suspend fun getByNameAndPhone(name: String, phone: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(client: ClientEntity): Long
}

@Dao
interface MovementDao {

    @Query("SELECT * FROM movements ORDER BY createdAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun getAllForItem(itemId: Long): Flow<List<MovementEntity>>

    @Insert
    suspend fun insert(movement: MovementEntity): Long
}