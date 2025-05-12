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
     * Helper function to make configuration more readable by adding multiple variant names.
     * 
     * @param names The variant names to be processed by Runfig
     */
    fun variants(vararg names: String) {
        variantNames.addAll(names)
    }
}