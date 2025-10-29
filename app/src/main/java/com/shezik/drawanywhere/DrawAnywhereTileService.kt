package com.shezik.drawanywhere

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.provider.Settings
import android.app.PendingIntent

class DrawAnywhereTileService: TileService(){

    override fun onClick() {
        super.onClick()
        val serviceIntent = Intent(this, MainService::class.java)

        if (isServiceRunning) {
            stopService(serviceIntent)
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            if (!Settings.canDrawOverlays(this)) {
                val permissionIntent = Intent(this, MainActivity::class.java)
                permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = PendingIntent.getActivity(this, 0, permissionIntent, PendingIntent.FLAG_IMMUTABLE)
                    pendingIntent?.let { startActivityAndCollapse(it) }
                } else {
                    @SuppressLint("StartActivityAndCollapseDeprecated")
                    startActivityAndCollapse(permissionIntent)
                }

                return
            }
            startForegroundService(serviceIntent)
            qsTile.state = Tile.STATE_ACTIVE
        }

        qsTile.updateTile()
    }

    private val isServiceRunning: Boolean
        get() = MainService::class.java.name in (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager)
            .getRunningServices(Int.MAX_VALUE)
            .map { it.service.className }

    override fun onStartListening() {
        super.onStartListening()
        qsTile.state = if (isServiceRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

}