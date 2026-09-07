package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioRecordHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordFile: File? = null

    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    companion object {
        private const val TAG = "AudioRecordHelper"
    }

    fun startRecording(): Result<File> {
        return try {
            stopPlaying()
            val audioFile = File(context.cacheDir, "triage_voice_${System.currentTimeMillis()}.m4a")
            currentRecordFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            Result.success(audioFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            isRecording = false
            mediaRecorder?.release()
            mediaRecorder = null
            Result.failure(e)
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return currentRecordFile
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            currentRecordFile
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentRecordFile
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
        } catch (ignored: Exception) {
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentRecordFile?.delete()
            currentRecordFile = null
        }
    }

    fun playAudio(file: File, onCompletion: () -> Unit) {
        stopPlaying()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioRecordHelper.isPlaying = false
                    onCompletion()
                }
                start()
            }
            this.isPlaying = true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            this.isPlaying = false
        }
    }

    fun stopPlaying() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (ignored: Exception) {
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun copyUriToTempFile(uri: Uri): Pair<File, String>? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "audio/mp4"
            val extension = when {
                mimeType.contains("ogg") -> "ogg"
                mimeType.contains("opus") -> "opus"
                mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
                mimeType.contains("wav") -> "wav"
                else -> "m4a"
            }

            val tempFile = File(context.cacheDir, "uploaded_audio_${System.currentTimeMillis()}.$extension")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            Pair(tempFile, mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "Error copying uri audio", e)
            null
        }
    }

    fun cleanup() {
        stopPlaying()
        if (isRecording) {
            cancelRecording()
        }
    }
}
