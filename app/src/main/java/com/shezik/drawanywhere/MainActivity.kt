package com.shezik.drawanywhere

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

class MainActivity : Activity() {
    override fun onResume() {
        super.onResume()

        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this@MainActivity, MainService::class.java)
            startForegroundService(intent)
            finish()
        } else {
            showOverlayPermissionDialog()
        }
    }

    private fun showOverlayPermissionDialog() =
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.overlay_permission_description))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.permission_accept)) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.permission_deny)) { _, _ ->
                finishAffinity()
            }
            .show()
}
