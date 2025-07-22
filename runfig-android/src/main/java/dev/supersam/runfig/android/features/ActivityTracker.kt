package dev.supersam.runfig.android.features

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ActivityTracker {
    private val logMessages = mutableListOf<String>()
    private const val MAX_LOG_SIZE = 100
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(activityName: String, event: String) {
        synchronized(logMessages) {
            if (logMessages.size >= MAX_LOG_SIZE) {
                logMessages.removeAt(0)
            }
            logMessages.add("${dateFormat.format(Date())} $activityName: $event")
        }
    }

    fun getLog(): List<String> {
        return synchronized(logMessages) { logMessages.toList() }
    }
}