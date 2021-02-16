package com.mone.countup_timer_service

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mone.countup_timer_service.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow


class CountUpTimerService : Service() {

    private val mBinder: IBinder = TimerBinder()
    private lateinit var chronometer: Chronometer
    private var isBound = false
    private val job = Job()
    var timeFlow: MutableStateFlow<String> = MutableStateFlow("")
    private lateinit var notificationBuilder: Notification.Builder

    private var notificationReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.extras?.getString(Constants.ACTION_NOTIFICATION_NAME)) {
                    Constants.ACTION_STOP -> {
                        Log.i(TAG, "Stop Notification Action has been clicked")
                        stopForegroundService()
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Start Service ..............................")

        startForegroundService()

        isBound = true
        chronometer = Chronometer(this)
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
    }

    private fun startForegroundService() {

        val notification: Notification = getNotification()

        registerReceiver(notificationReceiver, IntentFilter(Constants.ACTION_NOTIFICATION_KEY))

        // Notification ID cannot be 0.
        startForeground(Constants.NOTIFICATION_ID, notification)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getNotification(): Notification {

        // Create an explicit intent for an Activity in your app
        val intentMain = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentMain = PendingIntent.getActivity(this, 0, intentMain, 0)

        val intentStop = Intent(this, NotificationBroadcastReceiver::class.java).apply {
            action = Constants.ACTION_STOP
        }
        val pendingIntentStop = PendingIntent.getBroadcast(
            this, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = Notification.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle(getText(R.string.timer))
                .setContentText("0")
                .setSmallIcon(R.drawable.ic_timer_24)
                .setContentIntent(pendingIntentMain)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setColor(getColor(R.color.purple_700))
                .setColorized(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setTicker(getText(R.string.time))
                .setAutoCancel(true)
                .setStyle(Notification.DecoratedMediaCustomViewStyle())
                //.setCustomContentView(RemoteViews(this.packageName, R.layout.layout_notification))
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_baseline_close_24),
                        getText(R.string.btn_stop),
                        pendingIntentStop
                    ).build()
                )
        }

        return notificationBuilder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        CoroutineScope(job).launch(Dispatchers.IO) {

            // while isBound is true, means the service has a work to do because it is used in
            // While loop, So the service does not stopped and you should break from While loop.
            // Otherwise it recreates the service and notification again (after clicking on stop action)

            while (isBound) {
                if (!isBound) break

                timeFlow.value = getTimestamp().also { time ->
                    updateNotification(time)
                }
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

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Stop Service ...............................")
        isBound = false
        chronometer.stop()
        unregisterReceiver(notificationReceiver)
    }

    private fun getTimestamp(): String {
        val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base

        val hours = (elapsedMillis / 3600000).toInt()
        val minutes = (elapsedMillis - hours * 3600000).toInt() / 60000
        val seconds = (elapsedMillis - hours * 3600000 - minutes * 60000).toInt() / 1000

        val time = getString(R.string.time, hours, minutes, seconds)

        sendBroadcastEvent(Constants.ACTION_TIME_KEY, time)

        return time
    }

    private fun sendBroadcastEvent(action: String, time: String) {
        val timerIntent = Intent(action).apply {
            putExtra(Constants.ACTION_TIME_VALUE, time)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(timerIntent)
    }

    private fun stopForegroundService() {
        Log.d(TAG, "Stop foreground service.")

        isBound = false

        chronometer.stop()

        // Stop foreground service and remove the notification.
        stopForeground(true)

        // Stop the foreground service.
        stopSelf()

        // TODO: send message to activity when Timer going to be stopped (optional)
        sendBroadcastEvent(Constants.ACTION_TIME_KEY, Constants.ACTION_TIMER_STOP)
    }

    /**
     * updates the Notification content by [Constants.NOTIFICATION_ID]
     */
    private fun updateNotification(timestamp: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = notificationBuilder
            .setContentText(timestamp)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }

    inner class TimerBinder : Binder() {
        val service: CountUpTimerService
            get() = this@CountUpTimerService
    }

    companion object {
        private const val TAG = "CountUpTimerService"
    }
}