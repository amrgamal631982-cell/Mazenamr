package com.example.ui.reciters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.DownloadedAudioEntity
import com.example.data.local.ListeningPosition
import com.example.data.local.ReciterCoverageEntity
import com.example.data.quran.SurahMeta
import com.example.data.reciters.ReciterCatalog
import com.example.data.reciters.ReciterInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecitersViewModel(private val container: AppContainer) : ViewModel() {
    val reciters: List<ReciterInfo> = ReciterCatalog.RECITERS

    val defaultReciterId: StateFlow<String> = container.preferences.defaultReciterId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "husary")

    val listeningPosition: StateFlow<ListeningPosition> = container.preferences.listeningPosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListeningPosition())

    val downloadedAudio: StateFlow<List<DownloadedAudioEntity>> = container.reciterRepository
        .observeDownloadedAudio()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _surahs = MutableStateFlow<List<SurahMeta>>(emptyList())
    val surahs: StateFlow<List<SurahMeta>> = _surahs.asStateFlow()

    private val _selectedReciterId = MutableStateFlow("husary")
    val selectedReciterId: StateFlow<String> = _selectedReciterId.asStateFlow()

    private val _coverageVerifyProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val coverageVerifyProgress: StateFlow<Pair<Int, Int>?> = _coverageVerifyProgress.asStateFlow()

    private val _downloadingSurah = MutableStateFlow<Map<Pair<String, Int>, Float>>(emptyMap())
    val downloadingSurah: StateFlow<Map<Pair<String, Int>, Float>> = _downloadingSurah.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _surahs.value = container.quranRepository.getAllSurahs()
        }
    }

    fun selectReciter(reciterId: String) {
        _selectedReciterId.value = reciterId
    }

    fun observeCoverageFor(reciterId: String): StateFlow<Map<Int, ReciterCoverageEntity>> {
        val flow = MutableStateFlow<Map<Int, ReciterCoverageEntity>>(emptyMap())
        viewModelScope.launch {
            container.reciterRepository.observeCoverage(reciterId).collect { list ->
                flow.value = list.associateBy { it.surahIndex }
            }
        }
        return flow
    }

    fun setDefaultReciter(reciterId: String) {
        viewModelScope.launch {
            container.preferences.setDefaultReciterId(reciterId)
            _statusMessage.value = "تم تعيين ${ReciterCatalog.findById(reciterId).nameArabic} قارئًا افتراضيًا"
        }
    }

    fun verifySingleSurah(reciterId: String, surahIndex: Int) {
        viewModelScope.launch {
            container.reciterRepository.verifyAndResolveSurahAudio(reciterId, surahIndex)
        }
    }

    fun verifyAllSurahs(reciterId: String) {
        viewModelScope.launch {
            _statusMessage.value = null
            _coverageVerifyProgress.value = 0 to 114
            container.reciterRepository.verifyAllSurahsCoverage(reciterId) { done, total ->
                _coverageVerifyProgress.value = done to total
            }
            _coverageVerifyProgress.value = null
            _statusMessage.value = "تم التحقق الفعلي من توفر السور لهذا القارئ."
        }
    }

    fun downloadSurahForOffline(reciterId: String, surahIndex: Int) {
        val key = reciterId to surahIndex
        if (_downloadingSurah.value.containsKey(key)) return
        viewModelScope.launch {
            _downloadingSurah.update { it + (key to 0f) }
            val result = container.reciterRepository.downloadSurahAudio(reciterId, surahIndex) { read, total ->
                val frac = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0.5f
                _downloadingSurah.update { it + (key to frac) }
            }
            _downloadingSurah.update { it - key }
            if (result.isFailure) {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "تعذر تحميل السورة"
            } else {
                _statusMessage.value = "تم حفظ السورة للاستماع دون إنترنت"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecitersViewModel(container) as T
    }
}
