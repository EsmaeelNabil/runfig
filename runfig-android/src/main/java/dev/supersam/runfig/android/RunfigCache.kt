package dev.supersam.runfig.android

import android.content.SharedPreferences
import androidx.core.content.edit
import dev.supersam.runfig.android.util.getApplicationContext
import dev.supersam.runfig.android.util.getSharedPreferences

object RunfigCache {
    /**
     * Get a SharedPreferences instance with custom name.
     * 
     * @param preferencesName The name of the SharedPreferences file
     * @return SharedPreferences instance or null if context is unavailable
     */
    private fun getSharedPreferences(preferencesName: String): SharedPreferences? {
        return getApplicationContext()?.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
    }

    /**
     * Get all values from the default Runfig preferences file.
     * 
     * @return Map of all stored values or null if context is unavailable
     */
    fun getAll(): Map<String, *>? {
        return getSharedPreferences("runfig_prefs")?.all
    }

    /**
     * Get all values from a specific preferences file.
     * 
     * @param preferencesName The name of the SharedPreferences file
     * @return Map of all stored values or null if context is unavailable
     */
    fun getAll(preferencesName: String): Map<String, *>? {
        return getSharedPreferences(preferencesName)?.all
    }

    /**
     * Generic get method with custom preferences file name.
     * Used by the Gradle plugin for transformed BuildConfig fields.
     * 
     * @param preferencesName The name of the SharedPreferences file
     * @param key The preference key
     * @param defaultValue The default value to return if key doesn't exist
     * @return The stored value or defaultValue
     */
    @JvmStatic
    fun <T> get(preferencesName: String, key: String, defaultValue: T): T {
        return when (defaultValue) {
            is Boolean -> getBoolean(preferencesName, key, defaultValue) as T
            is Int -> getInt(preferencesName, key, defaultValue) as T
            is Long -> getLong(preferencesName, key, defaultValue) as T
            is Float -> getFloat(preferencesName, key, defaultValue) as T
            is String -> getString(preferencesName, key, defaultValue) as T
            else -> defaultValue
        }
    }

    @JvmStatic
    fun getBoolean(preferencesName: String, key: String, defaultValue: Boolean): Boolean {
        val prefs = getSharedPreferences(preferencesName) ?: return defaultValue
        if (prefs.contains(key)) return prefs.getBoolean(key, defaultValue)

        prefs.edit { putBoolean(key, defaultValue) }
        return defaultValue
    }

    @JvmStatic
    fun getInt(preferencesName: String, key: String, defaultValue: Int): Int {
        val prefs = getSharedPreferences(preferencesName) ?: return defaultValue
        if (prefs.contains(key)) return prefs.getInt(key, defaultValue)

        prefs.edit { putInt(key, defaultValue) }
        return defaultValue
    }

    @JvmStatic
    fun getLong(preferencesName: String, key: String, defaultValue: Long): Long {
        val prefs = getSharedPreferences(preferencesName) ?: return defaultValue
        if (prefs.contains(key)) return prefs.getLong(key, defaultValue)

        prefs.edit { putLong(key, defaultValue) }
        return defaultValue
    }

    @JvmStatic
    fun getFloat(preferencesName: String, key: String, defaultValue: Float): Float {
        val prefs = getSharedPreferences(preferencesName) ?: return defaultValue
        if (prefs.contains(key)) return prefs.getFloat(key, defaultValue)

        prefs.edit { putFloat(key, defaultValue) }
        return defaultValue
    }

    @JvmStatic
    fun getString(preferencesName: String, key: String, defaultValue: String): String {
        val prefs = getSharedPreferences(preferencesName) ?: return defaultValue
        if (prefs.contains(key)) return prefs.getString(key, defaultValue) ?: defaultValue

        prefs.edit { putString(key, defaultValue) }
        return defaultValue
    }

    fun putBoolean(preferencesName: String, key: String, value: Boolean) {
        getSharedPreferences(preferencesName)?.edit { putBoolean(key, value) }
    }

    fun putInt(preferencesName: String, key: String, value: Int) {
        getSharedPreferences(preferencesName)?.edit { putInt(key, value) }
    }

    fun putString(preferencesName: String, key: String, value: String) {
        getSharedPreferences(preferencesName)?.edit { putString(key, value) }
    }

    fun putLong(preferencesName: String, key: String, value: Long) {
        getSharedPreferences(preferencesName)?.edit { putLong(key, value) }
    }

    fun putFloat(preferencesName: String, key: String, value: Float) {
        getSharedPreferences(preferencesName)?.edit { putFloat(key, value) }
    }
}