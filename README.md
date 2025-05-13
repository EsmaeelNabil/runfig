# Runfig 

Runtime Configurations control for Android applications during development and testing.

#### Usage (Library + Gradle Plugin)

- apply the plugin in your `build.gradle.kts` file

```gradle
    plugins {
        alias("dev.supersam.runfig.gradle") version("0.0.2")
    }


    runfig {
        variants.add("StagingDebug")  
    }
```

- add the library to your `build.gradle` file

```gradle
    dependencies {
        implementation("dev.supersam.runfig:runfig-android:0.0.2")
    }
```

#### Run the app Press and Press and hold anywhere in the app for 2+ seconds to inject the overlay window
