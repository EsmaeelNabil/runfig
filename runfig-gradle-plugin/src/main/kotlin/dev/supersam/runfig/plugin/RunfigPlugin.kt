package dev.supersam.runfig.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized
import java.io.File

/**
 * Gradle plugin that transforms Android BuildConfig files to enable dynamic runtime configuration.
 *
 * This plugin replaces static final values in BuildConfig.java with calls to RunfigCache.get(),
 * allowing these values to be modified at runtime through SharedPreferences without recompilation.
 *
 * ## Requirements
 * - Android Gradle Plugin 7.0+ (uses modern Variant API)
 * - Gradle 7.0+
 * - Kotlin/Java projects with Android BuildConfig generation enabled
 *
 * ## Supported Project Types
 * - Android applications (`com.android.application`)
 * - Android libraries (`com.android.library`)
 * - Kotlin Multiplatform projects with Android targets
 *
 * ## Supported Field Types
 * - Boolean, Int, Long, Float, String
 *
 * ## Configuration Example
 * ```kotlin
 * runfig {
 *     variants("debug", "staging")
 *     excludeFields("VERSION_CODE", "BUILD_TYPE")
 *     preferencesName = "my_prefs"
 *     fieldPrefix = "app_"
 * }
 * ```
 *
 * @since 1.0.0 Uses modern Android Gradle Plugin Variant API (no BaseVariant deprecation warnings)
 */
class RunfigPlugin : Plugin<Project> {
    
    private companion object {
        // Precompiled regex patterns for field matching
        val FIELD_PATTERNS = mapOf(
            "Boolean" to "public static final boolean (\\w+) = (true|false);".toRegex(),
            "Int" to "public static final int (\\w+) = (\\d+);".toRegex(),
            "Long" to "public static final long (\\w+) = (\\d+L);".toRegex(),
            "Float" to "public static final float (\\w+) = (\\d+\\.\\d+f);".toRegex(),
            "String" to "public static final String (\\w+) = \"([^\"]*)\";".toRegex()
        )
        
        const val RUNFIG_CACHE_CLASS = "dev.supersam.runfig.android.RunfigCache"
        const val DEFAULT_PREFERENCES_NAME = "runfig_prefs"
    }

    override fun apply(project: Project) {
        project.logger.lifecycle("Runfig plugin applied to project ${project.name}")
        
        val extension = project.extensions.create("runfig", RunfigExtension::class.java)
        
        // Set up Android tasks immediately (modern Variant API must be called during plugin apply)
        setupAndroidTasks(project, extension)
        
        // Validate dependencies and setup fallback after evaluation
        project.afterEvaluate {
            if (!validateDependencies(project)) {
                // If validation fails, log warning but tasks are already configured
                project.logger.warn("   Note: Transform tasks have been created but will not execute correctly without the dependency.")
            }
            setupFallbackTaskAfterEvaluation(project, extension)
        }
    }

    /**
     * Validates that the required runfig-android dependency is present.
     * 
     * @param project The Gradle project to validate
     * @return true if dependencies are valid, false if missing required dependencies
     */
    private fun validateDependencies(project: Project): Boolean {
        val configurations = project.configurations
        val requiredDependency = "dev.supersam.runfig:runfig-android"
        val runfigAndroidProjectName = "runfig-android"
        
        // Check all configurations for the runfig-android dependency (external or project)
        val hasRunfigAndroid = configurations.any { config ->
            config.dependencies.any { dependency ->
                when {
                    // Check for external dependency
                    "${dependency.group}:${dependency.name}" == requiredDependency -> true
                    // Check for project dependency  
                    dependency.name == runfigAndroidProjectName -> true
                    // Check for project dependency with full path
                    dependency.name.endsWith(":$runfigAndroidProjectName") -> true
                    else -> false
                }
            }
        }
        
        if (!hasRunfigAndroid) {
            project.logger.warn("⚠️  Runfig plugin is applied but '$requiredDependency' dependency is missing!")
            project.logger.warn("   The transformed BuildConfig files require RunfigCache class from runfig-android library.")
            project.logger.warn("   Add this to your dependencies:")
            project.logger.warn("   implementation(\"$requiredDependency:VERSION\") // For external dependency")
            project.logger.warn("   OR")
            project.logger.warn("   implementation(project(\":runfig-android\"))  // For project dependency")
            project.logger.warn("   Skipping Runfig transformations until dependency is added.")
            return false
        }
        
        project.logger.info("✅ Runfig dependency validation passed - runfig-android library found")
        return true
    }

