package es.uc3m.android.chillmates.chores

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import es.uc3m.android.chillmates.R

/**
 * Receiver that catches broadcasts to trigger chore reminder notifications.
 * Works with ChoresPage.kt via Intent broadcasts.
 */
class ChoreReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Visual feedback that the broadcast was received
        Toast.makeText(
            context,
            context.getString(R.string.toast_generating_notification),
            Toast.LENGTH_SHORT
        ).show()

        val choreTitle = intent.getStringExtra("choreTitle")
            ?: context.getString(R.string.default_chore_title)
        val assignee = intent.getStringExtra("assignee")
            ?: context.getString(R.string.default_assignee)

        val channelId = "choresReminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_title_reminder))
            .setContentText(context.getString(R.string.notif_text_format, assignee, choreTitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // Trigger notification with a unique ID based on current time
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}