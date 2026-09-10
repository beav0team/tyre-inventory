package com.yassine.inventory

import android.content.Context
import android.net.Uri
import com.yassine.inventory.data.AppDatabase
import java.io.File

object DbBackup {

    fun dbFile(context: Context): File {
        val dir = context.applicationContext.getDatabasePath("inventory.db").parentFile
        dir?.mkdirs()
        return File(dir, "inventory.db")
    }

    fun backup(context: Context, uri: Uri): Boolean {
        val db = dbFile(context)
        if (!db.exists()) return false
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                db.inputStream().use { it.copyTo(out) }
                true
            } ?: false
        }.getOrDefault(false)
    }

    fun restore(context: Context, uri: Uri): Boolean {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        val bytes = runCatching { input.use { it.readBytes() } }.getOrElse {
            input.close()
            return false
        }
        if (bytes.isEmpty()) return false

        return runCatching {
            AppDatabase.closeAndReset()

            val db = dbFile(context)
            db.parentFile?.mkdirs()
            db.delete()
            File(db.path + "-wal").delete()
            File(db.path + "-shm").delete()
            db.writeBytes(bytes)
            true
        }.getOrElse { false }
    }
}