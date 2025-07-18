import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.mavenPublish)
}

android {
    namespace = "dev.supersam.runfig.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", "src/main/kotlin")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Startup Initializer
    implementation(libs.androidx.startup.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}

mavenPublishing {

    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true,
        )
    )

    coordinates(
        groupId = "dev.supersam.runfig",
        artifactId = "runfig-android",
        version = "0.0.5"
    )

    pom {
        name.set("Runfig Android")
        description.set("A library that allows you to override BuildConfig values at runtime.")
        inceptionYear.set("2025")
        url.set("https://github.com/esmaeelnabil/runfig")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("esmaeelnabil")
                name.set("Esmaeel Moustafa")
                email.set("esmaeel.nabil.m@gmail.com")
                url.set("https://supersam.dev")
            }
        }

        scm {
            url.set("https://github.com/esmaeelnabil/runfig")
            connection.set("scm:git:git://github.com/esmaeelnabil/runfig.git")
            developerConnection.set("scm:git:git@github.com:esmaeelnabil/runfig.git")
        }
    }

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}