    /**
     * Sets up transformation tasks for Android application and library projects using modern Variant API.
     */
    private fun setupAndroidTasks(project: Project, extension: RunfigExtension) {
        project.plugins.withId("com.android.application") {
            val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            androidComponents.onVariants { variant ->
                val variantName = variant.name
                val buildTypeName = variant.buildType ?: "unknown"
                if (shouldTransformVariant(variantName, buildTypeName, extension)) {
                    createTransformTask(project, variantName, buildTypeName, extension)
                }
            }
        }

        project.plugins.withId("com.android.library") {
            val androidComponents = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
            androidComponents.onVariants { variant ->
                val variantName = variant.name
                val buildTypeName = variant.buildType ?: "unknown"
                if (shouldTransformVariant(variantName, buildTypeName, extension)) {
                    createTransformTask(project, variantName, buildTypeName, extension)
                }
            }
        }
    }

    /**
     * Determines whether a build variant should be transformed.
     *
     * @param variantName The name of the build variant
     * @param buildTypeName The build type name
     * @param extension The plugin configuration
     * @return true if the variant should be transformed
     */
    private fun shouldTransformVariant(variantName: String, buildTypeName: String, extension: RunfigExtension): Boolean {
        return if (extension.variantNames.isEmpty()) {
            // Default: only debug variants (assuming debug builds are debuggable)
            buildTypeName.equals("debug", ignoreCase = true)
        } else {
            extension.variantNames.any { it.equals(variantName, ignoreCase = true) }
        }
    }

    /**
     * Creates a Gradle task to transform BuildConfig files for a specific variant.
     */
    private fun createTransformTask(project: Project, variantName: String, buildTypeName: String, extension: RunfigExtension) {
        val capitalizedVariantName = variantName.capitalized()
        val taskName = "transform${capitalizedVariantName}BuildConfig"
        val buildConfigDir = project.layout.buildDirectory.dir("generated/source/buildConfig/${variantName.lowercase()}")

        project.tasks.register(taskName) { task ->
            task.group = "runfig"
            task.description = "Transforms BuildConfig files for the $variantName variant"
            task.dependsOn("generate${capitalizedVariantName}BuildConfig")

            // Configure incremental build support
            configureTaskInputsOutputs(task, buildConfigDir, extension)

            task.doLast {
                // Double-check dependency at runtime
                if (validateDependencies(project)) {
                    transformBuildConfigFiles(project, buildConfigDir.get().asFile, extension)
                } else {
                    project.logger.warn("Skipping BuildConfig transformation for $variantName - runfig-android dependency missing")
                }
            }
        }

        // Ensure compilation tasks depend on our transform
        setupTaskDependencies(project, taskName, capitalizedVariantName, variantName)
    }

    /**
     * Configures task inputs and outputs for proper incremental build support.
     */
    private fun configureTaskInputsOutputs(task: org.gradle.api.Task, buildConfigDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>, extension: RunfigExtension) {
        task.inputs.dir(buildConfigDir)
            .withPropertyName("buildConfigDirectory")
            .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
        
        // Track configuration properties as inputs
        task.inputs.property("variantNames", extension.variantNames)
        task.inputs.property("excludedFields", extension.excludedFields)
        task.inputs.property("preferencesName", extension.preferencesName)
        task.inputs.property("fieldPrefix", extension.fieldPrefix)
        
        task.outputs.dir(buildConfigDir)
            .withPropertyName("transformedBuildConfigDirectory")
    }

