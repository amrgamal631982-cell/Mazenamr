package com.example.ui.tools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.BookmarkEntity
import com.example.data.local.BookmarkFolderEntity
import com.example.data.local.DownloadedAudioEntity
import com.example.data.local.TasbihConfig
import com.example.data.local.TasbihSessionEntity
import com.example.data.local.WirdLogEntity
import com.example.data.local.WirdPlanConfig
import com.example.data.quran.QuranAuditReport
import com.example.data.quran.QuranSearchResult
import com.example.data.tafsir.TafsirPackStatus
import com.example.worker.WirdReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolsViewModel(private val container: AppContainer) : ViewModel() {

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<QuranSearchResult>>(emptyList())
    val searchResults: StateFlow<List<QuranSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Tasbih
    val tasbihConfig: StateFlow<TasbihConfig> = container.preferences.tasbihConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasbihConfig())

    val tasbihSessions: StateFlow<List<TasbihSessionEntity>> = container.database.tasbihDao()
        .observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTasbihAllTime: StateFlow<Long> = container.database.tasbihDao()
        .observeTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Wird Tracker
    val wirdPlan: StateFlow<WirdPlanConfig> = container.preferences.wirdPlanConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WirdPlanConfig())

    val wirdLogs: StateFlow<List<WirdLogEntity>> = container.database.wirdLogDao()
        .observeLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingPosition = container.preferences.readingPosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.local.ReadingPosition())

    // Bookmarks
    val folders: StateFlow<List<BookmarkFolderEntity>> = container.database.bookmarkDao()
        .observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = container.database.bookmarkDao()
        .observeAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Storage
    private val _tafsirPacks = MutableStateFlow<List<TafsirPackStatus>>(emptyList())
    val tafsirPacks: StateFlow<List<TafsirPackStatus>> = _tafsirPacks.asStateFlow()

    private val _wordMeaningsBytes = MutableStateFlow(0L)
    val wordMeaningsBytes: StateFlow<Long> = _wordMeaningsBytes.asStateFlow()

    private val _packDownloadProgress = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val packDownloadProgress: StateFlow<Map<String, Pair<Int, Int>>> = _packDownloadProgress.asStateFlow()

    private val _storageMessage = MutableStateFlow<String?>(null)
    val storageMessage: StateFlow<String?> = _storageMessage.asStateFlow()

    val downloadedAudio: StateFlow<List<DownloadedAudioEntity>> = container.reciterRepository
        .observeDownloadedAudio()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAudioBytes: StateFlow<Long> = container.reciterRepository
        .observeTotalAudioBytes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Audit
    private val _auditReport = MutableStateFlow<QuranAuditReport?>(null)
    val auditReport: StateFlow<QuranAuditReport?> = _auditReport.asStateFlow()

    private val _isAuditing = MutableStateFlow(false)
    val isAuditing: StateFlow<Boolean> = _isAuditing.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = container.quranRepository.searchQuran(query)
            _isSearching.value = false
        }
    }

    // Tasbih actions
    fun incrementTasbih() {
        viewModelScope.launch {
            val cur = tasbihConfig.value
            val next = cur.currentCount + 1
            container.preferences.updateTasbihConfig(cur.copy(currentCount = next))
        }
    }

    fun resetTasbihAndSaveSession() {
        viewModelScope.launch {
            val cur = tasbihConfig.value
            if (cur.currentCount > 0) {
                container.database.tasbihDao().insertSession(
                    TasbihSessionEntity(
                        dhikrText = cur.selectedDhikr,
                        count = cur.currentCount,
                        target = cur.targetCount
                    )
                )
            }
            container.preferences.updateTasbihConfig(cur.copy(currentCount = 0))
        }
    }

    fun saveTasbihSessionOnly() {
        viewModelScope.launch {
            val cur = tasbihConfig.value
            if (cur.currentCount > 0) {
                container.database.tasbihDao().insertSession(
                    TasbihSessionEntity(
                        dhikrText = cur.selectedDhikr,
                        count = cur.currentCount,
                        target = cur.targetCount
                    )
                )
                container.preferences.updateTasbihConfig(cur.copy(currentCount = 0))
            }
        }
    }

    fun updateTasbihSettings(
        dhikr: String = tasbihConfig.value.selectedDhikr,
        target: Int = tasbihConfig.value.targetCount,
        vibration: Boolean = tasbihConfig.value.vibrationEnabled,
        sound: Boolean = tasbihConfig.value.soundEnabled
    ) {
        viewModelScope.launch {
            container.preferences.updateTasbihConfig(
                tasbihConfig.value.copy(
                    selectedDhikr = dhikr,
                    targetCount = target.coerceIn(1, 10000),
                    vibrationEnabled = vibration,
                    soundEnabled = sound
                )
            )
        }
    }

    // Wird Tracker actions
    fun updateWirdPlan(context: Context, dailyPages: Int, khatmahDays: Int, reminderEnabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            val newConfig = WirdPlanConfig(
                dailyTargetPages = dailyPages.coerceIn(1, 604),
                khatmahDays = khatmahDays.coerceIn(1, 365),
                reminderEnabled = reminderEnabled,
                reminderHour = hour,
                reminderMinute = minute
            )
            container.preferences.updateWirdPlan(newConfig)
            if (reminderEnabled) {
                WirdReminderWorker.scheduleDaily(context, hour, minute)
            } else {
                WirdReminderWorker.cancel(context)
            }
        }
    }

    fun logTodayPages(pagesDelta: Int) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val existing = container.database.wirdLogDao().getLogByDate(today)
            val newPages = ((existing?.pagesRead ?: 0) + pagesDelta).coerceIn(0, 604)
            val pos = readingPosition.value
            val target = wirdPlan.value.dailyTargetPages
            container.database.wirdLogDao().upsertLog(
                WirdLogEntity(
                    dateIso = today,
                    pagesRead = newPages,
                    fromSurah = existing?.fromSurah ?: pos.surahIndex,
                    fromAyah = existing?.fromAyah ?: pos.ayahNumber,
                    toSurah = pos.surahIndex,
                    toAyah = pos.ayahNumber,
                    completed = newPages >= target
                )
            )
        }
    }

    // Bookmarks actions
    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            container.database.bookmarkDao().insertFolder(BookmarkFolderEntity(name = name.trim()))
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            container.database.bookmarkDao().deleteBookmarkById(id)
        }
    }

    fun updateBookmarkNote(bookmark: BookmarkEntity, newNote: String, folderId: Long) {
        viewModelScope.launch {
            container.database.bookmarkDao().upsertBookmark(
                bookmark.copy(personalNote = newNote.trim(), folderId = folderId)
            )
        }
    }

    // Storage actions
    fun refreshStorageStats() {
        viewModelScope.launch {
            _tafsirPacks.value = container.tafsirRepository.getPackStatuses()
            _wordMeaningsBytes.value = container.tafsirRepository.getWordMeaningsCacheBytes()
        }
    }

    fun downloadTafsirPack(sourceId: String) {
        viewModelScope.launch {
            _storageMessage.value = null
            _packDownloadProgress.update { it + (sourceId to (0 to 114)) }
            val result = container.tafsirRepository.downloadFullPack(sourceId) { done, total ->
                _packDownloadProgress.update { it + (sourceId to (done to total)) }
            }
            _packDownloadProgress.update { it - sourceId }
            _storageMessage.value = if (result.isSuccess) {
                "تم تنزيل الحزمة كاملة (114 سورة) بنجاح للعمل دون إنترنت."
            } else {
                "تعذر إكمال التنزيل: ${result.exceptionOrNull()?.localizedMessage ?: "تحقق من الإنترنت"}"
            }
            refreshStorageStats()
        }
    }

    fun deleteTafsirPack(sourceId: String) {
        viewModelScope.launch {
            container.tafsirRepository.deletePack(sourceId)
            refreshStorageStats()
        }
    }

    fun clearWordMeaningsCache() {
        viewModelScope.launch {
            container.tafsirRepository.clearWordMeaningsCache()
            refreshStorageStats()
        }
    }

    fun deleteAudioFile(entity: DownloadedAudioEntity) {
        viewModelScope.launch {
            container.reciterRepository.deleteDownloadedFile(entity)
        }
    }

    fun deleteAllAudio() {
        viewModelScope.launch {
            container.reciterRepository.deleteAllAudio()
        }
    }

    // Audit action
    fun runQuranAudit() {
        viewModelScope.launch {
            _isAuditing.value = true
            _auditReport.value = container.quranRepository.runFullAudit()
            _isAuditing.value = false
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ToolsViewModel(container) as T
    }
}
