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

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    private object PreferencesKeys {
        val CURRENT_PEN_TYPE = stringPreferencesKey("current_pen_type")
        val TOOLBAR_POSITION_X = floatPreferencesKey("toolbar_position_x")
        val TOOLBAR_POSITION_Y = floatPreferencesKey("toolbar_position_y")
        val TOOLBAR_ORIENTATION = stringPreferencesKey("toolbar_orientation")
        val AUTO_CLEAR_CANVAS = booleanPreferencesKey("auto_clear_canvas")
        val VISIBLE_ON_START = booleanPreferencesKey("visible_on_start")

        // Pen-specific keys (for saving multiple pens)
        fun penColorKey(penType: PenType) = intPreferencesKey("${penType.name}_color")
        fun penWidthKey(penType: PenType) = floatPreferencesKey("${penType.name}_width")
        fun penAlphaKey(penType: PenType) = floatPreferencesKey("${penType.name}_alpha")
        // NEW: Keys for additional UiState fields
        val TOOLBAR_SCALE = floatPreferencesKey("toolbar_scale")
        val TOOLBAR_INACTIVE_ALPHA = floatPreferencesKey("toolbar_inactive_alpha")

        // NEW: Key for ServiceState (saved fields)
        val TOOLBAR_ACTIVE = booleanPreferencesKey("toolbar_active")
    }

    inline fun <reified T : Enum<T>> getEnumValueOrDefault(
        value: String?,
        defaultValue: T
    ): T {
        if (value == null) return defaultValue
        return try {
            enumValueOf(value)
        } catch (_: IllegalArgumentException) {
            defaultValue
        }
    }

    suspend fun getSavedUiState(): UiState {
        val preferences = context.dataStore.data.first()
        val defaultUiState = UiState()

        val currentPenType = getEnumValueOrDefault<PenType>(
            preferences[PreferencesKeys.CURRENT_PEN_TYPE],
            defaultUiState.currentPenType)

        // Reconstruct pen configurations
        val penConfigs = defaultUiState.penConfigs.toMutableMap()
        for (penType in PenType.entries) {
            val color = preferences[PreferencesKeys.penColorKey(penType)]
            val width = preferences[PreferencesKeys.penWidthKey(penType)]
            val alpha = preferences[PreferencesKeys.penAlphaKey(penType)]

            if (color != null || width != null || alpha != null) {
                penConfigs[penType] = PenConfig(
                    penType = penType,
                    // Should never reach Color.Red, unless defaultPenConfigs is not elaborate
                    color = color?.let { Color(it) } ?: penConfigs[penType]?.color ?: Color.Red,
                    // We don't have common default values for the two below
                    width = width ?: penConfigs[penType]?.width ?: 10f,
                    alpha = alpha ?: penConfigs[penType]?.alpha ?: 1f
                )
            }
        }

        val visibleOnStart = preferences[PreferencesKeys.VISIBLE_ON_START] ?: defaultUiState.visibleOnStart

        // NEW: Load additional UiState fields
        val toolbarScale = preferences[PreferencesKeys.TOOLBAR_SCALE] ?: defaultUiState.toolbarScale
        val toolbarInactiveAlpha = preferences[PreferencesKeys.TOOLBAR_INACTIVE_ALPHA] ?: defaultUiState.toolbarInactiveAlpha

        return UiState(
            currentPenType = currentPenType,
            penConfigs = penConfigs,
            toolbarOrientation = getEnumValueOrDefault<ToolbarOrientation>(
                preferences[PreferencesKeys.TOOLBAR_ORIENTATION],
                defaultUiState.toolbarOrientation),
            autoClearCanvas = preferences[PreferencesKeys.AUTO_CLEAR_CANVAS] ?: defaultUiState.autoClearCanvas,

            visibleOnStart = visibleOnStart,
            canvasVisible = visibleOnStart,
            firstDrawerOpen = visibleOnStart,
            toolbarScale = toolbarScale, //NEW: Include the loaded values
            toolbarInactiveAlpha = toolbarInactiveAlpha
        )
    }

    suspend fun saveUiState(uiState: UiState) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_PEN_TYPE] = uiState.currentPenType.name
            preferences[PreferencesKeys.TOOLBAR_ORIENTATION] = uiState.toolbarOrientation.name
            preferences[PreferencesKeys.AUTO_CLEAR_CANVAS] = uiState.autoClearCanvas
            preferences[PreferencesKeys.VISIBLE_ON_START] = uiState.visibleOnStart

            // Save each pen's configuration
            for ((penType, config) in uiState.penConfigs) {
                preferences[PreferencesKeys.penColorKey(penType)] = config.color.toArgb()
                preferences[PreferencesKeys.penWidthKey(penType)] = config.width
                preferences[PreferencesKeys.penAlphaKey(penType)] = config.alpha
                // NEW: Save additional UiState fields
                preferences[PreferencesKeys.TOOLBAR_SCALE] = uiState.toolbarScale
                preferences[PreferencesKeys.TOOLBAR_INACTIVE_ALPHA] = uiState.toolbarInactiveAlpha
            }
        }
    }

    suspend fun getSavedServiceState(): ServiceState {
        val preferences = context.dataStore.data.first()
        val defaultServiceState = ServiceState()
        // NEW: Load saved toolbarActive
        val toolbarActive = preferences[PreferencesKeys.TOOLBAR_ACTIVE] ?: defaultServiceState.toolbarActive

        return ServiceState(
            toolbarPosition = Offset(
                x = preferences[PreferencesKeys.TOOLBAR_POSITION_X] ?: defaultServiceState.toolbarPosition.x,
                y = preferences[PreferencesKeys.TOOLBAR_POSITION_Y] ?: defaultServiceState.toolbarPosition.y
            ),
            toolbarActive = toolbarActive
        )
    }

    suspend fun saveServiceState(serviceState: ServiceState) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOOLBAR_POSITION_X] = serviceState.toolbarPosition.x
            preferences[PreferencesKeys.TOOLBAR_POSITION_Y] = serviceState.toolbarPosition.y
            // NEW: Save toolbarActive
            preferences[PreferencesKeys.TOOLBAR_ACTIVE] = serviceState.toolbarActive
        }
    }
}