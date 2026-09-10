package com.yassine.inventory

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yassine.inventory.data.AppDatabase
import com.yassine.inventory.data.Item
import kotlinx.coroutines.flow.first

class LowStockWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val lowItems = AppDatabase.get(applicationContext)
            .itemDao()
            .getAll()
            .first()
            .filter { it.isLowStock }

        if (lowItems.isNotEmpty()) {
            notifyLowStock(applicationContext, lowItems)
        }
        return Result.success()
    }

    private fun notifyLowStock(context: Context, items: List<Item>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        manager.createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val first = items.first()
        val summary = if (items.size == 1) {
            context.resources.getQuantityString(
                R.plurals.notif_summary,
                1,
                first.saleTitle,
                first.quantity,
                first.minQuantity,
            )
        } else {
            val count = items.size
            context.resources.getQuantityString(R.plurals.notif_summary, count, count)
        }

        val details = items.joinToString("\n") {
            "${it.saleTitle} — ${it.quantity} (min ${it.minQuantity})"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(1, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "low_stock"
    }
}