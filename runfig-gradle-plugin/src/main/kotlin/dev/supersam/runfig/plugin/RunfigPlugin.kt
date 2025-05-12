/**
 * Runfig Gradle Plugin for Android - Dynamic BuildConfig Configuration
 *
 * This plugin transforms the generated BuildConfig.java files in Android projects
 * to make BuildConfig values dynamically configurable at runtime. It replaces static 
 * final values with calls to RunfigCache, which allows these values to be overridden 
 * dynamically without requiring recompilation.
 *
 * The plugin supports:
 * - Android applications (com.android.application)
 * - Android libraries (com.android.library)
 * - Kotlin Multiplatform projects with Android targets
 *
 * Supported field types:
 * - Boolean values
 * - Integer values
 * - Long values
 * - Float values
 * - String values
 *
 * By default, the plugin only transforms debug variants, but can be configured
 * to target specific variants through the 'runfig' extension.
 *
 * Example usage in build.gradle.kts:
 * ```kotlin
 * plugins {
 *     id("com.android.application")
 *     id("dev.supersam.runfig")
 * }
 *
 * runfig {
 *     variants("productionDebug", "stagingDebug")
 * }
 * ```
 */
package dev.supersam.runfig.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized
import java.io.File

/**
 * Gradle plugin that transforms Android BuildConfig files to enable dynamic runtime configuration.
 *
 * This plugin hooks into the Android build process to transform generated BuildConfig.java files,
 * replacing static final values with calls to RunfigCache.get() that allow runtime overrides.
 */
class RunfigPlugin : Plugin<Project> {
    // Precompiled regex patterns for better performance
    private val booleanPattern = "public static final boolean (\\w+) = (true|false);".toRegex()
    private val intPattern = "public static final int (\\w+) = (\\d+);".toRegex()
    private val longPattern = "public static final long (\\w+) = (\\d+L);".toRegex()
    private val floatPattern = "public static final float (\\w+) = (\\d+\\.\\d+f);".toRegex()
    private val stringPattern = "public static final String (\\w+) = \"([^\"]*)\";".toRegex()

