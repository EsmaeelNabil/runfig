package dev.supersam.runfig.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized

class RunfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Apply for standard Android projects
        project.plugins.withId("com.android.application") {
            val android = project.extensions.getByType(AppExtension::class.java)
            setupVariantTasks(project, android)
        }

        project.plugins.withId("com.android.library") {
            val android = project.extensions.getByType(LibraryExtension::class.java)
            setupVariantTasks(project, android)
        }

        // Create a fallback task for KMP projects
        project.afterEvaluate {
            // Only create the fallback if we have an Android component
            if (project.plugins.hasPlugin("com.android.application") ||
                project.plugins.hasPlugin("com.android.library") ||
                project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {

                createFallbackTask(project)
            }
        }
    }

    private fun setupVariantTasks(project: Project, androidExtension: Any) {
        when (androidExtension) {
            is AppExtension -> {
                androidExtension.applicationVariants.all { variant ->
                    createTransformTask(project, variant)
                }
            }
            is LibraryExtension -> {
                androidExtension.libraryVariants.all { variant ->
                    createTransformTask(project, variant)
                }
            }
        }
    }

    private fun createTransformTask(project: Project, variant: BaseVariant) {
        val variantName = variant.name.capitalized()
        val taskName = "transform${variantName}BuildConfig"

        val generateBuildConfigTask = project.tasks.named("generate${variantName}BuildConfig")

        project.tasks.register(taskName) { task ->
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

    private fun createFallbackTask(project: Project) {
        // Create a catch-all task that runs after all BuildConfig generation
        val taskName = "transformAllBuildConfigs"

        if (!project.tasks.names.contains(taskName)) {
            project.tasks.register(taskName) { task ->
                task.doLast {
                    transformBuildConfigFiles(project, project.layout.buildDirectory.get().asFile.toString())
                }
            }

            // Hook it into the build process
            project.tasks.matching { it.name.matches(Regex(".*generate.*BuildConfig.*")) }.all { generateTask ->
                generateTask.finalizedBy(taskName)
            }
        }
    }

    private fun transformBuildConfigFiles(project: Project, searchDir: String) {
        project.fileTree(searchDir).apply {
            include("**/BuildConfig.java")
        }.forEach { file ->
            val content = file.readText()
            project.logger.info("Found BuildConfig at ${file.path}")

            // Transform boolean fields
            var transformed = content.replace(
                "public static final boolean (\\w+) = (true|false);".toRegex(),
                "public static final boolean $1 = dev.supersam.runfig.android.RunfigCache.<Boolean>get(\"$1\", $2);"
            )

            // Transform int fields
            transformed = transformed.replace(
                "public static final int (\\w+) = (\\d+);".toRegex(),
                "public static final int $1 = dev.supersam.runfig.android.RunfigCache.<Integer>get(\"$1\", $2);"
            )

            // Transform long fields
            transformed = transformed.replace(
                "public static final long (\\w+) = (\\d+L);".toRegex(),
                "public static final long $1 = dev.supersam.runfig.android.RunfigCache.<Long>get(\"$1\", $2);"
            )

            // Transform float fields
            transformed = transformed.replace(
                "public static final float (\\w+) = (\\d+\\.\\d+f);".toRegex(),
                "public static final float $1 = dev.supersam.runfig.android.RunfigCache.<Float>get(\"$1\", $2);"
            )

            // Transform string fields
            transformed = transformed.replace(
                "public static final String (\\w+) = \"([^\"]*)\";".toRegex(),
                "public static final String $1 = dev.supersam.runfig.android.RunfigCache.<String>get(\"$1\", \"$2\");"
            )

            file.writeText(transformed)
            project.logger.info("Transformed BuildConfig in ${file.path}")
        }
    }
}