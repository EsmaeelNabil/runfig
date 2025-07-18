package dev.supersam.runfig.android

import android.content.SharedPreferences
import androidx.core.content.edit
import dev.supersam.runfig.android.util.getApplicationContext
import dev.supersam.runfig.android.util.getSharedPreferences

object RunfigCache {
    private val sharedPreferences: SharedPreferences? get() = getApplicationContext()?.getSharedPreferences()

    fun getAll(): Map<String, *>? {
        return sharedPreferences?.all
    }

    @JvmStatic
    fun <T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is Boolean -> getBoolean(key, defaultValue) as T
            is Int -> getInt(key, defaultValue) as T
            is Long -> getLong(key, defaultValue) as T
            is Float -> getFloat(key, defaultValue) as T
            is String -> getString(key, defaultValue) as T
            else -> defaultValue
        }
    }

    @JvmStatic
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val prefs = sharedPreferences ?: return defaultValue
        if (prefs.contains(key)) return prefs.getBoolean(key, defaultValue)

        prefs.edit { putBoolean(key, defaultValue) }
        return defaultValue
    }

    @JvmStatic
    fun getInt(key: String, defaultValue: Int): Int {
        val prefs = sharedPreferences ?: return defaultValue
        if (prefs.contains(key)) return prefs.getInt(key, defaultValue)

        prefs.edit { putInt(key, defaultValue) }
        return defaultValue
    }

    @JvmStatic
    fun getLong(key: String, defaultValue: Long): Long {
        val prefs = sharedPreferences ?: return defaultValue
        if (prefs.contains(key)) return prefs.getLong(key, defaultValue)

        prefs.edit { putLong(key, defaultValue) }
        return defaultValue
    }

    @JvmStatic
    fun getFloat(key: String, defaultValue: Float): Float {
        val prefs = sharedPreferences ?: return defaultValue
        if (prefs.contains(key)) return prefs.getFloat(key, defaultValue)

        prefs.edit { putFloat(key, defaultValue) }
        return defaultValue
    }

    @JvmStatic
    fun getString(key: String, defaultValue: String): String {
        val prefs = sharedPreferences ?: return defaultValue
        if (prefs.contains(key)) return prefs.getString(key, defaultValue) ?: defaultValue

        prefs.edit { putString(key, defaultValue) }
        return defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences?.edit { putBoolean(key, value) }
    }

    fun putInt(key: String, value: Int) {
        sharedPreferences?.edit { putInt(key, value) }
    }

    fun putString(key: String, value: String) {
        sharedPreferences?.edit { putString(key, value) }
    }

    fun putLong(key: String, value: Long) {
        sharedPreferences?.edit { putLong(key, value) }
    }

    fun putFloat(key: String, value: Float) {
        sharedPreferences?.edit { putFloat(key, value) }
    }
}