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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
    private lateinit var preferencesMgr: PreferencesManager
    private lateinit var viewModel: DrawViewModel
    private var uiStateJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        preferencesMgr = PreferencesManager(this)
        val initialUiState = runBlocking {
            preferencesMgr.getSavedUiState()
        }
        viewModel = DrawViewModel(
            controller = drawController,
            preferencesMgr = preferencesMgr,
            initialUiState = initialUiState,
            stopService = { stopSelf() }
        )

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
        toolbarParams.x = initialUiState.toolbarPosition.x.toInt()
        toolbarParams.y = initialUiState.toolbarPosition.y.toInt()
        toolbarParams.gravity = Gravity.TOP or
                Gravity.START
        // -------------------------------

        windowManager.addView(canvasView, canvasParams)
        windowManager.addView(toolbarView, toolbarParams)

        uiStateJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.uiState.collect { state ->
                // Canvas visibility and touch passthrough
                canvasParams.flags = if (state.canvasPassthrough)
                    canvasParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                else
                    canvasParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                windowManager.updateViewLayout(canvasView, canvasParams)

                canvasView.visibility = if (state.canvasVisible)
                    View.VISIBLE else View.GONE

                val targetAlpha = if (state.toolbarActive) 1.0f else 0.5f
                toolbarView.animate()
                    .alpha(targetAlpha)
                    .setDuration(300)  // Animate alpha change over 300 milliseconds
                    .start()

                // Toolbar position
                toolbarParams.x = state.toolbarPosition.x.toInt()
                toolbarParams.y = state.toolbarPosition.y.toInt()
                windowManager.updateViewLayout(toolbarView, toolbarParams)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uiStateJob?.cancel()
        if (::toolbarView.isInitialized && toolbarView.isAttachedToWindow)
            windowManager.removeView(toolbarView)
        if (::canvasView.isInitialized && canvasView.isAttachedToWindow)
            windowManager.removeView(canvasView)
        customLifecycleOwner.onPause()
        customLifecycleOwner.onDestroy()
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