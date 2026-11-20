package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.adhkar.AdhkarCategory
import com.example.data.adhkar.AdhkarRepository
import com.example.data.local.ReadingPosition
import com.example.data.local.WirdLogEntity
import com.example.data.local.WirdPlanConfig
import com.example.data.quran.QuranRepository
import com.example.data.quran.SurahMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HomeUiState(
    val isLoading: Boolean = true,
    val greeting: String = "السلام عليكم ورحمة الله وبركاته",
    val timeSubtitle: String = "",
    val readingPosition: ReadingPosition = ReadingPosition(),
    val lastSurahName: String = "الفاتحة",
    val surahOfTheDay: SurahMeta? = null,
    val wirdPlan: WirdPlanConfig = WirdPlanConfig(),
    val todayPagesRead: Int = 0,
    val totalPagesReadInKhatmah: Int = 0,
    val suggestedAdhkarCategory: AdhkarCategory = AdhkarCategory.MORNING,
    val suggestedAdhkarCount: Int = 0,
    val totalTasbihCount: Long = 0L
) {
    val wirdProgressFraction: Float
        get() = (todayPagesRead.toFloat() / wirdPlan.dailyTargetPages.coerceAtLeast(1)).coerceIn(0f, 1f)

    val khatmahProgressFraction: Float
        get() = (readingPosition.pageNumber.toFloat() / 604f).coerceIn(0f, 1f)
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val surahsFlow = MutableStateFlow<List<SurahMeta>>(emptyList())
    private val morningEveningCountsFlow = MutableStateFlow<Pair<Int, Int>>(0 to 0)

    private val todayIso: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val uiState: StateFlow<HomeUiState> = combine(
        container.preferences.readingPosition,
        container.preferences.wirdPlanConfig,
        container.database.wirdLogDao().observeLogs(),
        container.database.tasbihDao().observeTotalCount(),
        surahsFlow,
        morningEveningCountsFlow
    ) { values ->
        val pos = values[0] as ReadingPosition
        val plan = values[1] as WirdPlanConfig
        @Suppress("UNCHECKED_CAST")
        val logs = values[2] as List<WirdLogEntity>
        val tasbihTotal = values[3] as Long
        @Suppress("UNCHECKED_CAST")
        val surahs = values[4] as List<SurahMeta>
        @Suppress("UNCHECKED_CAST")
        val counts = values[5] as Pair<Int, Int>

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        val (greeting, subtitle, suggestedCategory, suggestedCount) = when {
            hour in 4..11 -> listOf("صباح الخير والبركة", "ابدأ يومك بذكر الله ووردك اليومي", AdhkarCategory.MORNING, counts.first)
            hour in 12..14 -> listOf("طاب يومك بذكر الله", "لا تنسَ وردك اليومي وتلاوة القرآن", AdhkarCategory.MORNING, counts.first)
            else -> listOf("مساء الخير والسكينة", "وقت أذكار المساء ومراجعة الورد", AdhkarCategory.EVENING, counts.second)
        }

        val lastSurah = surahs.firstOrNull { it.index == pos.surahIndex }
        val surahOfDay = if (surahs.isNotEmpty()) surahs[(dayOfYear - 1) % surahs.size] else null
        val todayLog = logs.firstOrNull { it.dateIso == todayIso }
        val totalLoggedPages = logs.sumOf { it.pagesRead }

        HomeUiState(
            isLoading = surahs.isEmpty(),
            greeting = greeting as String,
            timeSubtitle = subtitle as String,
            readingPosition = pos,
            lastSurahName = lastSurah?.nameArabic ?: "الفاتحة",
            surahOfTheDay = surahOfDay,
            wirdPlan = plan,
            todayPagesRead = todayLog?.pagesRead ?: 0,
            totalPagesReadInKhatmah = totalLoggedPages,
            suggestedAdhkarCategory = suggestedCategory as AdhkarCategory,
            suggestedAdhkarCount = suggestedCount as Int,
            totalTasbihCount = tasbihTotal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            val surahs = container.quranRepository.getAllSurahs()
            surahsFlow.value = surahs
            val adhkar = container.adhkarRepository.getAllByCategory()
            morningEveningCountsFlow.value =
                (adhkar[AdhkarCategory.MORNING]?.size ?: 0) to (adhkar[AdhkarCategory.EVENING]?.size ?: 0)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(container) as T
    }
}
