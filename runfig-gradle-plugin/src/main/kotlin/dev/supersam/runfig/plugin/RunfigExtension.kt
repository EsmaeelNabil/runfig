/**
 * Extension for configuring the Runfig Gradle plugin.
 *
 * This extension allows users to configure which Android build variants should be processed
 * by the Runfig plugin. By default, Runfig transforms BuildConfig files for all debug variants,
 * but this can be customized to target specific variants.
 *
 * Example usage in build.gradle.kts:
 * ```kotlin
 * runfig {
 *     // Option 1: Add variants one by one
 *     variantNames.add("productionDebug")
 *     
 *     // Option 2: Use the helper function to add multiple at once
 *     variants("stagingDebug", "uatDebug")
 *     
 *     // Option 3: Exclude specific fields from transformation
 *     excludeFields("SENSITIVE_API_KEY", "BUILD_TYPE")
 *     
 *     // Option 4: Use custom SharedPreferences file name
 *     preferencesName = "my_app_runfig_prefs"
 *     
 *     // Option 5: Add prefix to transformed field keys
 *     fieldPrefix = "RUNFIG_"
 * }
 * ```
 */
package dev.supersam.runfig.plugin

/**
 * Configuration extension for the Runfig plugin.
 */
open class RunfigExtension {
    /**
     * Specifies which variant names should have Runfig transformations applied.
     * If empty, all debug variants will be transformed by default.
     *
     * This list is case-insensitive when matching variant names.
     */
    var variantNames: MutableList<String> = mutableListOf()

    /**
     * List of BuildConfig field names that should be excluded from Runfig transformation.
     * These fields will remain as static final values and cannot be changed at runtime.
     * 
     * This is useful for sensitive values, version codes, or fields that should not be 
     * modifiable during testing.
     */
    var excludedFields: MutableList<String> = mutableListOf()

    /**
     * Custom name for the SharedPreferences file used to store runtime configuration values.
     * Defaults to "runfig_prefs".
     * 
     * This allows multiple apps or modules to use separate preference files.
     */
    var preferencesName: String = "runfig_prefs"

    /**
     * Optional prefix to add to all transformed field keys when storing in SharedPreferences.
     * This helps avoid conflicts with other preference keys and makes Runfig keys easily identifiable.
     * 
     * Example: If fieldPrefix = "RF_" and you have a field "API_URL", 
     * it will be stored as "RF_API_URL" in SharedPreferences.
     */
    var fieldPrefix: String = ""

    /**
     * Helper function to make configuration more readable by adding multiple variant names.
     * 
     * @param names The variant names to be processed by Runfig
     */
    fun variants(vararg names: String) {
        variantNames.addAll(names)
    }

    /**
     * Helper function to exclude multiple fields from Runfig transformation.
     * 
     * @param fieldNames The BuildConfig field names to exclude from transformation
     */
    fun excludeFields(vararg fieldNames: String) {
        excludedFields.addAll(fieldNames)
    }

    /**
     * Checks if a specific field should be excluded from transformation.
     * 
     * @param fieldName The BuildConfig field name to check
     * @return true if the field should be excluded, false otherwise
     */
    internal fun isFieldExcluded(fieldName: String): Boolean {
        return excludedFields.any { it.equals(fieldName, ignoreCase = true) }
    }

    /**
     * Gets the key that will be used to store a field in SharedPreferences.
     * This applies the configured fieldPrefix if set.
     * 
     * @param fieldName The original BuildConfig field name
     * @return The key to use in SharedPreferences
     */
    internal fun getPreferenceKey(fieldName: String): String {
        return fieldPrefix + fieldName
    }
}