    /**
     * Sets up dependencies to ensure compilation tasks run after our transform task.
     */
    private fun setupTaskDependencies(project: Project, taskName: String, variantName: String, originalVariantName: String) {
        project.afterEvaluate {
            val compilationTaskNames = listOf(
                "compile${variantName}JavaWithJavac",
                "compile${variantName}Kotlin",
                "compile${variantName}KotlinAndroid",
                "compile${originalVariantName}KotlinAndroid",
                "compile${variantName}Sources",
                "compile${variantName}"
            )
            
            compilationTaskNames.forEach { compilationTaskName ->
                project.tasks.findByName(compilationTaskName)?.dependsOn(taskName)
            }
        }
    }

    /**
     * Creates a fallback task for projects that might not have standard Android variant tasks.
     */
     private fun setupFallbackTaskAfterEvaluation(project: Project, extension: RunfigExtension) {
        logConfiguration(project, extension)
        validateConfiguration(project, extension)
        
        if (hasAndroidComponent(project)) {
            createFallbackTask(project, extension)
        }
    }

    /**
     * Logs the current plugin configuration.
     */
    private fun logConfiguration(project: Project, extension: RunfigExtension) {
        if (extension.variantNames.isEmpty()) {
            project.logger.lifecycle("Runfig will transform all debug variants (default behavior)")
        } else {
            project.logger.lifecycle("Runfig will transform these variants: ${extension.variantNames.joinToString()}")
        }
    }

    /**
     * Validates the plugin configuration.
     * 
     * Note: With modern Variant API, detailed validation is simplified as the variants
     * are processed directly when they're created by the Android Gradle Plugin.
     */
    private fun validateConfiguration(project: Project, extension: RunfigExtension) {
        if (extension.variantNames.isNotEmpty()) {
            project.logger.info("Runfig configured for specific variants: ${extension.variantNames.joinToString()}")
            project.logger.info("Note: Unknown variant names will be ignored during processing")
        }
    }

    /**
     * Checks if the project has Android components.
     */
    private fun hasAndroidComponent(project: Project): Boolean {
        return project.plugins.hasPlugin("com.android.application") ||
               project.plugins.hasPlugin("com.android.library") ||
               project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
    }

    /**
     * Creates a fallback task for Kotlin Multiplatform and edge cases.
     */
    private fun createFallbackTask(project: Project, extension: RunfigExtension) {
        val taskName = "transformAllBuildConfigs"
        
        if (project.tasks.names.contains(taskName)) return
        
        project.tasks.register(taskName) { task ->
            task.group = "runfig"
            task.description = "Transforms BuildConfig files for all eligible variants (fallback)"
            
            val buildDir = project.layout.buildDirectory.dir("generated/source/buildConfig")
            configureTaskInputsOutputs(task, buildDir, extension)
            
            task.doLast {
                // Double-check dependency at runtime
                if (validateDependencies(project)) {
                    transformAllBuildConfigs(project, extension)
                } else {
                    project.logger.warn("Skipping BuildConfig transformation (fallback) - runfig-android dependency missing")
                }
            }
        }
        
        // Only activate fallback if no variant-specific tasks exist
        project.afterEvaluate {
            if (!hasVariantSpecificTasks(project, taskName)) {
                project.tasks.matching { 
                    it.name.matches(Regex(".*generate.*BuildConfig.*")) 
                }.all { it.finalizedBy(taskName) }
            }
        }
    }

    /**
     * Checks if variant-specific transform tasks exist.
     */
    private fun hasVariantSpecificTasks(project: Project, excludeTaskName: String): Boolean {
        return project.tasks.names.any { taskName ->
            taskName.matches(Regex("transform.*BuildConfig")) && taskName != excludeTaskName
        }
    }

    /**
     * Transforms all BuildConfig files in the fallback task.
     */
    private fun transformAllBuildConfigs(project: Project, extension: RunfigExtension) {
        val buildDirPath = project.layout.buildDirectory.get().asFile.toString()
        val patterns = if (extension.variantNames.isEmpty()) {
            listOf("**/debug/**/BuildConfig.java")
        } else {
            extension.variantNames.map { "**/${it.lowercase()}/**/BuildConfig.java" }
        }
        
        project.fileTree(buildDirPath).apply {
            include(patterns)
        }.forEach { file ->
            transformSingleBuildConfigFile(project, file, extension)
        }
    }

