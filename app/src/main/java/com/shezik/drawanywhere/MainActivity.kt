package com.shezik.drawanywhere

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(Modifier.padding(24.dp)) {
                Button(onClick = {
                    val intent = Intent(this@MainActivity, MainService::class.java)
                    startForegroundService(intent)

                }) {
                    Text("Start Overlay")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    stopService(Intent(this@MainActivity, MainService::class.java))
                }) {
                    Text("Stop Overlay")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!Settings.canDrawOverlays(this))
            showOverlayPermissionDialog()
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
