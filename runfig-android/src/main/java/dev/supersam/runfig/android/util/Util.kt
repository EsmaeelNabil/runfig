package dev.supersam.runfig.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

// get the application context using reflection

@SuppressLint("PrivateApi")
fun getApplicationContext(): Context? {
    return try {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread")
        val currentActivityThread = currentActivityThreadMethod.invoke(null)
        val getApplicationMethod = activityThreadClass.getMethod("getApplication")
        getApplicationMethod.invoke(currentActivityThread) as Context
    } catch (e: Exception) {
        null
    }
}


fun Context.getSharedPreferences(): SharedPreferences {
    return this.getSharedPreferences("runfig", Context.MODE_PRIVATE)
}


