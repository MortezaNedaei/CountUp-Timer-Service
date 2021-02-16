package com.mone.countup_timer_service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mone.countup_timer_service.utils.Constants

open class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        context.sendBroadcast(
            Intent(Constants.ACTION_NOTIFICATION_KEY).apply {
                putExtra(Constants.ACTION_NOTIFICATION_NAME, intent.action)
            })

//        when (intent.action) {
//            Constants.ACTION_STOP -> {}
//        }
    }
}