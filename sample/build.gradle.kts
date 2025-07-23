plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dev.supersam.runfig.gradle")
}

// Configure Runfig for this sample
runfig {
    // Apply to debug variant
    variants("debug")
    excludeFields("VERSION_CODE", "BUILD_TYPE") // Added BUILD_TYPE
}

android {
    namespace = "dev.supersam.runfig.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.supersam.runfig.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false

            // Add test BuildConfig fields
            buildConfigField("String", "API_BASE_URL", "\"https://api.staging.example.com\"")
            buildConfigField("int", "NETWORK_TIMEOUT", "30000")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
            buildConfigField("String", "APP_THEME", "\"light\"")
            buildConfigField("float", "ANIMATION_SPEED", "1.0f")
        }
        release {
            isMinifyEnabled = true
            buildConfigField("String", "API_BASE_URL", "\"https://api.prod.example.com\"")
            buildConfigField("int", "NETWORK_TIMEOUT", "10000")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
            buildConfigField("String", "APP_THEME", "\"light\"")
            buildConfigField("float", "ANIMATION_SPEED", "1.0f")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlin {
        jvmToolchain(11)
        compilerOptions {
            freeCompilerArgs.addAll(
                listOf(
                    "-Xskip-metadata-version-check",
                    "-Xallow-incompatible-classifications"
                )
            )
        }
    }
}

dependencies {
    // Use the local modules for development
    implementation(project(":runfig-android"))

    // Android basics
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
}

