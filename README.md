# Runfig 

Runtime Configurations control for Android applications during development and testing.



#### Publish Locally

```bash
./gradlew publishToMavenLocal
```


#### Use Locally (Library + Gradle Plugin)

- add mavenLocal() to your repositories in your `build.gradle` file

```gradle
    repositories {
        mavenLocal()
    }
```

- apply the plugin in your `build.gradle` file

```gradle
    plugins {
        id 'dev.supersam.runfig.gradle' version '0.0.1'
    }
```

- add the library to your `build.gradle` file

```gradle
    dependencies {
        implementation 'com.github.runfig:runfig-android:0.0.1'
    }
```

#### Run the app Press and Press and hold anywhere in the app for 2+ seconds to inject the overlay window


### For now, it will be applied to all build types, and you have to use the library if you applied the plugin as it depends of it.