    /**
     * Applies the plugin to the specified project.
     *
     * This method:
     * 1. Creates and registers the 'runfig' extension
     * 2. Sets up tasks for transforming BuildConfig in Android app and library projects
     * 3. Creates a fallback task for Kotlin Multiplatform projects
     * 4. Validates configuration after project evaluation
     *
     * @param project The Gradle project to apply the plugin to
     */
    override fun apply(project: Project) {
        project.logger.lifecycle("Runfig plugin applied to project ${project.name}")

        val extension = project.extensions.create("runfig", RunfigExtension::class.java)

        // Apply for standard Android projects
        project.plugins.withId("com.android.application") {
            val android = project.extensions.getByType(AppExtension::class.java)
            setupVariantTasks(project, android, extension)
        }

        project.plugins.withId("com.android.library") {
            val android = project.extensions.getByType(LibraryExtension::class.java)
            setupVariantTasks(project, android, extension)
        }

        // Create a fallback task for KMP projects
        project.afterEvaluate {
            // Log configuration information
            if (extension.variantNames.isEmpty()) {
                project.logger.lifecycle("Runfig will transform all debug variants (default behavior)")
            } else {
                project.logger.lifecycle("Runfig will transform these variants: ${extension.variantNames.joinToString()}")
            }

            // Validate variant names if specified
            if (extension.variantNames.isNotEmpty()) {
                validateVariantNames(project, extension)
            }

            // Only create the fallback if we have an Android component
            if (project.plugins.hasPlugin("com.android.application") ||
                project.plugins.hasPlugin("com.android.library") ||
                project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {

                createFallbackTask(project, extension)
            }
        }
    }

    /**
     * Determines if Runfig should be applied to the given variant.
     * 
     * The decision logic is:
     * - If no variants are specified in the extension, apply to all debug variants (isDebuggable)
     * - If variants are specified, apply only to those exact variants (case-insensitive matching)
     *
     * @param variant The Android build variant to check
     * @param extension The Runfig extension with configuration
     * @return true if Runfig should transform this variant's BuildConfig, false otherwise
     */
    private fun shouldApplyToVariant(variant: BaseVariant, extension: RunfigExtension): Boolean {
        return if (extension.variantNames.isEmpty()) {
            // Default behavior: apply to all debug variants, prioritizing isDebuggable flag
            variant.buildType.isDebuggable
        } else {
            // Apply only to specified variants (case-insensitive comparison)
            extension.variantNames.map { it.lowercase() }.contains(variant.name.lowercase())
        }
    }

    /**
     * Validates that the specified variant names actually exist in the project.
     * 
     * This method logs warnings for any variant names specified in the runfig extension
     * that don't match actual build variants in the project.
     *
     * @param project The Gradle project being built
     * @param extension The Runfig extension with configuration
     */
    private fun validateVariantNames(project: Project, extension: RunfigExtension) {
        val availableVariants = mutableListOf<String>()
        
        // Collect available variant names
        project.plugins.withId("com.android.application") {
            val android = project.extensions.getByType(AppExtension::class.java)
            android.applicationVariants.all { variant ->
                availableVariants.add(variant.name)
            }
        }
        
        project.plugins.withId("com.android.library") {
            val android = project.extensions.getByType(LibraryExtension::class.java)
            android.libraryVariants.all { variant ->
                availableVariants.add(variant.name)
            }
        }
        
        // Check for unknown variants
        val unknownVariants = extension.variantNames.filter { variantName -> 
            !availableVariants.any { it.equals(variantName, ignoreCase = true) }
        }
        
        if (unknownVariants.isNotEmpty() && availableVariants.isNotEmpty()) {
            project.logger.warn("Runfig: Unknown variant names specified: ${unknownVariants.joinToString()}")
            project.logger.warn("Available variants are: ${availableVariants.joinToString()}")
        }
    }
    /**
     * Sets up the transformation tasks for each eligible Android variant.
     * 
     * This method configures the variant-specific tasks for both 
     * application and library projects.
     *
     * @param project The Gradle project being built
     * @param androidExtension The Android extension (either AppExtension or LibraryExtension)
     * @param extension The Runfig extension with configuration
     */
    private fun setupVariantTasks(project: Project, androidExtension: Any, extension: RunfigExtension) {
        when (androidExtension) {
            is AppExtension -> {
                androidExtension.applicationVariants.all { variant ->
                    if (shouldApplyToVariant(variant, extension)) {
                        createTransformTask(project, variant)
                    }
                }
            }
            is LibraryExtension -> {
                androidExtension.libraryVariants.all { variant ->
                    if (shouldApplyToVariant(variant, extension)) {
                        createTransformTask(project, variant)
                    }
                }
            }
        }
    }

    /**
     * Creates a Gradle task to transform the BuildConfig files for a specific variant.
     * 
     * The task is configured to run after the variant's BuildConfig generation task
     * and before the compile tasks.
     *
     * @param project The Gradle project being built
     * @param variant The specific Android build variant to process
     */
    private fun createTransformTask(project: Project, variant: BaseVariant) {
        val variantName = variant.name.capitalized()
        val taskName = "transform${variantName}BuildConfig"

        val generateBuildConfigTask = project.tasks.named("generate${variantName}BuildConfig")

        project.tasks.register(taskName) { task ->
            task.group = "runfig"
            task.description = "Transforms BuildConfig files for the ${variant.name} variant"
            task.dependsOn(generateBuildConfigTask)

            val buildConfigDir = "${project.layout.buildDirectory.get().asFile}/generated/source/buildConfig/${variant.name.lowercase()}"

            task.doLast {
                transformBuildConfigFiles(project, buildConfigDir)
            }
        }

        // Set up task dependencies
        project.tasks.findByName("compile${variantName}JavaWithJavac")?.dependsOn(taskName)
        project.tasks.findByName("compile${variant.name}KotlinAndroid")?.dependsOn(taskName)
        project.tasks.findByName("compile${variantName}")?.dependsOn(taskName)
    }

    /**
     * Creates a fallback task to transform BuildConfig files across all variants.
     * 
     * This task is used as a safety net for Kotlin Multiplatform projects and 
     * runs after all BuildConfig generation tasks.
     *
     * @param project The Gradle project being built
     * @param extension The Runfig extension with configuration
     */
    private fun createFallbackTask(project: Project, extension: RunfigExtension) {
        // Create a catch-all task that runs after all BuildConfig generation
        val taskName = "transformAllBuildConfigs"

        if (!project.tasks.names.contains(taskName)) {
            project.tasks.register(taskName) { task ->
                task.group = "runfig"
                task.description = "Transforms BuildConfig files for all eligible variants"
                
                task.doLast {
                    val buildDir = project.layout.buildDirectory.get().asFile.toString()

                    if (extension.variantNames.isEmpty()) {
                        // Process only debug variant dirs by default
                        project.logger.info("Fallback task: processing debug variants")
                        project.fileTree(buildDir).apply {
                            include("**/debug/**/BuildConfig.java")
                        }.forEach { file ->
                            transformBuildConfigFile(project, file)
                        }
                    } else {
                        // Process specified variants with a single search for better performance
                        project.logger.info("Fallback task: processing specified variants: ${extension.variantNames.joinToString()}")
                        val patterns = extension.variantNames.map { 
                            "**/${it.lowercase()}/**/BuildConfig.java" 
                        }
                        
                        project.fileTree(buildDir).apply {
                            include(patterns)
                        }.forEach { file ->
                            transformBuildConfigFile(project, file)
                        }
                    }
                }
            }

            // Hook it into the build process
            project.tasks.matching { it.name.matches(Regex(".*generate.*BuildConfig.*")) }.all { generateTask ->
                generateTask.finalizedBy(taskName)
            }
        }
    }

    /**
     * Transforms all BuildConfig files in the specified directory.
     * 
     * This method recursively finds and processes all BuildConfig.java files
     * in the given directory tree.
     *
     * @param project The Gradle project being built
     * @param searchDir The directory to search for BuildConfig files
     */
    private fun transformBuildConfigFiles(project: Project, searchDir: String) {
        try {
            project.fileTree(searchDir).apply {
                include("**/BuildConfig.java")
            }.forEach { file ->
                transformBuildConfigFile(project, file)
            }
        } catch (e: Exception) {
            project.logger.error("Error processing directory $searchDir: ${e.message}")
        }
    }

    /**
     * Transforms a single BuildConfig file to use Runfig's dynamic configuration.
     * 
     * This method:
     * 1. Reads the content of the BuildConfig.java file
     * 2. Replaces static final declarations with calls to RunfigCache.get()
     * 3. Writes the modified content back to the file
     *
     * The transformation supports boolean, int, long, float, and String fields.
     *
     * @param project The Gradle project being built
     * @param file The BuildConfig.java file to transform
     */
    private fun transformBuildConfigFile(project: Project, file: File) {
        try {
            val content = file.readText()
            project.logger.info("Found BuildConfig at ${file.path}")

            // Transform boolean fields
            var transformed = content.replace(
                booleanPattern,
                "public static final boolean $1 = dev.supersam.runfig.android.RunfigCache.<Boolean>get(\"$1\", $2);"
            )

            // Transform int fields
            transformed = transformed.replace(
                intPattern,
                "public static final int $1 = dev.supersam.runfig.android.RunfigCache.<Integer>get(\"$1\", $2);"
            )

            // Transform long fields
            transformed = transformed.replace(
                longPattern,
                "public static final long $1 = dev.supersam.runfig.android.RunfigCache.<Long>get(\"$1\", $2);"
            )

            // Transform float fields
            transformed = transformed.replace(
                floatPattern,
                "public static final float $1 = dev.supersam.runfig.android.RunfigCache.<Float>get(\"$1\", $2);"
            )

            // Transform string fields
            transformed = transformed.replace(
                stringPattern,
                "public static final String $1 = dev.supersam.runfig.android.RunfigCache.<String>get(\"$1\", \"$2\");"
            )

            file.writeText(transformed)
            project.logger.info("Transformed BuildConfig in ${file.path}")
        } catch (e: Exception) {
            project.logger.error("Failed to transform BuildConfig in ${file.path}: ${e.message}")
            // Continue with other files instead of failing the build
        }
    }
}