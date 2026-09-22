package com.marwadiuniversity.trustlens.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marwadiuniversity.trustlens.data.local.room.ScanDatabase
import com.marwadiuniversity.trustlens.data.local.room.ScanHistoryEntity
import com.marwadiuniversity.trustlens.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanHistoryRepository

    init {
        val dao = ScanDatabase.getDatabase(application).scanHistoryDao()
        repository = ScanHistoryRepository(dao)
    }

    val allScans: StateFlow<List<ScanHistoryEntity>> = repository.allScans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var selectedFilter by mutableStateOf("ALL")
        private set

    fun setFilter(filter: String) {
        selectedFilter = filter
    }

    fun deleteScan(scan: ScanHistoryEntity) {
        viewModelScope.launch {
            repository.deleteScan(scan)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
