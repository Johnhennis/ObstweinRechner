package com.example.fruchtweinrechner.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Rein lokale Einstellung (kein Firestore) - bleibt bewusst auf diesem Gerät.
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _fontScale = MutableStateFlow(prefs.getFloat(KEY_FONT_SCALE, 1.0f))
    val fontScale: StateFlow<Float> = _fontScale

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
        _fontScale.value = scale
    }

    companion object {
        private const val KEY_FONT_SCALE = "font_scale"
    }
}
