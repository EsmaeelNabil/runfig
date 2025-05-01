import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.mavenPublish)
}

group = project.property("GROUP") as String
version = project.property("VERSION_NAME") as String


dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.gradle)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
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

mavenPublishing {
    configure(GradlePlugin(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))
    publishToMavenCentral(host = SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
}
