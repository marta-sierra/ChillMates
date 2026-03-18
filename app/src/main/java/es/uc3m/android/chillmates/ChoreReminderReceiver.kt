package es.uc3m.android.chillmates

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ChoreReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. QUICK VISUAL FEEDBACK (To know the button is actually working and broadcasting)
        Toast.makeText(context, "🔔 Generating notification...", Toast.LENGTH_SHORT).show()

        // Retrieve data sent from the broadcast intent
        val choreTitle = intent.getStringExtra("CHORE_TITLE") ?: "a chore"
        val assignee = intent.getStringExtra("ASSIGNEE") ?: "Someone"

        val channelId = "chores_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the Notification Channel (Required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chores Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications to remind flatmates about their chores"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the visual notification with a safe, standard Android icon
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Reminder: ChillMates")
            .setContentText("$assignee needs to: $choreTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority so it pops up on screen
            .setAutoCancel(true) // Dismisses the notification when tapped
            .build()

        // Trigger the notification (Using a time-based ID so multiple notifications don't overwrite each other)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}