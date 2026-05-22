package com.example.recorder

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AudioPlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private var currentPlayingFile: String? = null

    fun play(file: File) {
        val filePath = file.absolutePath
        if (_isPlaying.value && currentPlayingFile == filePath) {
            pause()
            return
        }

        if (currentPlayingFile == filePath && mediaPlayer != null) {
            // Resume
            mediaPlayer?.start()
            _isPlaying.value = true
            startTrackingProgress()
            return
        }

        // Play new file
        stop()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
                _isPlaying.value = true
                _durationMs.value = duration.toLong()
                _currentPositionMs.value = 0L
                currentPlayingFile = filePath
                
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = duration.toLong()
                    progressJob?.cancel()
                }

                startTrackingProgress()
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error preparing or starting MediaPlayer", e)
                _isPlaying.value = false
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error stopping player", e)
            }
            release()
        }
        mediaPlayer = null
        currentPlayingFile = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    private fun startTrackingProgress() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPositionMs.value = it.currentPosition.toLong()
                    }
                }
                delay(100)
            }
        }
    }
}