    /**
     * Transforms all BuildConfig files in a directory.
     */
    private fun transformBuildConfigFiles(project: Project, searchDir: File, extension: RunfigExtension) {
        try {
            project.fileTree(searchDir).apply {
                include("**/BuildConfig.java")
            }.forEach { file ->
                transformSingleBuildConfigFile(project, file, extension)
            }
        } catch (e: Exception) {
            project.logger.error("Error processing directory ${searchDir.absolutePath}: ${e.message}")
        }
    }

    /**
     * Transforms a single BuildConfig file.
     *
     * @param project The Gradle project
     * @param file The BuildConfig.java file to transform
     * @param extension The plugin configuration
     */
    private fun transformSingleBuildConfigFile(project: Project, file: File, extension: RunfigExtension) {
        try {
            val originalContent = file.readText()
            project.logger.info("Processing BuildConfig: ${file.path}")

            val transformedContent = transformContent(originalContent, extension, project)
            val transformCount = countTransformations(originalContent, transformedContent)

            if (transformCount > 0) {
                file.writeText(transformedContent)
                logTransformation(project, transformCount, file.path, extension)
            } else {
                project.logger.info("No fields transformed in: ${file.path}")
            }
        } catch (e: Exception) {
            project.logger.error("Failed to transform ${file.path}: ${e.message}")
        }
    }

    /**
     * Transforms the content of a BuildConfig file.
     */
    private fun transformContent(content: String, extension: RunfigExtension, project: Project): String {
        var transformed = content

        FIELD_PATTERNS.forEach { (type, pattern) ->
            transformed = transformed.replace(pattern) { match ->
                val fieldName = match.groupValues[1]
                
                if (extension.isFieldExcluded(fieldName)) {
                    project.logger.info("Skipping excluded field: $fieldName")
                    match.value
                } else {
                    generateTransformedField(type, fieldName, match.groupValues, extension)
                }
            }
        }

        return transformed
    }

    /**
     * Generates a transformed field declaration.
     */
    private fun generateTransformedField(type: String, fieldName: String, matchValues: List<String>, extension: RunfigExtension): String {
        val prefKey = extension.getPreferenceKey(fieldName)
        val prefsName = extension.preferencesName
        val defaultValue = if (type == "String") "\"${matchValues[2]}\"" else matchValues[2]
        
        // Map type names for proper Java field declarations and generic types
        val javaType = when (type) {
            "Boolean" -> "boolean"
            "Int" -> "int"
            "Long" -> "long"
            "Float" -> "float"
            "String" -> "String"
            else -> type.lowercase()
        }
        
        val genericType = when (type) {
            "Int" -> "Integer"
            else -> type
        }
        
        return "public static final $javaType $fieldName = $RUNFIG_CACHE_CLASS.<$genericType>get(\"$prefsName\", \"$prefKey\", $defaultValue);"
    }

    /**
     * Counts the number of transformations made.
     */
    private fun countTransformations(original: String, transformed: String): Int {
        return FIELD_PATTERNS.values.sumOf { pattern ->
            pattern.findAll(original).count() - pattern.findAll(transformed).count()
        }
    }

    /**
     * Logs transformation details.
     */
    private fun logTransformation(project: Project, count: Int, filePath: String, extension: RunfigExtension) {
        project.logger.info("Transformed $count fields in: $filePath")
        
        if (extension.excludedFields.isNotEmpty()) {
            project.logger.info("Excluded fields: ${extension.excludedFields.joinToString()}")
        }
        if (extension.fieldPrefix.isNotEmpty()) {
            project.logger.info("Field prefix: '${extension.fieldPrefix}'")
        }
        if (extension.preferencesName != DEFAULT_PREFERENCES_NAME) {
            project.logger.info("Custom preferences file: '${extension.preferencesName}'")
        }
    }
}