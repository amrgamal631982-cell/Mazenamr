package com.example.ui.mushaf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.BookmarkEntity
import com.example.data.local.BookmarkFolderEntity
import com.example.data.quran.Ayah
import com.example.data.quran.HizbMeta
import com.example.data.quran.JuzMeta
import com.example.data.quran.SajdaMeta
import com.example.data.quran.SurahMeta
import com.example.data.tafsir.AyahStudyBundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MushafIndexTab(val labelArabic: String) {
    SURAHS("السور (114)"),
    JUZS("الأجزاء (30)"),
    HIZBS("الأحزاب (60)"),
    PAGES("الصفحات (604)"),
    SAJDAS("السجدات (15)")
}

enum class ReaderViewMode(val labelArabic: String) {
    BY_PAGE("عرض الصفحة (604)"),
    BY_SURAH("عرض السورة كاملة")
}

data class MushafIndexUiState(
    val isLoading: Boolean = true,
    val selectedTab: MushafIndexTab = MushafIndexTab.SURAHS,
    val filterQuery: String = "",
    val surahs: List<SurahMeta> = emptyList(),
    val juzs: List<JuzMeta> = emptyList(),
    val hizbs: List<HizbMeta> = emptyList(),
    val sajdas: List<SajdaMeta> = emptyList()
)

data class MushafReaderUiState(
    val isLoading: Boolean = true,
    val viewMode: ReaderViewMode = ReaderViewMode.BY_PAGE,
    val currentPage: Int = 1,
    val currentSurah: SurahMeta? = null,
    val ayahs: List<Ayah> = emptyList(),
    val allSurahs: Map<Int, SurahMeta> = emptyMap(),
    val selectedAyah: Ayah? = null,
    val studyBundle: AyahStudyBundle? = null,
    val isLoadingStudy: Boolean = false,
    val isSelectedAyahBookmarked: Boolean = false,
    val selectedAyahNote: String = "",
    val selectedAyahFolderId: Long = 1L
)

class MushafViewModel(private val container: AppContainer) : ViewModel() {
    private val _indexState = MutableStateFlow(MushafIndexUiState())
    val indexState: StateFlow<MushafIndexUiState> = _indexState.asStateFlow()

    private val _readerState = MutableStateFlow(MushafReaderUiState())
    val readerState: StateFlow<MushafReaderUiState> = _readerState.asStateFlow()

