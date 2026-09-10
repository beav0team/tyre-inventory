package com.yassine.inventory

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yassine.inventory.data.AppDatabase
import com.yassine.inventory.data.ClientEntity
import com.yassine.inventory.data.InvoiceEntity
import com.yassine.inventory.data.InvoiceLineEntity
import com.yassine.inventory.data.InventoryRepository
import com.yassine.inventory.data.Item
import com.yassine.inventory.data.MovementEntity
import com.yassine.inventory.data.MovementType
import com.yassine.inventory.data.PaymentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SortOption(val label: String) {
    SIZE("Size"),
    BRAND("Brand"),
    PRICE("Price"),
    STOCK("Quantity"),
}

data class InventoryStats(
    val itemCount: Int,
    val unitCount: Int,
    val stockValue: Double,
    val lowCount: Int,
) {
    companion object {
        val EMPTY = InventoryStats(0, 0, 0.0, 0)
    }
}

data class SalesSummary(
    val todayRevenue: Double,
    val todayCount: Int,
    val monthRevenue: Double,
    val monthProfit: Double,
)

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _rim = MutableStateFlow(0)
    val rim: StateFlow<Int> = _rim.asStateFlow()

    private val _sizeSpec = MutableStateFlow<String?>(null)
    val sizeSpec: StateFlow<String?> = _sizeSpec.asStateFlow()

    private val _brand = MutableStateFlow<String?>(null)
    val brand: StateFlow<String?> = _brand.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.SIZE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _scanResult = MutableStateFlow<Item?>(null)
    val scanResult: StateFlow<Item?> = _scanResult.asStateFlow()

    private val allItems: StateFlow<List<Item>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allStock: StateFlow<List<Item>> = allItems

    val categories: StateFlow<List<String>> = allItems
        .map { list -> list.map { it.rimDiameter }.filter { it > 0 }.distinct().sorted().map { "${it}\"" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subCategories: StateFlow<List<String>> = combine(allItems, _rim) { list, rim ->
        if (rim <= 0) emptyList()
        else list.filter { it.rimDiameter == rim }
            .map { it.sizeSpec }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val brands: StateFlow<List<String>> = combine(allItems, _rim, _sizeSpec) { list, rim, spec ->
        list.filter { (rim <= 0 || it.rimDiameter == rim) && (spec == null || it.sizeSpec == spec) }
            .map { it.brand }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filteredItems: StateFlow<List<Item>> = combine(
        allItems, _query, _rim, _sizeSpec, _brand,
    ) { list, q, rim, spec, brand ->
        list.filter { item ->
            val matchesRim = rim <= 0 || item.rimDiameter == rim
            val matchesSpec = spec == null || item.sizeSpec == spec
            val matchesBrand = brand == null || item.brand.equals(brand, ignoreCase = true)
            val matchesQuery = q.isBlank() ||
                item.name.contains(q, ignoreCase = true) ||
                item.brand.contains(q, ignoreCase = true) ||
                item.model.contains(q, ignoreCase = true) ||
                item.sku.contains(q, ignoreCase = true) ||
                item.sizeSpec.contains(q, ignoreCase = true)
            matchesRim && matchesSpec && matchesBrand && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val items: StateFlow<List<Item>> = combine(filteredItems, _sortOption) { list, sort ->
        list.sortedWith(
            when (sort) {
                SortOption.SIZE -> compareBy(
                    { it.rimDiameter },
                    { it.width },
                    { it.profile },
                    { it.brand.lowercase() },
                    { it.model.lowercase() },
                )
                SortOption.BRAND -> compareBy(
                    { it.brand.lowercase() },
                    { it.model.lowercase() },
                    { it.sizeSpec },
                )
                SortOption.PRICE -> compareBy<Item>({ it.price }, { it.sizeSpec }, { it.brand.lowercase() })
                SortOption.STOCK -> compareBy<Item>({ it.quantity }, { it.sizeSpec }, { it.brand.lowercase() })
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<InventoryStats> = items
        .map { list ->
            InventoryStats(
                itemCount = list.size,
                unitCount = list.sumOf { it.quantity },
                stockValue = list.sumOf { it.quantity * it.price },
                lowCount = list.count { it.isLowStock },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryStats.EMPTY)

    val invoices: StateFlow<List<InvoiceEntity>> = repository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clients: StateFlow<List<ClientEntity>> = repository.observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movements: StateFlow<List<MovementEntity>> = repository.observeMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val salesSummary: StateFlow<SalesSummary> = combine(
        repository.observeRevenue(startOfDay(), startOfDay() + DAY_MS),
        repository.observeRevenue(startOfMonth(), startOfMonth() + MONTH_MS),
        repository.observeProfit(startOfMonth(), startOfMonth() + MONTH_MS),
        invoices,
    ) { todayRev, monthRev, monthProfit, all ->
        SalesSummary(
            todayRevenue = todayRev,
            todayCount = all.count {
                it.createdAt >= startOfDay() && it.createdAt < startOfDay() + DAY_MS
            },
            monthRevenue = monthRev,
            monthProfit = monthProfit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SalesSummary(0.0, 0, 0.0, 0.0))

    fun setQuery(value: String) {
        _query.value = value
    }

    fun selectRim(rim: Int) {
        _rim.value = rim
        _sizeSpec.value = null
        _brand.value = null
    }

    fun rimSelected(rim: Int): Boolean = _rim.value == rim

    fun selectSubCategory(spec: String?) {
        _sizeSpec.value = spec
        _brand.value = null
    }

    fun subCategorySelected(spec: String?): Boolean =
        _rim.value != 0 && _sizeSpec.value == spec

    fun selectBrand(brand: String?) {
        _brand.value = brand
    }

    fun brandSelected(brand: String?): Boolean = _brand.value == brand

    fun setSortOption(value: SortOption) {
        _sortOption.value = value
    }

    fun upsert(item: Item) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val withTime = if (item.id == 0L) {
                item.copy(createdAt = now, updatedAt = now)
            } else {
                item.copy(updatedAt = now)
            }
            repository.upsert(withTime)
        }
    }

    fun adjustQuantity(item: Item, delta: Int) {
        viewModelScope.launch {
            val oldQty = item.quantity.coerceAtLeast(0)
            val newQty = (oldQty + delta).coerceAtLeast(0)
            val applied = newQty - oldQty
            repository.upsert(
                item.copy(
                    quantity = newQty,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            if (applied != 0) {
                repository.logMovement(
                    itemId = item.id,
                    itemName = item.saleTitle,
                    delta = applied,
                    type = if (applied > 0) MovementType.RESTOCK else MovementType.ADJUST,
                    note = if (applied > 0) item.supplier else "manual",
                )
            }
        }
    }

    fun restock(item: Item, add: Int) {
        viewModelScope.launch {
            repository.upsert(
                item.copy(
                    quantity = item.quantity + add,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            repository.logMovement(
                itemId = item.id,
                itemName = item.saleTitle,
                delta = add,
                type = MovementType.RESTOCK,
                note = item.supplier,
            )
        }
    }

    fun delete(item: Item) {
        viewModelScope.launch { repository.delete(item) }
    }

    fun handleBarcode(sku: String) {
        viewModelScope.launch {
            val found = repository.getBySku(sku)
            _scanResult.value = found ?: Item(name = "", sku = sku)
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    fun completeSale(
        lines: List<InvoiceLine>,
        number: String,
        context: Context,
        vatPercent: Double,
        discountPercent: Int,
        paymentStatus: PaymentStatus,
        paidAmount: Double,
        onDone: (InvoiceEntity) -> Unit,
    ) {
        viewModelScope.launch {
            val subtotal = lines.sumOf { it.total }
            val discountAmount = subtotal * discountPercent / 100.0
            val afterDiscount = subtotal - discountAmount
            val vatAmount = afterDiscount * vatPercent / 100.0
            val total = afterDiscount + vatAmount
            val settled = if (paymentStatus == PaymentStatus.CASH) total else paidAmount.coerceIn(0.0, total)
            val normalizedStatus = when {
                paymentStatus == PaymentStatus.CASH -> PaymentStatus.CASH
                settled <= 0 -> PaymentStatus.CREDIT
                settled >= total -> PaymentStatus.CASH
                else -> PaymentStatus.PARTIAL
            }

            val invoice = InvoiceEntity(
                number = number,
                clientName = _clientNameCache,
                clientPhone = _clientPhoneCache,
                subtotal = subtotal,
                discountPercent = discountPercent,
                vatPercent = vatPercent,
                total = total,
                paidAmount = settled,
                status = normalizedStatus,
            )
            val invoiceId = repository.saveInvoice(
                invoice,
                lines.map { line ->
                    InvoiceLineEntity(
                        itemId = line.item.id,
                        description = line.description,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        unitCost = line.item.costPrice,
                        total = line.total,
                    )
                }
            )
            repository.upsertClient(invoice.clientName, invoice.clientPhone)

            lines.forEach { line ->
                val current = repository.getById(line.item.id)
                if (current != null) {
                    val newQty = (current.quantity - line.quantity).coerceAtLeast(0)
                    repository.upsert(
                        current.copy(
                            quantity = newQty,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                    repository.logMovement(
                        itemId = current.id,
                        itemName = current.saleTitle,
                        delta = -line.quantity,
                        type = MovementType.SALE,
                        note = invoice.number,
                    )
                }
            }
            onDone(invoice.copy(id = invoiceId))
        }
    }

    private var _clientNameCache: String = ""
    private var _clientPhoneCache: String = ""

    fun cacheClient(name: String, phone: String) {
        _clientNameCache = name.trim()
        _clientPhoneCache = phone.trim()
    }

    fun updatePayment(invoice: InvoiceEntity, paid: Double) {
        viewModelScope.launch {
            val settled = paid.coerceIn(0.0, invoice.total)
            val status = when {
                settled <= 0 -> PaymentStatus.CREDIT
                settled >= invoice.total -> PaymentStatus.CASH
                else -> PaymentStatus.PARTIAL
            }
            repository.updateInvoicePayment(invoice.id, settled, status)
        }
    }

    fun reprintInvoice(
        invoiceId: Long,
        context: Context,
        vatPercent: Double,
        onDone: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val invoice = repository.getInvoice(invoiceId) ?: run { onDone(false); return@launch }
            val lines = repository.getInvoiceLines(invoiceId)
            val dateText = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                Date(invoice.createdAt)
            )
            val data = InvoiceData(
                number = invoice.number,
                date = dateText,
                client = invoice.clientName,
                phone = invoice.clientPhone,
                lines = lines.map { line ->
                    InvoiceLine(
                        item = Item(name = line.description),
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        descriptionOverride = line.description,
                    )
                },
                discountPercent = invoice.discountPercent,
                vatPercent = invoice.vatPercent.takeIf { it > 0 } ?: vatPercent,
                paymentStatus = invoice.status,
                paidAmount = invoice.paidAmount,
            )
            onDone(InvoicePdf.createAndShare(context, data))
        }
    }

    fun importCsv(uri: Uri, context: Context, onDone: (Int, Int) -> Unit) {
        viewModelScope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }.getOrNull()

            if (text.isNullOrBlank()) {
                onDone(0, 0)
                return@launch
            }

            val imported = CsvImporter.parse(text)
            val now = System.currentTimeMillis()
            var added = 0
            var updated = 0

            imported.forEach { item ->
                val existing = repository.getBySku(item.sku)
                if (existing != null) {
                    repository.upsert(
                        item.copy(
                            id = existing.id,
                            createdAt = existing.createdAt,
                            updatedAt = now,
                        )
                    )
                    updated++
                } else {
                    repository.upsert(item.copy(createdAt = now, updatedAt = now))
                    added++
                }
            }
            onDone(added, updated)
        }
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val MONTH_MS = 31L * DAY_MS

        private fun startOfDay(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        private fun startOfMonth(): Long = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val database = AppDatabase.get(context)
                InventoryViewModel(
                    InventoryRepository(
                        database.itemDao(),
                        database.invoiceDao(),
                        database.clientDao(),
                        database.movementDao(),
                    )
                )
            }
        }
    }
}