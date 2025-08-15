/*
DrawAnywhere: An Android application that lets you draw on top of other apps.
Copyright (C) 2025 shezik

This program is free software: you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free Software
Foundation, either version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along
with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.shezik.drawanywhere

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
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
import android.view.WindowManager.LayoutParams
import android.view.WindowInsets
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.round
import androidx.core.app.ServiceCompat

class MainService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 100
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
    private var serviceStateJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        preferencesMgr = PreferencesManager(this)
        val (initialUiState, initialServiceState) = runBlocking {
            preferencesMgr.getSavedUiState() to preferencesMgr.getSavedServiceState()
        }
        viewModel = DrawViewModel(
            controller = drawController,
            preferencesMgr = preferencesMgr,
            initialUiState = initialUiState,
            initialServiceState = initialServiceState,
            stopService = { stopSelf() }
        )

        customLifecycleOwner.onCreate()
        customLifecycleOwner.onResume()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

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

        val canvasParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                    LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        handleCanvasPassthrough(canvasParams, initialUiState)
        // ------------------------------

        // -------- Setup toolbar --------
        toolbarView = ComposeView(this).apply {
            setContent {
                DrawToolbar(viewModel = viewModel)
            }
        }
        customLifecycleOwner.attachToDecorView(toolbarView)

        val toolbarParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                    LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        toolbarParams.gravity = Gravity.TOP or
                Gravity.START

        handleToolbarPosition(toolbarParams, initialServiceState, windowManager, toolbarView, viewModel)
        // -------------------------------

        windowManager.addView(canvasView, canvasParams)
        windowManager.addView(toolbarView, toolbarParams)

        uiStateJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.uiState.collect { state ->
                handleCanvasPassthrough(canvasParams, state)
                windowManager.updateViewLayout(canvasView, canvasParams)

                canvasView.visibility = if (state.canvasVisible)
                    View.VISIBLE else View.GONE
            }
        }

        serviceStateJob = CoroutineScope(Dispatchers.Main).launch {
            viewModel.serviceState.collect { state ->
                handleToolbarPosition(toolbarParams, state, windowManager, toolbarView, viewModel)
                windowManager.updateViewLayout(toolbarView, toolbarParams)

                val targetAlpha = if (state.toolbarActive) 1.0f else 0.5f
                toolbarView.animate()
                    .alpha(targetAlpha)
                    .setDuration(300)  // Animate alpha change over milliseconds
                    .start()
            }
        }
    }

    private fun handleCanvasPassthrough(
        canvasParams: LayoutParams,
        state: UiState
    ) {
        canvasParams.flags = if (state.canvasPassthrough)
            canvasParams.flags or LayoutParams.FLAG_NOT_TOUCHABLE
        else
            canvasParams.flags and LayoutParams.FLAG_NOT_TOUCHABLE.inv()
    }

    private fun handleToolbarPosition(
        toolbarParams: LayoutParams,
        state: ServiceState,
        windowManager: WindowManager,
        toolbarView: View,
        viewModel: DrawViewModel
    ) {
        val rounded = state.toolbarPosition.round()

        if (state.positionValidated) {
            toolbarParams.x = rounded.x
            toolbarParams.y = rounded.y
        } else {
            val (screenWidth, screenHeight) = getUsableScreenSize(windowManager)
            val coercedX = rounded.x.coerceIn(0, screenWidth - toolbarView.width)
            val coercedY = rounded.y.coerceIn(0, screenHeight - toolbarView.height)
            viewModel.setToolbarPosition(Offset(coercedX.toFloat(), coercedY.toFloat()), true)
        }
    }

    @Suppress("DEPRECATION")
    private fun getUsableScreenSize(windowManager: WindowManager): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.maximumWindowMetrics
            val insets = windowMetrics.windowInsets.getInsets(
                WindowInsets.Type.navigationBars()
            )
            val bounds = windowMetrics.bounds
            val usableWidth = bounds.width() - insets.left - insets.right
            val usableHeight = bounds.height() - insets.top - insets.bottom
            usableWidth to usableHeight
        } else {
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            size.x to size.y
        }

    override fun onDestroy() {
        super.onDestroy()
        uiStateJob?.cancel()
        serviceStateJob?.cancel()
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}