package com.galacticai.autonet.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.galacticai.autonet.R
import com.galacticai.autonet.receivers.UsbReceiver


class UsbService : Service() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "service_channel"
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
    }

    private lateinit var receiver: UsbReceiver

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.service),
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    lateinit var notification: Notification

    private fun showNotification() {
        with(NotificationManagerCompat.from(this)) {
            val permission =
                ActivityCompat.checkSelfPermission(applicationContext, POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) return

            notification = NotificationCompat.Builder(this@UsbService, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_usb_24)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_status_label_enabled))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0L))
                .setSound(null)
                .setOngoing(true)
                .build()

            notify(R.id.persistent_notification, notification)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, getString(R.string.starting_service), Toast.LENGTH_SHORT).show()
        receiver = UsbReceiver()

        val usbFilter = IntentFilter().apply {
            addAction("android.hardware.usb.action.USB_STATE")
        }
        registerReceiver(receiver, usbFilter)

        val manager = NotificationManagerCompat.from(this)
        val channel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (channel == null) createNotificationChannel()
        showNotification()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        val manager = NotificationManagerCompat.from(this)
        if (::notification.isInitialized) {
            for (notification in manager.activeNotifications) {
                if (notification.id != R.id.persistent_notification) continue
                manager.cancel(R.id.persistent_notification)
                break
            }
        }

        if (::receiver.isInitialized) unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


