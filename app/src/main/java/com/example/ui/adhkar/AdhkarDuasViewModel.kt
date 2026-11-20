package com.example.ui.adhkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.adhkar.AdhkarCategory
import com.example.data.adhkar.DhikrItem
import com.example.data.duas.DuaCategory
import com.example.data.duas.DuaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdhkarDuasUiState(
    val isLoading: Boolean = true,
    val activeTab: Int = 0, // 0 = Adhkar, 1 = Duas
    val selectedAdhkarCategory: AdhkarCategory = AdhkarCategory.MORNING,
    val adhkarByCategory: Map<AdhkarCategory, List<DhikrItem>> = emptyMap(),
    val dhikrRemainingCounts: Map<String, Int> = emptyMap(),
    val selectedDuaCategory: DuaCategory = DuaCategory.QURANIC,
    val duasByCategory: Map<DuaCategory, List<DuaItem>> = emptyMap()
)

class AdhkarDuasViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(AdhkarDuasUiState())
    val uiState: StateFlow<AdhkarDuasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val adhkar = container.adhkarRepository.getAllByCategory()
            val duas = container.duasRepository.getAllByCategory()
            val initialCounts = adhkar.values.flatten().associate { it.id to it.repeatCount }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    adhkarByCategory = adhkar,
                    dhikrRemainingCounts = initialCounts,
                    duasByCategory = duas
                )
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectAdhkarCategory(category: AdhkarCategory) {
        _uiState.update { it.copy(selectedAdhkarCategory = category) }
    }

    fun selectDuaCategory(category: DuaCategory) {
        _uiState.update { it.copy(selectedDuaCategory = category) }
    }

    fun decrementDhikr(item: DhikrItem) {
        _uiState.update { st ->
            val current = st.dhikrRemainingCounts[item.id] ?: item.repeatCount
            if (current <= 0) st
            else st.copy(dhikrRemainingCounts = st.dhikrRemainingCounts + (item.id to (current - 1)))
        }
    }

    fun resetDhikr(item: DhikrItem) {
        _uiState.update { st ->
            st.copy(dhikrRemainingCounts = st.dhikrRemainingCounts + (item.id to item.repeatCount))
        }
    }

    fun resetCategoryCounters(category: AdhkarCategory) {
        _uiState.update { st ->
            val items = st.adhkarByCategory[category].orEmpty()
            val updated = st.dhikrRemainingCounts.toMutableMap()
            items.forEach { updated[it.id] = it.repeatCount }
            st.copy(dhikrRemainingCounts = updated)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdhkarDuasViewModel(container) as T
    }
}
