package com.mone.countup_timer_service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow


class CountUpTimerService : Service() {

    private val mBinder: IBinder = MyBinder()
    private lateinit var mChronometer: Chronometer
    private var isBound = false
    private val job = Job()
    var timeFlow: MutableStateFlow<String> = MutableStateFlow("")

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Start Service ..............................")
        isBound = true
        mChronometer = Chronometer(this)
        mChronometer.base = SystemClock.elapsedRealtime()
        mChronometer.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        CoroutineScope(job).launch(Dispatchers.IO) {
            while (isBound) {
                if (!isBound) break

                timeFlow.value = getTimestamp()
                Log.i(TAG, timeFlow.value)
                delay(1000)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        isBound = true
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Stop Service ...............................")
        isBound = false
        mChronometer.stop()
    }

    private fun getTimestamp(): String {
        val elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.base

        val hours = (elapsedMillis / 3600000).toInt()
        val minutes = (elapsedMillis - hours * 3600000).toInt() / 60000
        val seconds = (elapsedMillis - hours * 3600000 - minutes * 60000).toInt() / 1000
        return resources.getString(R.string.time, hours, minutes, seconds)
    }

    inner class MyBinder : Binder() {
        val service: CountUpTimerService
            get() = this@CountUpTimerService
    }

    companion object {
        private val TAG = "CountUpTimerService"
    }
}