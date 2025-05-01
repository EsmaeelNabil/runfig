package dev.supersam.runfig.android.initialization

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogManager {
    private const val CRASH_LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 10

    fun logCrash(context: Context, throwable: Throwable) {
        try {
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val logFile = File(logDir, "crash_$timestamp.log")

            FileOutputStream(logFile).use { fos ->
                PrintWriter(fos).use { writer ->
                    val sw = StringWriter()
                    throwable.printStackTrace(PrintWriter(sw))
                    writer.println(
                        "Timestamp: ${
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(
                                Date()
                            )
                        }"
                    )
                    writer.println("Thread: ${Thread.currentThread().name}")
                    writer.println("\n--- Stack Trace ---")
                    writer.println(sw.toString())
                    writer.println("\n--- Device Info ---")
                    // Add device info if needed (Build.MODEL, Build.VERSION.SDK_INT etc.)
                }
            }
            // Clean up old logs
            cleanupOldLogs(logDir)
        } catch (e: Exception) {
            // Failed to log crash, maybe log to Logcat
            Log.e("CrashLogManager", "Failed to write crash log", e)
        }
    }

    fun getCrashLogs(context: Context): List<File> {
        val logDir = File(context.filesDir, CRASH_LOG_DIR)
        if (!logDir.exists() || !logDir.isDirectory) {
            return emptyList()
        }
        return logDir.listFiles { file -> file.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun clearCrashLogs(context: Context): Boolean {
        val logDir = File(context.filesDir, CRASH_LOG_DIR)
        if (logDir.exists()) {
            return logDir.deleteRecursively()
        }

        return false
    }


    private fun cleanupOldLogs(logDir: File) {
        val logFiles = logDir.listFiles { file -> file.name.endsWith(".log") }
            ?.sortedBy { it.lastModified() } // Sort oldest first
            ?: return

        if (logFiles.size > MAX_LOG_FILES) {
            logFiles.take(logFiles.size - MAX_LOG_FILES).forEach { it.delete() }
        }
    }
}