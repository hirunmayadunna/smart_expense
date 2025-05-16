package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val builder = NotificationCompat.Builder(context, "budget_alert_channel")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Daily Reminder")
            .setContentText("Don't forget to record your expenses today! 💸")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(2002, builder.build())
        }
    }
}