    val folders: StateFlow<List<BookmarkFolderEntity>> = container.database.bookmarkDao()
        .observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = container.database.bookmarkDao()
        .observeAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fontSizeSp: StateFlow<Float> = container.preferences.mushafFontSizeSp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24f)

    val defaultReciterId: StateFlow<String> = container.preferences.defaultReciterId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "husary")

    init {
        viewModelScope.launch {
            val surahs = container.quranRepository.getAllSurahs()
            val juzs = container.quranRepository.getAllJuzs()
            val hizbs = container.quranRepository.getAllHizbs()
            val sajdas = container.quranRepository.getAllSajdas()
            _indexState.update {
                it.copy(
                    isLoading = false,
                    surahs = surahs,
                    juzs = juzs,
                    hizbs = hizbs,
                    sajdas = sajdas
                )
            }
            _readerState.update {
                it.copy(allSurahs = surahs.associateBy { s -> s.index })
            }
        }
    }

    fun selectIndexTab(tab: MushafIndexTab) {
        _indexState.update { it.copy(selectedTab = tab) }
    }

    fun updateIndexFilter(query: String) {
        _indexState.update { it.copy(filterQuery = query) }
    }

    fun openPosition(surahIndex: Int, ayahNumber: Int, pageNumber: Int?) {
        viewModelScope.launch {
            _readerState.update { it.copy(isLoading = true) }
            val targetPage = pageNumber ?: container.quranRepository.getAyah(surahIndex, ayahNumber)?.pageNumber ?: 1
            if (_readerState.value.viewMode == ReaderViewMode.BY_PAGE) {
                loadPageInternal(targetPage)
            } else {
                loadSurahInternal(surahIndex)
            }
        }
    }

    fun switchViewMode(mode: ReaderViewMode) {
        viewModelScope.launch {
            _readerState.update { it.copy(viewMode = mode, isLoading = true) }
            val cur = _readerState.value
            if (mode == ReaderViewMode.BY_PAGE) {
                val p = cur.ayahs.firstOrNull()?.pageNumber ?: cur.currentPage
                loadPageInternal(p)
            } else {
                val s = cur.currentSurah?.index ?: cur.ayahs.firstOrNull()?.surahIndex ?: 1
                loadSurahInternal(s)
            }
        }
    }

    fun loadPage(pageNumber: Int) {
        viewModelScope.launch {
            _readerState.update { it.copy(viewMode = ReaderViewMode.BY_PAGE, isLoading = true) }
            loadPageInternal(pageNumber.coerceIn(1, 604))
        }
    }

    fun loadSurah(surahIndex: Int) {
        viewModelScope.launch {
            _readerState.update { it.copy(viewMode = ReaderViewMode.BY_SURAH, isLoading = true) }
            loadSurahInternal(surahIndex.coerceIn(1, 114))
        }
    }

    private suspend fun loadPageInternal(pageNumber: Int) {
        val surahs = container.quranRepository.getAllSurahs().associateBy { it.index }
        val ayahs = container.quranRepository.getPageAyahs(pageNumber)
        val first = ayahs.firstOrNull()
        _readerState.update {
            it.copy(
                isLoading = false,
                currentPage = pageNumber,
                currentSurah = first?.let { a -> surahs[a.surahIndex] },
                ayahs = ayahs,
                allSurahs = surahs
            )
        }
        if (first != null) {
            container.preferences.saveReadingPosition(first.surahIndex, first.ayahNumber, pageNumber)
        }
    }

    private suspend fun loadSurahInternal(surahIndex: Int) {
        val surahs = container.quranRepository.getAllSurahs().associateBy { it.index }
        val ayahs = container.quranRepository.getSurahAyahs(surahIndex)
        val first = ayahs.firstOrNull()
        _readerState.update {
            it.copy(
                isLoading = false,
                currentPage = first?.pageNumber ?: 1,
                currentSurah = surahs[surahIndex],
                ayahs = ayahs,
                allSurahs = surahs
            )
        }
        if (first != null) {
            container.preferences.saveReadingPosition(surahIndex, first.ayahNumber, first.pageNumber)
        }
    }

    fun onAyahLongPressed(ayah: Ayah) {
        viewModelScope.launch {
            val existing = container.database.bookmarkDao().findBookmark(ayah.surahIndex, ayah.ayahNumber)
            _readerState.update {
                it.copy(
                    selectedAyah = ayah,
                    studyBundle = null,
                    isLoadingStudy = true,
                    isSelectedAyahBookmarked = existing != null,
                    selectedAyahNote = existing?.personalNote ?: "",
                    selectedAyahFolderId = existing?.folderId ?: 1L
                )
            }
            val bundle = container.tafsirRepository.loadAyahStudy(
                surahIndex = ayah.surahIndex,
                ayahNumber = ayah.ayahNumber,
                fetchRemoteIfMissing = true
            )
            _readerState.update {
                it.copy(studyBundle = bundle, isLoadingStudy = false)
            }
        }
    }

    fun refreshSelectedAyahStudy() {
        val ayah = _readerState.value.selectedAyah ?: return
        onAyahLongPressed(ayah)
    }

    fun dismissAyahSheet() {
        _readerState.update { it.copy(selectedAyah = null, studyBundle = null) }
    }

    fun saveAyahBookmark(ayah: Ayah, folderId: Long, personalNote: String) {
        viewModelScope.launch {
            val surahName = _readerState.value.allSurahs[ayah.surahIndex]?.nameArabic ?: ""
            val existing = container.database.bookmarkDao().findBookmark(ayah.surahIndex, ayah.ayahNumber)
            container.database.bookmarkDao().upsertBookmark(
                BookmarkEntity(
                    id = existing?.id ?: 0L,
                    surahIndex = ayah.surahIndex,
                    ayahNumber = ayah.ayahNumber,
                    pageNumber = ayah.pageNumber,
                    surahName = surahName,
                    uthmaniSnippet = ayah.uthmaniText,
                    folderId = folderId,
                    personalNote = personalNote.trim(),
                    isReadingPosition = false
                )
            )
            _readerState.update {
                it.copy(
                    isSelectedAyahBookmarked = true,
                    selectedAyahNote = personalNote.trim(),
                    selectedAyahFolderId = folderId
                )
            }
        }
    }

    fun removeAyahBookmark(ayah: Ayah) {
        viewModelScope.launch {
            container.database.bookmarkDao().deleteByAyah(ayah.surahIndex, ayah.ayahNumber)
            _readerState.update { it.copy(isSelectedAyahBookmarked = false, selectedAyahNote = "") }
        }
    }

    fun markAyahAsLastRead(ayah: Ayah) {
        viewModelScope.launch {
            container.preferences.saveReadingPosition(ayah.surahIndex, ayah.ayahNumber, ayah.pageNumber)
        }
    }

    fun adjustFontSize(newSp: Float) {
        viewModelScope.launch {
            container.preferences.setMushafFontSize(newSp)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MushafViewModel(container) as T
    }
}
