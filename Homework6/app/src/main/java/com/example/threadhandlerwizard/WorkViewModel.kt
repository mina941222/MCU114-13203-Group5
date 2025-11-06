package com.example.threadhandlerwizard

import android.app.Application
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class WorkViewModel(private val app: Application) : AndroidViewModel(app) {
    private val handlerThread = HandlerThread("VM-Work").apply { start() }
    private val worker = Handler(handlerThread.looper)

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _status = MutableLiveData("Idle")
    val status: LiveData<String> = _status

    private var mediaPlayer: MediaPlayer? = null

    @Volatile private var running = false
    private var workRunnable: Runnable? = null

    fun start() {
        if (running) return
        running = true

        _status.postValue("Preparing...")
        _progress.postValue(0)

        startMusic()

        workRunnable = Runnable {
            try {
                Thread.sleep(10000)
                if (!running) throw InterruptedException()

                _status.postValue("Working")
                for (i in 1..100) {
                    if (!running) break
                    Thread.sleep(350)
                    _progress.postValue(i)
                }

                // 3. Finish
                if (running) {
                    _status.postValue("Work Finished")
                } else {
                    _status.postValue("Canceled")
                }
                running = false
            } catch (e: InterruptedException) {
                _status.postValue("Canceled")
                running = false
            } finally {
                stopMusic()
            }
        }.also { worker.post(it) }
    }

    fun cancel() {
        if (!running) return
        running = false
        workRunnable?.let { worker.removeCallbacks(it) }
        handlerThread.looper.thread.interrupt()

        stopMusic()
    }

    override fun onCleared() {
        cancel()
        handlerThread.quitSafely()
        stopMusic()
        super.onCleared()
    }

    private fun startMusic() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(app, R.raw.background_music)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    private fun stopMusic() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            mediaPlayer = null
        }
    }
}