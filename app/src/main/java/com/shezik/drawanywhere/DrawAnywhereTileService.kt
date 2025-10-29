package com.shezik.drawanywhere

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.provider.Settings

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
                startActivityAndCollapse(permissionIntent)
                return
            } else {
                startForegroundService(serviceIntent)
                qsTile.state = Tile.STATE_ACTIVE
            }
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
