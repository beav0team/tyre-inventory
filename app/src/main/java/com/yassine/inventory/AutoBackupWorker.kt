package com.yassine.inventory

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = applicationContext
            val backupsDir = File(appContext.getExternalFilesDir(null), "backups")
            if (!backupsDir.exists() && !backupsDir.mkdirs()) {
                Result.failure()
            } else {
                cleanupOld(backupsDir)
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val target = File(backupsDir, "inventory_$stamp.db")
                copyDatabase(appContext, target)
                Result.success()
            }
        }.getOrElse {
            Result.retry()
        }
    }

    private fun copyDatabase(appContext: Context, target: File) {
        val dbName = "inventory.db"
        val source = appContext.getDatabasePath(dbName)
        if (!source.exists()) return

        val suffixes = listOf("", "-wal", "-shm", "-journal")
        suffixes.forEach { suffix ->
            val src = File("${source.absolutePath}$suffix")
            val dst = File("${target.absolutePath}$suffix")
            if (src.exists() && src.isFile) {
                src.copyTo(dst, overwrite = true)
            }
        }
    }

    private fun cleanupOld(backupsDir: File) {
        val cutoff = System.currentTimeMillis() - KEEP_DAYS * 24L * 60 * 60 * 1000
        backupsDir.listFiles { f -> f.name.startsWith("inventory_") && f.name.endsWith(".db") }
            ?.forEach { file ->
                if (file.name != "inventory.db" && file.lastModified() < cutoff) {
                    file.delete()
                    File("${file.absolutePath}-wal").delete()
                    File("${file.absolutePath}-shm").delete()
                }
            }
    }

    companion object {
        const val NAME = "auto_backup"
        const val TAG = "auto_backup_daily"
        private const val KEEP_DAYS = 7L
    }
}