package com.yassine.inventory.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentStatus {
    CASH,
    PARTIAL,
    CREDIT,
}

@Entity(tableName = "invoices", indices = [Index(value = ["number"], unique = true)])
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val clientName: String = "",
    val clientPhone: String = "",
    val subtotal: Double = 0.0,
    val discountPercent: Int = 0,
    val vatPercent: Double = 0.0,
    val total: Double = 0.0,
    val paidAmount: Double = 0.0,
    val status: PaymentStatus = PaymentStatus.CASH,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val dueAmount: Double get() = total - paidAmount
}

@Entity(tableName = "invoice_lines")
data class InvoiceLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long = 0,
    val itemId: Long = 0,
    val description: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val unitCost: Double = 0.0,
    val total: Double = 0.0,
)

@Entity(tableName = "clients", indices = [Index(value = ["name", "phone"], unique = true)])
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

enum class MovementType {
    SALE,
    RESTOCK,
    ADJUST,
    DELETE,
}

@Entity(tableName = "movements", indices = [Index(value = ["itemId"])])
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long = 0,
    val itemName: String = "",
    val delta: Int = 0,
    val type: MovementType = MovementType.ADJUST,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)