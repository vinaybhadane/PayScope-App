package com.payscope.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RenewalNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "payscope_renewals"
        const val CHANNEL_NAME = "PayScope Renewal & Price Alerts"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies before recurring subscription renewals and when stealth price increases are detected."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showRenewalNotification(
        merchantName: String,
        amount: Double,
        daysRemaining: Int,
        notificationId: Int = 1001
    ) {
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Upcoming Renewal: $merchantName")
            .setContentText("₹${amount.toInt()} is estimated to renew in $daysRemaining days.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Handled when notification permission is pending
        }
    }

    fun showPriceHikeNotification(
        merchantName: String,
        oldAmount: Double,
        newAmount: Double,
        notificationId: Int = 1002
    ) {
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Price Increase Alert: $merchantName")
            .setContentText("Rate increased from ₹${oldAmount.toInt()} to ₹${newAmount.toInt()} (+${(((newAmount - oldAmount)/oldAmount)*100).toInt()}%).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Handled
        }
    }
}
