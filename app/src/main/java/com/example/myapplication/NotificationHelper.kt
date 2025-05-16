package com.example.myapplication

import android.app.*
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    fun sendBudgetAlert(context: Context, message: String) {
        val channelId = "budget_alert_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when approaching or exceeding budget"
            }
            manager.createNotificationChannel(channel)
        }

        // ✨ Decode logo as large icon
        val largeIconBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)

        // ✨ Final Notification Builder
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo)                 // Tiny status bar icon
            .setLargeIcon(largeIconBitmap)                 // Big icon inside the notification
            .setContentTitle("Budget Alert 🚨")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)                           // Dismiss on click

        manager.notify(1001, builder.build())
    }
}
