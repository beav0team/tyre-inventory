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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yassine.inventory.InventoryViewModel
import com.yassine.inventory.R
import com.yassine.inventory.ShopSettingsStore
import com.yassine.inventory.data.InvoiceEntity
import com.yassine.inventory.data.PaymentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: InventoryViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsState()
    val summary by viewModel.salesSummary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sales_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SalesSummaryHeader(summary = summary)

            if (invoices.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            stringResource(R.string.sales_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(invoices, key = { it.id }) { invoice ->
                        InvoiceHistoryCard(
                            invoice = invoice,
                            onReprint = {
                                viewModel.reprintInvoice(
                                    invoice.id,
                                    context,
                                    ShopSettingsStore.read(context).vatPercent,
                                ) { ok ->
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            if (ok) R.string.invoice_share_title else R.string.toast_reprint_error
                                        ),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            onRecordPayment = { paid ->
                                if (paid > 0) {
                                    viewModel.updatePayment(invoice, invoice.paidAmount + paid)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_payment_updated),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesSummaryHeader(summary: com.yassine.inventory.SalesSummary) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryStat(
                        label = stringResource(R.string.sales_today_revenue),
                        value = money(summary.todayRevenue),
                    )
                    SummaryStat(
                        label = stringResource(R.string.sales_today_count),
                        value = summary.todayCount.toString(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryStat(
                        label = stringResource(R.string.sales_month_revenue),
                        value = money(summary.monthRevenue),
                    )
                    SummaryStat(
                        label = stringResource(R.string.sales_month_profit),
                        value = money(summary.monthProfit),
                        emphasize = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, emphasize: Boolean = false) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (emphasize) Color(0xFFFBE3A8) else Color.White,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun InvoiceHistoryCard(
    invoice: InvoiceEntity,
    onReprint: () -> Unit,
    onRecordPayment: (Double) -> Unit,
) {
    var expanded by remember(invoice.id) { mutableStateOf(false) }
    var paidInput by remember(invoice.id) { mutableStateOf("") }
    val dateText = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(invoice.createdAt))
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        invoice.number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        invoice.clientName.ifBlank { dateText },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (invoice.clientName.isNotBlank()) {
                        Text(
                            dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        money(invoice.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    StatusBadge(status = invoice.status, paid = invoice.paidAmount, due = invoice.dueAmount)
                }
            }

            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${stringResource(R.string.invoice_subtotal)}: ${money(invoice.subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (invoice.discountPercent > 0) {
                        Text(
                            "${stringResource(R.string.invoice_discount)} ${invoice.discountPercent}%: -${money(invoice.subtotal * invoice.discountPercent / 100.0)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (invoice.vatPercent > 0) {
                        Text(
                            "${stringResource(R.string.invoice_vat)} (${invoice.vatPercent}%): ${money(invoice.subtotal * (1.0 - invoice.discountPercent / 100.0) * invoice.vatPercent / 100.0)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        "${stringResource(R.string.invoice_paid)}: ${money(invoice.paidAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (invoice.dueAmount > 0) {
                        Text(
                            "${stringResource(R.string.invoice_due)}: ${money(invoice.dueAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onReprint,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null)
                        Spacer(Modifier.padding(5.dp))
                        Text(stringResource(R.string.invoice_reprint))
                    }

                    if (invoice.dueAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = paidInput,
                                onValueChange = { paidInput = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text(stringResource(R.string.invoice_paid_amount)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = MaterialTheme.shapes.extraLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    onRecordPayment(paidInput.toDoubleOrNull() ?: 0.0)
                                    paidInput = ""
                                },
                                enabled = (paidInput.toDoubleOrNull() ?: 0.0) > 0,
                                shape = MaterialTheme.shapes.extraLarge,
                            ) {
                                Text(stringResource(R.string.invoice_record_payment))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: PaymentStatus, paid: Double, due: Double) {
    val label = when (status) {
        PaymentStatus.CASH -> stringResource(R.string.status_paid)
        PaymentStatus.PARTIAL -> stringResource(R.string.status_partial)
        PaymentStatus.CREDIT -> stringResource(R.string.status_credit)
    }
    val color = when (status) {
        PaymentStatus.CASH -> MaterialTheme.colorScheme.primary
        PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        PaymentStatus.CREDIT -> MaterialTheme.colorScheme.error
    }
    Text(
        text = "$label" + if (due > 0) " · ${money(due)}" else "",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun money(value: Double): String =
    String.format(Locale.US, "%.2f", value) + " DH"