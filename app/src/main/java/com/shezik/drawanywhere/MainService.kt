package com.shezik.drawanywhere

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "default_channel"
    }

    private val customLifecycleOwner = CustomLifecycleOwner()
    private lateinit var windowManager: WindowManager
    private lateinit var canvasView: View
    private lateinit var toolbarView: View
    private val drawController = DrawController()
    private val viewModel = DrawViewModel(drawController)

    override fun onCreate() {
        super.onCreate()
        customLifecycleOwner.onCreate()
        customLifecycleOwner.onResume()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // -------- Setup canvas --------
        canvasView = ComposeView(this).apply {
            setContent {
                DrawCanvas(
                    modifier = Modifier.fillMaxSize()
                        .stylusAwareDrawing(viewModel = viewModel),
                    controller = drawController,
                    backgroundColor = Color.Transparent
                )
            }
        }
        customLifecycleOwner.attachToDecorView(canvasView)

        val canvasParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        // ------------------------------

        // -------- Setup toolbar --------
        toolbarView = ComposeView(this).apply {
            setContent {
                DrawToolbar(viewModel = viewModel)
            }
        }
        customLifecycleOwner.attachToDecorView(toolbarView)

        val toolbarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or  // TODO: ?
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,   // TODO: ?
            PixelFormat.TRANSLUCENT
        )
        toolbarParams.gravity = Gravity.TOP or
                Gravity.START
        // -------------------------------

        windowManager.addView(canvasView, canvasParams)
        windowManager.addView(toolbarView, toolbarParams)

        CoroutineScope(Dispatchers.Main).launch {
            viewModel.uiState.collect { state ->
                // Canvas visibility and touch passthrough
                canvasParams.flags = if (state.canvasPassthrough)
                    canvasParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                else
                    canvasParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                windowManager.updateViewLayout(canvasView, canvasParams)

                canvasView.visibility = if (state.canvasVisible)
                    View.VISIBLE else View.GONE

                // Toolbar position
                toolbarParams.x = state.toolbarPosition.x.toInt()
                toolbarParams.y = state.toolbarPosition.y.toInt()
                windowManager.updateViewLayout(toolbarView, toolbarParams)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        customLifecycleOwner.onPause()
        customLifecycleOwner.onDestroy()
        windowManager.removeView(canvasView)
        windowManager.removeView(toolbarView)
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