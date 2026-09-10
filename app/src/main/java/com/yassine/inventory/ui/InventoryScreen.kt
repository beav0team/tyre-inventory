package com.yassine.inventory.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yassine.inventory.AppLocale
import com.yassine.inventory.InventoryViewModel
import com.yassine.inventory.R
import com.yassine.inventory.InvoiceData
import com.yassine.inventory.InvoiceLine
import com.yassine.inventory.InvoiceNumber
import com.yassine.inventory.InvoicePdf
import com.yassine.inventory.ShopSettingsStore
import com.yassine.inventory.data.Item
import com.yassine.inventory.data.PaymentStatus
import com.yassine.inventory.SortOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val OLD_STOCK_DAYS = 120L

private enum class AppScreen { HOME, SALES, MOVEMENTS, SETTINGS, DESIGN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onScan: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---- Routing ----
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }

    // ---- UI state ----
    var activeSection by rememberSaveable { mutableStateOf(Section.STOCK) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var isAdding by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<Item?>(null) }
    var deleteTarget by rememberSaveable { mutableStateOf<Item?>(null) }

    // ---- Invoice state (saved across config changes & process death) ----
    var clientName by rememberSaveable { mutableStateOf("") }
    var phoneNum by rememberSaveable { mutableStateOf("") }
    var discountPct by rememberSaveable { mutableStateOf("") }
    var paymentStatus by rememberSaveable { mutableStateOf(PaymentStatus.CASH) }
    var paidAmountStr by rememberSaveable { mutableStateOf("") }
    val invoiceLines = rememberSaveable(saver = invoiceLineListSaver) {
        mutableStateListOf<InvoiceLine>()
    }

    val items by viewModel.items.collectAsState()

    val vatPercent = remember { ShopSettingsStore.read(context).vatPercent }

    val onGenerateInvoice: () -> Unit = {
        if (invoiceLines.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.invoice_empty), Toast.LENGTH_SHORT).show()
        } else {
            val discountValue = (discountPct.toDoubleOrNull() ?: 0.0).toInt().coerceIn(0, 100)
            val number = InvoiceNumber.next(context)
            val data = InvoiceData(
                number = number,
                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                client = clientName.trim(),
                phone = phoneNum.trim(),
                lines = invoiceLines.toList(),
                discountPercent = discountValue,
                vatPercent = vatPercent,
                paymentStatus = paymentStatus,
                paidAmount = paidAmountStr.toDoubleOrNull() ?: 0.0,
            )
            val linesSnapshot = invoiceLines.toList()
            viewModel.cacheClient(clientName, phoneNum)
            if (InvoicePdf.createAndShare(context, data)) {
                viewModel.completeSale(
                    lines = linesSnapshot,
                    number = number,
                    context = context,
                    vatPercent = vatPercent,
                    discountPercent = discountValue,
                    paymentStatus = paymentStatus,
                    paidAmount = paidAmountStr.toDoubleOrNull() ?: 0.0,
                    onDone = {
                        invoiceLines.clear()
                        clientName = ""
                        phoneNum = ""
                        discountPct = ""
                        paidAmountStr = ""
                        paymentStatus = PaymentStatus.CASH
                        Toast.makeText(context, context.getString(R.string.invoice_generated), Toast.LENGTH_LONG).show()
                    },
                )
            }
        }
    }

    if (screen != AppScreen.HOME) {
        when (screen) {
            AppScreen.SALES -> SalesHistoryScreen(viewModel = viewModel, onClose = { screen = AppScreen.HOME })
            AppScreen.MOVEMENTS -> MovementsScreen(viewModel = viewModel, onClose = { screen = AppScreen.HOME })
            AppScreen.SETTINGS -> ShopSettingsScreen(onClose = { screen = AppScreen.HOME })
            AppScreen.DESIGN -> InvoiceDesignScreen(onClose = { screen = AppScreen.HOME })
            AppScreen.HOME -> Unit
        }
        return
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { section ->
                    val label = when (section) {
                        Section.STOCK -> stringResource(R.string.nav_stock)
                        Section.INVOICE -> stringResource(R.string.nav_invoice)
                        Section.TOOLS -> stringResource(R.string.nav_tools)
                    }
                    val icon = when (section) {
                        Section.STOCK -> Icons.Filled.Inventory2
                        Section.INVOICE -> Icons.Filled.ReceiptLong
                        Section.TOOLS -> Icons.Filled.Settings
                    }
                    NavigationBarItem(
                        selected = activeSection == section,
                        onClick = { activeSection = section },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (activeSection == Section.STOCK) {
                        IconButton(onClick = { searchOpen = !searchOpen }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search_hint),
                            )
                        }
                        if (!searchOpen) {
                            IconButton(onClick = { isAdding = true }) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.add_tyre),
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            if (activeSection == Section.STOCK && searchOpen) {
                ExtendedFloatingActionButton(
                    onClick = { isAdding = true },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.add_tyre))
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize()) {
                // Search overlay
                if (activeSection == Section.STOCK && searchOpen) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                Box(Modifier.weight(1f)) {
                    when (activeSection) {
                        Section.STOCK -> StockTab(
                            viewModel = viewModel,
                            query = query,
                            onAdd = { isAdding = true },
                            onEdit = { editing = it },
                            onDelete = { deleteTarget = it },
                        )

                        Section.INVOICE -> InvoicePane(
                            viewModel = viewModel,
                            lines = invoiceLines,
                            client = clientName,
                            onClientChange = { clientName = it },
                            phone = phoneNum,
                            onPhoneChange = { phoneNum = it },
                            discount = discountPct,
                            onDiscountChange = { discountPct = it },
                            paymentStatus = paymentStatus,
                            onPaymentStatusChange = { paymentStatus = it },
                            paidAmount = paidAmountStr,
                            onPaidAmountChange = { paidAmountStr = it },
                            vatPercent = vatPercent,
                            onGenerate = onGenerateInvoice,
                        )

                        Section.TOOLS -> ToolsPane(
                            onExport = onExport,
                            onImport = onImport,
                            onBackup = onBackup,
                            onRestore = onRestore,
                            onScan = onScan,
                            onSales = { screen = AppScreen.SALES },
                            onMovements = { screen = AppScreen.MOVEMENTS },
                            onSettings = { screen = AppScreen.SETTINGS },
                            onDesign = { screen = AppScreen.DESIGN },
                            onSelectLanguage = { tag ->
                                AppLocale.set(context.applicationContext, tag)
                                (context as? Activity)?.recreate()
                            },
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (isAdding) {
        ItemEditorDialog(
            initial = null,
            onDismiss = { isAdding = false },
            onSave = { item ->
                scope.launch {
                    viewModel.upsert(item)
                    Toast.makeText(context, context.getString(R.string.tyre_added), Toast.LENGTH_SHORT).show()
                    isAdding = false
                }
            },
        )
    }
    editing?.let { item ->
        ItemEditorDialog(
            initial = item,
            onDismiss = { editing = null },
            onSave = { edited ->
                scope.launch {
                    viewModel.upsert(edited)
                    Toast.makeText(context, context.getString(R.string.tyre_updated), Toast.LENGTH_SHORT).show()
                    editing = null
                }
            },
        )
    }
    deleteTarget?.let { item ->
        userDeleteConfirm(
            item = item,
            onConfirm = {
                scope.launch {
                    viewModel.delete(item)
                    Toast.makeText(context, context.getString(R.string.tyre_deleted), Toast.LENGTH_SHORT).show()
                    deleteTarget = null
                }
            },
            onCancel = {
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun userDeleteConfirm(
    item: Item,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.delete_tyre_title)) },
        text = {
            Text(
                stringResource(
                    R.string.delete_tyre_body,
                    item.saleTitle,
                    item.sizeSpec,
                    item.brand,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.delete_tyre),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Stock tab
// ---------------------------------------------------------------------------

@Composable
private fun StockTab(
    viewModel: InventoryViewModel,
    query: String,
    onAdd: () -> Unit,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit,
) {
    val items by viewModel.items.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val rimList by viewModel.categories.collectAsState()
    val specList by viewModel.subCategories.collectAsState()
    val brandList by viewModel.brands.collectAsState()
    val activeRim by viewModel.rim.collectAsState()
    val activeSpec by viewModel.sizeSpec.collectAsState()
    val activeBrand by viewModel.brand.collectAsState()
    val sort by viewModel.sortOption.collectAsState()

    val chips: List<Pair<String, Boolean>> = when {
        activeRim <= 0 -> rimList.map { it to false }
        activeSpec == null -> specList.map { it to false }
        else -> brandList.map { it to (activeBrand == it) }
    }
    val onPickChip: (String) -> Unit = when {
        activeRim <= 0 -> { t -> viewModel.selectRim(t.removeSuffix("\"").toIntOrNull() ?: 0) }
        activeSpec == null -> { t -> viewModel.selectSubCategory(t) }
        else -> { t -> viewModel.selectBrand(t) }
    }
    val clearFilter: (() -> Unit)? = when {
        activeRim > 0 && activeSpec == null -> { { viewModel.selectRim(0) } }
        activeSpec != null && activeBrand == null -> { { viewModel.selectSubCategory(null) } }
        else -> null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 104.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeaderHero(stats = stats)
        }

        item {
            var menuOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.sort_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Box {
                    TextButton(onClick = { menuOpen = true }) {
                        Text(sortLabel(sort), fontWeight = FontWeight.Bold)
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sortLabel(option)) },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }

        if (chips.isNotEmpty() || clearFilter != null) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 16.dp),
                ) {
                    if (clearFilter != null) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = clearFilter,
                                label = {
                                    Text(stringResource(R.string.clear_search))
                                },
                            )
                        }
                    }
                    items(chips) { (text, selected) ->
                        FilterChip(
                            selected = selected,
                            onClick = { onPickChip(text) },
                            label = { Text(text) },
                        )
                    }
                }
            }
        }

        if (items.isEmpty() && query.isBlank()) {
            item {
                EmptyState(
                    title = stringResource(R.string.empty_no_tyres_title),
                    body = stringResource(R.string.empty_no_tyres_body),
                )
            }
        } else if (items.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.empty_no_results_title),
                    body = stringResource(R.string.empty_no_results_body, query),
                )
            }
        }

        items(items, key = { it.id }) { item ->
            ItemRow(
                item = item,
                onClick = { onEdit(item) },
                onIncrease = { viewModel.adjustQuantity(item, 1) },
                onDecrease = { viewModel.adjustQuantity(item, -1) },
                onDelete = { onDelete(item) },
            )
        }
    }
}

