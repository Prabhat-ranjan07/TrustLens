package com.marwadiuniversity.trustlens.data.repository

import com.marwadiuniversity.trustlens.data.local.room.ScanHistoryDao
import com.marwadiuniversity.trustlens.data.local.room.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

class ScanHistoryRepository(private val dao: ScanHistoryDao) {
    val allScans: Flow<List<ScanHistoryEntity>> = dao.getAllScans()

    suspend fun getScanById(id: Long): ScanHistoryEntity? {
        return dao.getScanById(id)
    }

    suspend fun insertScan(scan: ScanHistoryEntity) {
        dao.insertScan(scan)
    }

    suspend fun deleteScan(scan: ScanHistoryEntity) {
        dao.deleteScan(scan)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }
}
