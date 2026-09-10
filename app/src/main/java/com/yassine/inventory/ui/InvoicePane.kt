package com.yassine.inventory.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yassine.inventory.InvoiceLine
import com.yassine.inventory.InventoryViewModel
import com.yassine.inventory.R
import com.yassine.inventory.data.Item
import java.util.Locale

@Composable
fun InvoicePane(
    viewModel: InventoryViewModel,
    lines: SnapshotStateList<InvoiceLine>,
    client: String,
    onClientChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    discount: String,
    onDiscountChange: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    val context = LocalContext.current
    val allStock by viewModel.allStock.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    val subtotal = lines.sumOf { it.total }
    val disc = (discount.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val total = subtotal * (1.0 - disc / 100.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = client,
            onValueChange = onClientChange,
            label = { Text(stringResource(R.string.invoice_client)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(R.string.invoice_phone)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )

        if (lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.invoice_no_items_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.invoice_no_items_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            HorizontalDivider()
            lines.forEachIndexed { index, line ->
                InvoiceLineRow(
                    line = line,
                    onQtyChange = { delta ->
                        lines[index] = line.copy(
                            quantity = (line.quantity + delta).coerceAtLeast(1)
                        )
                    },
                    onPriceChange = { newPrice ->
                        lines[index] = line.copy(unitPrice = newPrice)
                    },
                    onRemove = { lines.removeAt(index) },
                )
            }
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = discount,
                    onValueChange = {
                        onDiscountChange(it.filter(Char::isDigit).take(2))
                    },
                    label = { Text(stringResource(R.string.invoice_discount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f),
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.invoice_subtotal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(money(subtotal), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (disc > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        "-${money(subtotal * disc / 100.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.invoice_total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    money(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (allStock.isNotEmpty()) {
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.invoice_add_items))
            }
        }

        Button(
            onClick = onGenerate,
            enabled = lines.isNotEmpty(),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.LocalShipping, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.invoice_generate))
        }
        Spacer(Modifier.size(8.dp))
    }

    if (showPicker && allStock.isNotEmpty()) {
        AddItemsDialog(
            items = allStock,
            existing = lines,
            onPick = { item ->
                val existingIndex = lines.indexOfFirst { it.item.id == item.id }
                if (existingIndex >= 0) {
                    val old = lines[existingIndex]
                    lines[existingIndex] = old.copy(quantity = old.quantity + 1)
                } else {
                    lines.add(InvoiceLine(item = item, quantity = 1, unitPrice = item.price))
                }
                Toast.makeText(context, "${item.saleTitle} +1", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun InvoiceLineRow(
    line: InvoiceLine,
    onQtyChange: (Int) -> Unit,
    onPriceChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        line.description,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        line.item.saleSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.invoice_remove_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QtyStepper(
                    quantity = line.quantity,
                    onIncrease = { onQtyChange(1) },
                    onDecrease = { onQtyChange(-1) },
                )
                Spacer(Modifier.weight(1f))
                PriceField(
                    value = line.unitPrice,
                    onChange = onPriceChange,
                )
                Text(
                    money(line.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(96.dp),
                )
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
        IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(R.string.remove_one),
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "$quantity",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.add_one),
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PriceField(
    value: Double,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value.decimalsToString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            onChange(input.toDoubleOrNull() ?: 0.0)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.extraLarge,
        label = { Text(stringResource(R.string.editor_price)) },
        modifier = modifier.width(88.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemsDialog(
    items: List<Item>,
    existing: SnapshotStateList<InvoiceLine>,
    onPick: (Item) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, items) {
        if (query.isBlank()) items
        else items.filter {
            it.saleTitle.contains(query, ignoreCase = true) ||
                it.sizeSpec.contains(query, ignoreCase = true) ||
                it.sku.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.invoice_add_items)) },
        text = {
            Column {
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
                    modifier = Modifier.fillMaxWidth(),
                )
                if (filtered.isEmpty()) {
                    Text(
                        stringResource(R.string.empty_no_results_body, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(filtered) { item ->
                            val inInvoice = existing.firstOrNull {
                                it.item.id == item.id
                            }?.quantity ?: 0
                            ListItem(
                                headlineContent = {
                                    Text(
                                        item.saleTitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        item.saleSubtitle +
                                            if (inInvoice > 0) "   × $inInvoice" else "",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(item) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun Double.decimalsToString(): String =
    if (this == Math.floor(this) && !isInfinite()) {
        toLong().toString()
    } else {
        toString()
    }

private fun money(value: Double): String =
    String.format(Locale.US, "%.2f", value) + " DH"