@Composable
private fun HeaderHero(stats: com.yassine.inventory.InventoryStats) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    )
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HeroStat(
                        label = stringResource(R.string.in_stock_label),
                        value = stats.unitCount.toString(),
                    )
                    HeroStat(
                        label = stringResource(R.string.stat_types),
                        value = stats.itemCount.toString(),
                    )
                    HeroStat(
                        label = stringResource(R.string.stat_value),
                        value = money(stats.stockValue),
                    )
                    HeroStat(
                        label = stringResource(R.string.stat_low),
                        value = stats.lowCount.toString(),
                    )
                }
                Text(
                    text = pluralStringResource(R.plurals.tyres_ready, stats.unitCount, stats.unitCount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ItemRow(
    item: Item,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit,
) {
    val ageDays = remember(item.createdAt) {
        ((System.currentTimeMillis() - item.createdAt) / 86_400_000L).toInt().coerceAtLeast(0)
    }
    val oldStock = ageDays > OLD_STOCK_DAYS

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.saleTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (oldStock) {
                    Text(
                        text = stringResource(R.string.old_stock),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_tyre),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (item.saleTitle != item.saleSubtitle) {
                Text(
                    text = item.saleSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (oldStock) {
                Text(
                    text = stringResource(R.string.old_stock_prefix) + " " +
                        pluralStringResource(R.plurals.stock_days, ageDays, ageDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QtyStepper(
                    quantity = item.quantity,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease,
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = money(item.price * item.quantity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = item.saleMeta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.hasMargin) {
                        val margin = item.price - item.costPrice
                        Text(
                            text = "+${money(margin)}/pneu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (item.isLowStock && item.supplier.isNotBlank()) {
                        Text(
                            text = item.supplier,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QtyStepper(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(R.string.remove_one),
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "$quantity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.add_one),
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun sortLabel(sort: SortOption): String =
    when (sort) {
        SortOption.SIZE -> stringResource(R.string.sort_size)
        SortOption.BRAND -> stringResource(R.string.sort_brand)
        SortOption.PRICE -> stringResource(R.string.sort_price)
        SortOption.STOCK -> stringResource(R.string.sort_quantity)
    }

// ---------------------------------------------------------------------------
// Tools tab
// ---------------------------------------------------------------------------

@Composable
private fun ToolsPane(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onScan: () -> Unit,
    onSales: () -> Unit,
    onMovements: () -> Unit,
    onSettings: () -> Unit,
    onDesign: () -> Unit,
    onSelectLanguage: (String) -> Unit,
) {
    val toolsContext = LocalContext.current
    val currentTag = remember { AppLocale.currentTag(toolsContext) }
    val langTags = listOf("en", "fr", "ar")
    val langNames = listOf(
        stringResource(R.string.lang_english),
        stringResource(R.string.lang_french),
        stringResource(R.string.lang_arabic),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.nav_tools),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        // Business tools
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.tools_header_business),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                )
                ToolListItem(
                    title = stringResource(R.string.tools_sales),
                    subtitle = stringResource(R.string.tools_sales_sub),
                    icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                    onClick = onSales,
                )
                ToolListItem(
                    title = stringResource(R.string.tools_movements),
                    subtitle = stringResource(R.string.tools_movements_sub),
                    icon = { Icon(Icons.Filled.SwapVert, contentDescription = null) },
                    onClick = onMovements,
                )
                ToolListItem(
                    title = stringResource(R.string.tools_settings),
                    subtitle = stringResource(R.string.tools_settings_sub),
                    icon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
                    onClick = onSettings,
                )
                ToolListItem(
                    title = stringResource(R.string.tools_invoice_design),
                    subtitle = stringResource(R.string.tools_invoice_design_sub),
                    icon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    onClick = onDesign,
                )
            }
        }

        // Language
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.tools_header_language),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 10.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    langTags.forEachIndexed { index, tag ->
                        FilterChip(
                            selected = currentTag == tag,
                            onClick = { onSelectLanguage(tag) },
                            label = { Text(langNames[index]) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Data tools
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.tools_header_data),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                )
                ToolListItem(
                    title = stringResource(R.string.tools_export),
                    subtitle = stringResource(R.string.tools_export_sub),
                    icon = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                    onClick = onExport,
                )
                ToolListItem(
                    title = stringResource(R.string.tools_import),
                    subtitle = stringResource(R.string.tools_import_sub),
                    icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                    onClick = onImport,
                )
                ToolListItem(
                    title = stringResource(R.string.tools_backup),
                    subtitle = stringResource(R.string.tools_backup_sub),
                    icon = { Icon(Icons.Filled.CloudUpload, contentDescription = null) },
                    onClick = onBackup,
                )
                ToolListItem(
                    title = stringResource(R.string.tools_restore),
                    subtitle = stringResource(R.string.tools_restore_sub),
                    icon = { Icon(Icons.Filled.Autorenew, contentDescription = null) },
                    onClick = onRestore,
                )
                ToolListItem(
                    title = stringResource(R.string.tool_scan),
                    subtitle = stringResource(R.string.tool_scan_sub),
                    icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                    onClick = onScan,
                )
            }
        }

        Text(
            text = stringResource(R.string.about_version),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        Text(
            text = stringResource(R.string.about_line),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun ToolListItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Box(Modifier.size(24.dp)) {
                icon()
            }
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    )
}

// ---------------------------------------------------------------------------
// Invoice line save/restore across process death
// ---------------------------------------------------------------------------

private val invoiceLineListSaver = listSaver<SnapshotStateList<InvoiceLine>, List<Any>>(
    save = { lines ->
        lines.map { line ->
            listOf<Any>(
                line.item.id,
                line.item.name,
                line.item.brand,
                line.item.model,
                line.item.season,
                line.item.sizeSpec,
                line.quantity,
                line.unitPrice,
                line.descriptionOverride ?: "",
            )
        }
    },
    restore = { saved ->
        val restored = SnapshotStateList<InvoiceLine>()
        saved.forEach { raw ->
            if (raw.size >= 8) {
                val (w, p, r) = parseSpec(raw[5] as String)
                restored.add(
                    InvoiceLine(
                        item = Item(
                            id = raw[0] as Long,
                            name = raw[1] as String,
                            brand = raw[2] as String,
                            model = raw[3] as String,
                            season = raw[4] as String,
                            width = w,
                            profile = p,
                            rimDiameter = r,
                        ),
                        quantity = raw[6] as Int,
                        unitPrice = raw[7] as Double,
                        descriptionOverride = (raw.getOrNull(8) as? String)?.takeIf { it.isNotBlank() },
                    )
                )
            }
        }
        restored
    },
)

private fun parseSpec(spec: String): Triple<Int, Int, Int> {
    val m = Regex("""(\d+)/(\d+)\s*R(\d+)""").find(spec) ?: return Triple(0, 0, 0)
    return Triple(
        m.groupValues[1].toIntOrNull() ?: 0,
        m.groupValues[2].toIntOrNull() ?: 0,
        m.groupValues[3].toIntOrNull() ?: 0,
    )
}

private enum class Section { STOCK, INVOICE, TOOLS }

private fun money(value: Double): String =
    String.format(Locale.US, "%.2f", value) + " DH"