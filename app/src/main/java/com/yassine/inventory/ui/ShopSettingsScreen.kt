package com.yassine.inventory.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yassine.inventory.R
import com.yassine.inventory.ShopSettings
import com.yassine.inventory.ShopSettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val current = remember { ShopSettingsStore.read(context) }

    var shopName by remember { mutableStateOf(current.shopName) }
    var address by remember { mutableStateOf(current.shopAddress) }
    var phone by remember { mutableStateOf(current.shopPhone) }
    var rc by remember { mutableStateOf(current.shopRC) }
    var ice by remember { mutableStateOf(current.shopICE) }
    var vat by remember { mutableStateOf(current.vatPercent.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text(stringResource(R.string.settings_shop_name)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.settings_address)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.settings_phone)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = rc,
                onValueChange = { rc = it },
                label = { Text(stringResource(R.string.settings_rc)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ice,
                onValueChange = { ice = it },
                label = { Text(stringResource(R.string.settings_ice)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vat,
                onValueChange = { input ->
                    vat = input.filter { it.isDigit() || it == '.' }.take(5)
                },
                label = { Text(stringResource(R.string.settings_vat)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    ShopSettingsStore.write(
                        context,
                        ShopSettings(
                            shopName = shopName.trim().ifBlank { context.getString(R.string.app_name) },
                            shopAddress = address.trim(),
                            shopPhone = phone.trim(),
                            shopRC = rc.trim(),
                            shopICE = ice.trim(),
                            vatPercent = (vat.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                        ),
                    )
                    Toast.makeText(context, context.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                    onClose()
                },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.padding(5.dp))
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        }
    }
}