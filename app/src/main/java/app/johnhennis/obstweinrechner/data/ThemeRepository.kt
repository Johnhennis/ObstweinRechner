package app.johnhennis.obstweinrechner.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private const val PREFS_NAME = "app_prefs"
private const val KEY_THEME_MODE = "themeMode"

// Rein lokale Einstellung (wie die Schriftgröße), bewusst eigenständig statt
// in die bestehende SettingsRepository eingebaut, um deren unbekannten
// Aufbau nicht anzutasten.
class ThemeRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readSaved())
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private fun readSaved(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}
