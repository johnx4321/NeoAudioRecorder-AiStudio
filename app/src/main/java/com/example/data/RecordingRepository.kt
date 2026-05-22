package com.example.data

import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val recordingDao: RecordingDao) {
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()
    val favoriteRecordings: Flow<List<RecordingEntity>> = recordingDao.getFavoriteRecordings()

    suspend fun insert(recording: RecordingEntity): Long {
        return recordingDao.insertRecording(recording)
    }

    suspend fun update(recording: RecordingEntity) {
        recordingDao.updateRecording(recording)
    }

    suspend fun delete(recording: RecordingEntity) {
        recordingDao.deleteRecording(recording)
    }

    suspend fun deleteById(id: Long) {
        recordingDao.deleteById(id)
    }

    suspend fun getById(id: Long): RecordingEntity? {
        return recordingDao.getRecordingById(id)
    }
}
