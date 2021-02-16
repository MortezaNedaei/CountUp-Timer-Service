package com.mone.countup_timer_service.utils

import android.app.ActivityManager
import android.app.Service
import android.content.Context

fun Context.isServiceRunningInForeground(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            if (service.foreground) {
                return true
            }
        }
    }
    return false
}