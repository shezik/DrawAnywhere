package com.shezik.drawanywhere

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat

class MainService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "default_channel"
    }

    private val customLifecycleOwner = CustomLifecycleOwner()
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    override fun onCreate() {
        super.onCreate()
        customLifecycleOwner.onCreate()
        customLifecycleOwner.onResume()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        overlayView = ComposeView(this).apply {
            setContent {
                DrawSurface()
            }
        }
        customLifecycleOwner.attachToDecorView(overlayView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        customLifecycleOwner.onPause()
        customLifecycleOwner.onDestroy()
        windowManager.removeView(overlayView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .build()
    }
}