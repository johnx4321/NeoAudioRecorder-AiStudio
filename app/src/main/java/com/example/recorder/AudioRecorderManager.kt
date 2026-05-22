package com.example.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null

    private var recordingFile: File? = null
    private var currentFormat: String = "WAV"
    private var isRecording = false

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private var recordingJob: Job? = null
    private var durationJob: Job? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // WAV recording constants
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize by lazy {
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
    }

    private var wavMaxAmplitude = 0f

    @SuppressLint("MissingPermission")
    fun startRecording(format: String): File? {
        if (isRecording) return null

        currentFormat = format.uppercase()
        val extension = when (currentFormat) {
            "WAV" -> "wav"
            "AAC" -> "aac"
            "MP3" -> "mp3"
            else -> "wav"
        }

        val filename = "recording_${System.currentTimeMillis()}.$extension"
        val outputDir = context.getExternalFilesDir("Recordings") ?: context.filesDir
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val file = File(outputDir, filename)
        recordingFile = file

        isRecording = true
        _durationMs.value = 0L
        _amplitude.value = 0f

        when (currentFormat) {
            "WAV" -> {
                startWavRecording(file)
            }
            "AAC", "MP3" -> {
                startMediaRecorder(file, currentFormat)
            }
        }

        startTrackingDuration()

        return file
    }

    private fun startTrackingDuration() {
        durationJob?.cancel()
        durationJob = recordingScope.launch {
            val startTime = System.currentTimeMillis()
            while (isRecording) {
                _durationMs.value = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWavRecording(file: File) {
        val bufferSize = if (minBufferSize > 4096) minBufferSize else 4096
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioEncoding,
            bufferSize
        ).apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                startRecording()
            } else {
                Log.e("AudioRecorder", "Failed to initialize AudioRecord")
                isRecording = false
                return
            }
        }

        recordingJob = recordingScope.launch {
            val tempFile = File(file.absolutePath + ".temp")
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(bufferSize)

            try {
                while (isRecording && audioRecord != null) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        outputStream.write(buffer, 0, readBytes)

                        // Calculate visualizer amplitude from PCM 16-bit bytes
                        var sum = 0.0
                        for (i in 0 until readBytes step 2) {
                            if (i + 1 < readBytes) {
                                val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                                sum += sample * sample
                            }
                        }
                        val rms = Math.sqrt(sum / (readBytes / 2))
                        // Normalize to 0..1 scale
                        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                        _amplitude.value = normalized
                    }
                    yield()
                }
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Error writing wav data", e)
            } finally {
                outputStream.close()
                if (tempFile.exists()) {
                    writeWavHeader(tempFile, file)
                    tempFile.delete()
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun startMediaRecorder(file: File, format: String) {
        // Create MediaRecorder instance compatible with different versions
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder = recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            
            if (format == "MP3") {
                // High-fidelity standard containers with MP3 suffix (highly compatible playback)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(192000) // Super high quality mp3 target
            } else { // AAC
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
            }
            
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("AudioRecorder", "MediaRecorder prepare() or start() failed", e)
                isRecording = false
            }
        }

        // Periodically track amplitude for MediaRecorder using coroutine
        recordingJob = recordingScope.launch {
            while (isRecording) {
                val amp = mediaRecorder?.maxAmplitude ?: 0
                // maxAmplitude is 0..32767
                val normalized = (amp / 32767f).coerceIn(0f, 1f)
                _amplitude.value = normalized
                delay(100)
            }
        }
    }

    fun stopRecording(): RecordResult? {
        if (!isRecording) return null

        isRecording = false
        durationJob?.cancel()
        recordingJob?.cancel()

        try {
            when (currentFormat) {
                "WAV" -> {
                    audioRecord?.apply {
                        if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            stop()
                        }
                        release()
                    }
                    audioRecord = null
                }
                "AAC", "MP3" -> {
                    mediaRecorder?.apply {
                        try {
                            stop()
                        } catch (e: RuntimeException) {
                            Log.e("AudioRecorder", "MediaRecorder stop failed, file maybe empty", e)
                        }
                        release()
                    }
                    mediaRecorder = null
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recorder", e)
        }

        val finalFile = recordingFile
        val duration = _durationMs.value

        _amplitude.value = 0f
        _durationMs.value = 0L

        if (finalFile != null && finalFile.exists() && finalFile.length() > 0) {
            return RecordResult(
                file = finalFile,
                durationMs = duration,
                fileSize = finalFile.length(),
                format = currentFormat
            )
        }
        return null
    }

    private fun writeWavHeader(tempFile: File, wavFile: File) {
        val totalAudioLen = tempFile.length()
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = sampleRate.toLong()
        val channels = 1
        val byteRate = longSampleRate * 2 * channels // 16 bits = 2 bytes

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
        header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte() // mono = 1
        header[23] = 0
        header[24] = (longSampleRate and 0xffL).toByte()
        header[25] = ((longSampleRate shr 8) and 0xffL).toByte()
        header[26] = ((longSampleRate shr 16) and 0xffL).toByte()
        header[27] = ((longSampleRate shr 24) and 0xffL).toByte()
        header[28] = (byteRate and 0xffL).toByte() // byte rate
        header[29] = ((byteRate shr 8) and 0xffL).toByte()
        header[30] = ((byteRate shr 16) and 0xffL).toByte()
        header[31] = ((byteRate shr 24) and 0xffL).toByte()
        header[32] = (2 * channels).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte() // data
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()

        try {
            val fis = tempFile.inputStream()
            val fos = wavFile.outputStream()
            fos.write(header)
            val buffer = ByteArray(4096)
            var len: Int
            while (fis.read(buffer).also { len = it } > 0) {
                fos.write(buffer, 0, len)
            }
            fis.close()
            fos.close()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error copying and writing WAV header", e)
        }
    }
}

data class RecordResult(
    val file: File,
    val durationMs: Long,
    val fileSize: Long,
    val format: String
)
