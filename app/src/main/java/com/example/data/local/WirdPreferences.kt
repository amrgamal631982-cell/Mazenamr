package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wirdDataStore: DataStore<Preferences> by preferencesDataStore(name = "wird_settings")

data class ReadingPosition(
    val surahIndex: Int = 1,
    val ayahNumber: Int = 1,
    val pageNumber: Int = 1,
    val updatedAt: Long = 0L
)

data class ListeningPosition(
    val reciterId: String = "husary",
    val surahIndex: Int = 1,
    val ayahNumber: Int = 1,
    val positionMs: Long = 0L
)

data class WirdPlanConfig(
    val dailyTargetPages: Int = 20, // ~1 Juz/day for 30-day Khatmah
    val khatmahDays: Int = 30,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 6,
    val reminderMinute: Int = 0
)

data class TasbihConfig(
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val selectedDhikr: String = "سبحان الله",
    val targetCount: Int = 33,
    val currentCount: Int = 0
)

data class SavedLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val label: String = "",
    val updatedAt: Long = 0L
)

class WirdPreferences(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MUSHAF_FONT_SP = floatPreferencesKey("mushaf_font_sp")
        val SHOW_PAUSE_MARKS = booleanPreferencesKey("show_pause_marks")

        val LAST_READ_SURAH = intPreferencesKey("last_read_surah")
        val LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
        val LAST_READ_PAGE = intPreferencesKey("last_read_page")
        val LAST_READ_AT = longPreferencesKey("last_read_at")

        val DEFAULT_RECITER_ID = stringPreferencesKey("default_reciter_id")
        val LAST_LISTEN_RECITER = stringPreferencesKey("last_listen_reciter")
        val LAST_LISTEN_SURAH = intPreferencesKey("last_listen_surah")
        val LAST_LISTEN_AYAH = intPreferencesKey("last_listen_ayah")
        val LAST_LISTEN_MS = longPreferencesKey("last_listen_ms")

        val WIRD_DAILY_PAGES = intPreferencesKey("wird_daily_pages")
        val WIRD_KHATMAH_DAYS = intPreferencesKey("wird_khatmah_days")
        val WIRD_REMINDER_ENABLED = booleanPreferencesKey("wird_reminder_enabled")
        val WIRD_REMINDER_HOUR = intPreferencesKey("wird_reminder_hour")
        val WIRD_REMINDER_MINUTE = intPreferencesKey("wird_reminder_minute")

        val TASBIH_VIBRATION = booleanPreferencesKey("tasbih_vibration")
        val TASBIH_SOUND = booleanPreferencesKey("tasbih_sound")
        val TASBIH_DHIKR = stringPreferencesKey("tasbih_dhikr")
        val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        val TASBIH_CURRENT = intPreferencesKey("tasbih_current")

        val QIBLA_LAT = doublePreferencesKey("qibla_lat")
        val QIBLA_LNG = doublePreferencesKey("qibla_lng")
        val QIBLA_LABEL = stringPreferencesKey("qibla_label")
        val QIBLA_UPDATED_AT = longPreferencesKey("qibla_updated_at")
    }

    val themeMode: Flow<AppThemeMode> = context.wirdDataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            AppThemeMode.DARK.name -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    val mushafFontSizeSp: Flow<Float> = context.wirdDataStore.data.map { prefs ->
        prefs[Keys.MUSHAF_FONT_SP] ?: 24f
    }

    val readingPosition: Flow<ReadingPosition> = context.wirdDataStore.data.map { prefs ->
        ReadingPosition(
            surahIndex = prefs[Keys.LAST_READ_SURAH] ?: 1,
            ayahNumber = prefs[Keys.LAST_READ_AYAH] ?: 1,
            pageNumber = prefs[Keys.LAST_READ_PAGE] ?: 1,
            updatedAt = prefs[Keys.LAST_READ_AT] ?: 0L
        )
    }

    val defaultReciterId: Flow<String> = context.wirdDataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_RECITER_ID] ?: "husary"
    }

    val listeningPosition: Flow<ListeningPosition> = context.wirdDataStore.data.map { prefs ->
        ListeningPosition(
            reciterId = prefs[Keys.LAST_LISTEN_RECITER] ?: (prefs[Keys.DEFAULT_RECITER_ID] ?: "husary"),
            surahIndex = prefs[Keys.LAST_LISTEN_SURAH] ?: 1,
            ayahNumber = prefs[Keys.LAST_LISTEN_AYAH] ?: 1,
            positionMs = prefs[Keys.LAST_LISTEN_MS] ?: 0L
        )
    }

    val wirdPlanConfig: Flow<WirdPlanConfig> = context.wirdDataStore.data.map { prefs ->
        WirdPlanConfig(
            dailyTargetPages = prefs[Keys.WIRD_DAILY_PAGES] ?: 20,
            khatmahDays = prefs[Keys.WIRD_KHATMAH_DAYS] ?: 30,
            reminderEnabled = prefs[Keys.WIRD_REMINDER_ENABLED] ?: false,
            reminderHour = prefs[Keys.WIRD_REMINDER_HOUR] ?: 6,
            reminderMinute = prefs[Keys.WIRD_REMINDER_MINUTE] ?: 0
        )
    }

    val tasbihConfig: Flow<TasbihConfig> = context.wirdDataStore.data.map { prefs ->
        TasbihConfig(
            vibrationEnabled = prefs[Keys.TASBIH_VIBRATION] ?: true,
            soundEnabled = prefs[Keys.TASBIH_SOUND] ?: false,
            selectedDhikr = prefs[Keys.TASBIH_DHIKR] ?: "سبحان الله",
            targetCount = prefs[Keys.TASBIH_TARGET] ?: 33,
            currentCount = prefs[Keys.TASBIH_CURRENT] ?: 0
        )
    }

    val savedLocation: Flow<SavedLocation> = context.wirdDataStore.data.map { prefs ->
        SavedLocation(
            latitude = prefs[Keys.QIBLA_LAT],
            longitude = prefs[Keys.QIBLA_LNG],
            label = prefs[Keys.QIBLA_LABEL] ?: "",
            updatedAt = prefs[Keys.QIBLA_UPDATED_AT] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.wirdDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setMushafFontSize(sp: Float) {
        context.wirdDataStore.edit { it[Keys.MUSHAF_FONT_SP] = sp.coerceIn(18f, 36f) }
    }

    suspend fun saveReadingPosition(surahIndex: Int, ayahNumber: Int, pageNumber: Int) {
        context.wirdDataStore.edit {
            it[Keys.LAST_READ_SURAH] = surahIndex
            it[Keys.LAST_READ_AYAH] = ayahNumber
            it[Keys.LAST_READ_PAGE] = pageNumber
            it[Keys.LAST_READ_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setDefaultReciterId(reciterId: String) {
        context.wirdDataStore.edit { it[Keys.DEFAULT_RECITER_ID] = reciterId }
    }

    suspend fun saveListeningPosition(reciterId: String, surahIndex: Int, ayahNumber: Int, positionMs: Long) {
        context.wirdDataStore.edit {
            it[Keys.LAST_LISTEN_RECITER] = reciterId
            it[Keys.LAST_LISTEN_SURAH] = surahIndex
            it[Keys.LAST_LISTEN_AYAH] = ayahNumber
            it[Keys.LAST_LISTEN_MS] = positionMs
        }
    }

    suspend fun updateWirdPlan(config: WirdPlanConfig) {
        context.wirdDataStore.edit {
            it[Keys.WIRD_DAILY_PAGES] = config.dailyTargetPages.coerceIn(1, 604)
            it[Keys.WIRD_KHATMAH_DAYS] = config.khatmahDays.coerceIn(1, 365)
            it[Keys.WIRD_REMINDER_ENABLED] = config.reminderEnabled
            it[Keys.WIRD_REMINDER_HOUR] = config.reminderHour
            it[Keys.WIRD_REMINDER_MINUTE] = config.reminderMinute
        }
    }

    suspend fun updateTasbihConfig(config: TasbihConfig) {
        context.wirdDataStore.edit {
            it[Keys.TASBIH_VIBRATION] = config.vibrationEnabled
            it[Keys.TASBIH_SOUND] = config.soundEnabled
            it[Keys.TASBIH_DHIKR] = config.selectedDhikr
            it[Keys.TASBIH_TARGET] = config.targetCount
            it[Keys.TASBIH_CURRENT] = config.currentCount
        }
    }

    suspend fun saveQiblaLocation(latitude: Double, longitude: Double, label: String) {
        context.wirdDataStore.edit {
            it[Keys.QIBLA_LAT] = latitude
            it[Keys.QIBLA_LNG] = longitude
            it[Keys.QIBLA_LABEL] = label
            it[Keys.QIBLA_UPDATED_AT] = System.currentTimeMillis()
        }
    }
}
