package com.example.threadhandlerwizard

import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorkViewModel : ViewModel() {
    private val handlerThread = HandlerThread("VM-Work").apply { start() }
    private val worker = Handler(handlerThread.looper)

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _status = MutableLiveData("Idle")
    val status: LiveData<String> = _status

    @Volatile private var running = false
    private var workRunnable: Runnable? = null

    fun start() {
        if (running) return
        running = true

        _status.postValue("Preparing...")
        _progress.postValue(0)

        workRunnable = Runnable {
            try {
                Thread.sleep(10000)
                if (!running) throw InterruptedException()

                _status.postValue("Working...")
                for (i in 1..100) {
                    if (!running) break
                    Thread.sleep(350)
                    _progress.postValue(i)
                }

                if (running) {
                    _status.postValue("背景工作結束!")
                } else {
                    _status.postValue("Canceled")
                }
                running = false
            } catch (e: InterruptedException) {
                _status.postValue("Canceled")
                running = false
            }
        }.also { worker.post(it) }
    }

    fun cancel() {
        if (!running) return
        running = false
        workRunnable?.let { worker.removeCallbacks(it) }
        handlerThread.looper.thread.interrupt()
    }

    override fun onCleared() {
        cancel()
        handlerThread.quitSafely()
        super.onCleared()
    }
}