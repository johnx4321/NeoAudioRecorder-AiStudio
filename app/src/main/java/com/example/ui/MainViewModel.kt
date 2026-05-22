package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.RecordingEntity
import com.example.data.RecordingRepository
import com.example.recorder.AudioPlayerManager
import com.example.recorder.AudioRecorderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val repository: RecordingRepository,
    private val recorderManager: AudioRecorderManager,
    private val playerManager: AudioPlayerManager
) : ViewModel() {

    // Theme toggling (default is true = dark theme)
    val isDarkTheme = mutableStateOf(true)

    // Selection format ("WAV", "AAC", "MP3")
    val selectedFormat = mutableStateOf("WAV")

    // Active screen index: 0 = Record screen, 1 = Recordings History screen
    val activeTab = mutableStateOf(0)

    // Recording States
    val isRecording = MutableStateFlow(false)
    val amplitude: StateFlow<Float> = recorderManager.amplitude
    val recordedDurationMs: StateFlow<Long> = recorderManager.durationMs

    // Playback States
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val playDurationMs: StateFlow<Long> = playerManager.durationMs
    val activePlaybackEntity = MutableStateFlow<RecordingEntity?>(null)

    // Audio list from Database repository
    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteRecordings: StateFlow<List<RecordingEntity>> = repository.favoriteRecordings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Error and success notification state
    val messageText = MutableStateFlow<String?>(null)

    fun startRecording() {
        if (isRecording.value) return
        
        // Stop playback when starting new record
        stopPlayback()

        val file = recorderManager.startRecording(selectedFormat.value)
        if (file != null) {
            isRecording.value = true
            showFlashMessage("Recording started in ${selectedFormat.value} format...")
        } else {
            showFlashMessage("Failed to start recording. Please verify Mic Permissions.")
        }
    }

    fun stopRecording() {
        if (!isRecording.value) return

        val result = recorderManager.stopRecording()
        isRecording.value = false

        if (result != null) {
            viewModelScope.launch {
                val niceName = createNiceName(result.format)
                val entity = RecordingEntity(
                    name = niceName,
                    filePath = result.file.absolutePath,
                    durationMs = result.durationMs,
                    fileSize = result.fileSize,
                    format = result.format,
                    timestamp = System.currentTimeMillis()
                )
                repository.insert(entity)
                showFlashMessage("Saved: $niceName!")
            }
        } else {
            showFlashMessage("Recording stopped. File is empty.")
        }
    }

    private fun createNiceName(format: String): String {
        val totalCount = recordings.value.size + 1
        return "Audio Node $totalCount (${format.uppercase()})"
    }

    private fun showFlashMessage(msg: String) {
        messageText.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (messageText.value == msg) {
                messageText.value = null
            }
        }
    }

    fun dismissMessage() {
        messageText.value = null
    }

    // Playback control
    fun togglePlayback(entity: RecordingEntity) {
        val file = File(entity.filePath)
        if (!file.exists()) {
            showFlashMessage("File not found! It may have been deleted outside the app.")
            removeBrokenRecording(entity)
            return
        }

        val active = activePlaybackEntity.value
        if (active?.id == entity.id) {
            if (isPlaying.value) {
                playerManager.pause()
            } else {
                playerManager.play(file)
            }
        } else {
            activePlaybackEntity.value = entity
            playerManager.play(file)
        }
    }

    fun seekPlayback(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun stopPlayback() {
        playerManager.stop()
        activePlaybackEntity.value = null
    }

    fun toggleFavorite(entity: RecordingEntity) {
        viewModelScope.launch {
            val updated = entity.copy(isFavorite = !entity.isFavorite)
            repository.update(updated)
            // If the playing item is updated, also update active entity
            if (activePlaybackEntity.value?.id == entity.id) {
                activePlaybackEntity.value = updated
            }
        }
    }

    fun deleteRecording(entity: RecordingEntity) {
        viewModelScope.launch {
            // Stop playback if deleting the active file
            if (activePlaybackEntity.value?.id == entity.id) {
                stopPlayback()
            }
            
            // Delete actual file
            val file = File(entity.filePath)
            if (file.exists()) {
                file.delete()
            }
            
            // Delete in database
            repository.delete(entity)
            showFlashMessage("Recording deleted.")
        }
    }

    private fun removeBrokenRecording(entity: RecordingEntity) {
        viewModelScope.launch {
            repository.delete(entity)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.stopRecording()
        playerManager.stop()
    }
}

class MainViewModelFactory(
    private val repository: RecordingRepository,
    private val recorderManager: AudioRecorderManager,
    private val playerManager: AudioPlayerManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, recorderManager, playerManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
