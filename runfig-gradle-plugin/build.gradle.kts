import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(deps.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(deps.plugins.mavenPublish)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(deps.gradle)
    testImplementation(kotlin("test"))
    testImplementation(deps.junit)
}

gradlePlugin {
    plugins {
        create("RunfigPlugin") {
            id = "dev.supersam.runfig.gradle"
            implementationClass = "dev.supersam.runfig.plugin.RunfigPlugin"
            displayName = "Runfig Gradle Plugin"
            description = "A plugin that transforms BuildConfig to allow runtime value overrides"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

// Configure Java compilation options
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

// Configure Kotlin compilation options
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

mavenPublishing {
    configure(GradlePlugin(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))

    coordinates(
        groupId = "dev.supersam.runfig",
        artifactId = "runfig-gradle-plugin",
        version = "0.0.5"
    )

    pom {
        name.set("Runfig Gradle Plugin")
        description.set("Replace build config fields with shared preference values to be able to change them at runtime.")
        url.set("https://github.com/esmaeelnabil/runfig")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
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