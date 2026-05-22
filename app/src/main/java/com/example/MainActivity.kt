package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.RecordingDatabase
import com.example.data.RecordingRepository
import com.example.recorder.AudioPlayerManager
import com.example.recorder.AudioRecorderManager
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.RecordDashboard
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize core architecture
        val database = RecordingDatabase.getDatabase(applicationContext)
        val repository = RecordingRepository(database.recordingDao())
        val recorderManager = AudioRecorderManager(applicationContext)
        val playerManager = AudioPlayerManager()

        // Create ViewModel
        val factory = MainViewModelFactory(repository, recorderManager, playerManager)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme(darkTheme = viewModel.isDarkTheme.value, dynamicColor = false) {
                RecordDashboard(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
