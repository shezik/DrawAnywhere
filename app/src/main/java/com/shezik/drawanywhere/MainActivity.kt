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
