package com.yassine.inventory

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.journeyapps.barcodescanner.ScanOptions
import com.yassine.inventory.data.Item
import com.yassine.inventory.ui.InventoryScreen
import com.yassine.inventory.ui.theme.InventoryTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locales = AppLocale.locales(newBase)
        val context = if (locales.isNotEmpty()) {
            newBase.createConfigurationContext(
                Configuration(newBase.resources.configuration).apply {
                    setLocales(android.os.LocaleList(*locales.toTypedArray()))
                }
            )
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        scheduleLowStockChecks()
        setContent {
            InventoryTheme {
                InventoryRoot()
            }
        }
    }

    private fun scheduleLowStockChecks() {
        val workManager = WorkManager.getInstance(this)

        val oneTime = OneTimeWorkRequestBuilder<LowStockWorker>().build()
        workManager.enqueueUniqueWork("low_stock_once", ExistingWorkPolicy.REPLACE, oneTime)

        val periodic = PeriodicWorkRequestBuilder<LowStockWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            "low_stock_daily",
            ExistingPeriodicWorkPolicy.KEEP,
            periodic,
        )
    }
}

@Composable
private fun InventoryRoot() {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModel.factory(context.applicationContext)
    )

    var scanPending by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val content = result.data?.getStringExtra("SCAN_RESULT")
            if (!content.isNullOrBlank()) {
                viewModel.handleBarcode(content)
            } else {
                Toast.makeText(context, "No code read", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (scanPending && grants[Manifest.permission.CAMERA] == true) {
            scanPending = false
            launchScanner(context, scanLauncher)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCsv(uri, context) { added, updated ->
                Toast.makeText(
                    context,
                    "Imported: $added new, $updated updated",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val ok = DbBackup.backup(context, uri)
            Toast.makeText(
                context,
                if (ok) "Backup saved" else "Backup failed",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val ok = DbBackup.restore(context, uri)
            Toast.makeText(
                context,
                if (ok) "Restore complete — reloading" else "Restore failed",
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) activity?.recreate()
        }
    }

    InventoryScreen(
        viewModel = viewModel,
        onExport = {
            if (viewModel.items.value.isEmpty()) {
                Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
            } else {
                shareCsv(context, viewModel.items.value)
            }
        },
        onImport = {
            importLauncher.launch(
                arrayOf("text/*", "text/csv", "application/csv", "application/vnd.ms-excel", "*/*")
            )
        },
        onBackup = { backupLauncher.launch("inventory_backup.db") },
        onRestore = { restoreLauncher.launch(arrayOf("*/*")) },
        onScan = {
            val cameraGranted = context.checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (cameraGranted) {
                launchScanner(context, scanLauncher)
            } else {
                scanPending = true
                requestPermissions.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS)
                )
            }
        },
    )
}

private fun launchScanner(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
) {
    val options = ScanOptions()
        .setBeepEnabled(false)
        .setOrientationLocked(true)
    launcher.launch(options.createScanIntent(context))
}

private fun shareCsv(context: Context, items: List<Item>) {
    val uri = CsvExporter.export(items, context)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export inventory"))
}