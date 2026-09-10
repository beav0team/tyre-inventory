package com.yassine.inventory.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yassine.inventory.R
import com.yassine.inventory.data.Item

@Composable
fun ItemEditorDialog(
    initial: Item?,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit,
) {
    var brand by rememberSaveable(initial?.id) { mutableStateOf(initial?.brand ?: "") }
    var model by rememberSaveable(initial?.id) { mutableStateOf(initial?.model ?: "") }
    var rim by rememberSaveable(initial?.id) { mutableStateOf((initial?.rimDiameter ?: 0).toString()) }
    var width by rememberSaveable(initial?.id) { mutableStateOf((initial?.width ?: 0).toString()) }
    var profile by rememberSaveable(initial?.id) { mutableStateOf((initial?.profile ?: 0).toString()) }
    var loadIndex by rememberSaveable(initial?.id) { mutableStateOf(initial?.loadIndex ?: "") }
    var speedIndex by rememberSaveable(initial?.id) { mutableStateOf(initial?.speedIndex ?: "") }
    var season by rememberSaveable(initial?.id) { mutableStateOf(initial?.season ?: "") }
    var quantity by rememberSaveable(initial?.id) { mutableStateOf((initial?.quantity ?: 0).toString()) }
    var minQuantity by rememberSaveable(initial?.id) { mutableStateOf((initial?.minQuantity ?: 0).toString()) }
    var price by rememberSaveable(initial?.id) { mutableStateOf(if (initial == null) "" else initial.price.toString()) }
    var sku by rememberSaveable(initial?.id) { mutableStateOf(initial?.sku ?: "") }
    var notes by rememberSaveable(initial?.id) { mutableStateOf(initial?.notes ?: "") }

    val seasons = stringArrayResource(R.array.seasons)
    val r = rim.toIntOrNull() ?: 0
    val w = width.toIntOrNull() ?: 0
    val p = profile.toIntOrNull() ?: 0
    val valid = brand.isNotBlank() || (r in 1..22 && w > 0 && p > 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.editor_title_add else R.string.editor_title_edit
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 0.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(stringResource(R.string.editor_brand)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.editor_model)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { input ->
                            width = input.filter(Char::isDigit).take(3)
                        },
                        label = { Text(stringResource(R.string.editor_width)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = profile,
                        onValueChange = { input ->
                            profile = input.filter(Char::isDigit).take(3)
                        },
                        label = { Text(stringResource(R.string.editor_profile)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = rim,
                    onValueChange = { input ->
                        rim = input.filter(Char::isDigit).take(2)
                    },
                    label = { Text(stringResource(R.string.editor_rim_category)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.editor_season),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    seasons.forEach { candidate ->
                        FilterChip(
                            selected = season == candidate,
                            onClick = { season = candidate },
                            label = { Text(candidate) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = loadIndex,
                        onValueChange = { loadIndex = it },
                        label = { Text(stringResource(R.string.editor_load_index)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = speedIndex,
                        onValueChange = { speedIndex = it },
                        label = { Text(stringResource(R.string.editor_speed_index)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { input ->
                            quantity = input.filter(Char::isDigit)
                        },
                        label = { Text(stringResource(R.string.editor_quantity)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minQuantity,
                        onValueChange = { input ->
                            minQuantity = input.filter(Char::isDigit)
                        },
                        label = { Text(stringResource(R.string.editor_low_at)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text(stringResource(R.string.editor_price)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text(stringResource(R.string.editor_sku)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.editor_notes)) },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val w = width.toIntOrNull() ?: 0
                    val p = profile.toIntOrNull() ?: 0
                    val r = rim.toIntOrNull() ?: 0
                    val spec = if (w > 0 && p > 0 && r > 0) "$w/$p R$r" else ""
                    onSave(
                        Item(
                            id = initial?.id ?: 0L,
                            name = spec.ifBlank { brand.ifBlank { sku } },
                            sku = sku.trim(),
                            rimDiameter = r.coerceIn(0, 22),
                            width = w,
                            profile = p,
                            brand = brand.trim(),
                            model = model.trim(),
                            season = season,
                            loadIndex = loadIndex.trim(),
                            speedIndex = speedIndex.trim(),
                            quantity = quantity.toIntOrNull() ?: 0,
                            minQuantity = minQuantity.toIntOrNull() ?: 0,
                            price = price.toDoubleOrNull() ?: 0.0,
                            notes = notes.trim(),
                            category = if (r > 0) "${r}\"" else "",
                            subCategory = spec,
                        )
                    )
                },
            ) {
                Text(
                    stringResource(R.string.save),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}