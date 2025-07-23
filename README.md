# 🔧 Runfig

<div align="center">
  <img src="https://img.shields.io/badge/platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/AGP-7.0%2B-blue.svg" alt="AGP Version" />
  <img src="https://img.shields.io/badge/API-24%2B-orange.svg" alt="API Level" />
</div>

<div align="center">
  <h3>Dynamic BuildConfig Runtime Configuration for Android</h3>
  <p>Transform your Android BuildConfig values into runtime-configurable parameters without recompilation</p>
</div>

---

## ✨ What it does

- **Transform BuildConfig fields** from static values to runtime-configurable SharedPreferences
- **Debug overlay interface** for editing configuration values during development
- **Zero recompilation** - change values instantly without rebuilding your app
- **Type-safe** - supports Boolean, Int, Long, Float, and String fields

## 🚀 Quick Start

### 1. Add the plugin and library

```kotlin
// build.gradle.kts (app module)
plugins {
    id("com.android.application")
    id("dev.supersam.runfig.gradle") version "6.0.0"
}

dependencies {
    implementation("dev.supersam.runfig:runfig-android:6.0.0")
}

runfig {
    variants("debug")  // Transform debug builds only
}
```

### 2. Add BuildConfig fields

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://api.staging.example.com\"")
            buildConfigField("int", "NETWORK_TIMEOUT", "30000")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
        }
    }
}
```

### 3. Access debug interface

**Long press anywhere in your app for 2+ seconds** - the debug overlay will appear automatically.

## 📋 What happens

### Before transformation
```java
public static final String API_BASE_URL = "https://api.staging.example.com";
```

### After transformation
```java
public static final String API_BASE_URL = RunfigCache.<String>get("runfig_prefs", "API_BASE_URL", "https://api.staging.example.com");
```

Your BuildConfig fields now read from SharedPreferences with fallback to original values.

## ⚙️ Configuration Options

```kotlin
runfig {
    // Target specific build variants
    variants("debug", "staging")
    
    // Exclude sensitive fields from transformation
    excludeFields("VERSION_CODE", "BUILD_TYPE", "SENSITIVE_KEY")
    
    // Use custom SharedPreferences file
    preferencesName = "my_app_config"  // Default: "runfig_prefs"
    
    // Add prefix to preference keys
    fieldPrefix = "RUNFIG_"  // Keys become: RUNFIG_API_BASE_URL, etc.
}
```

## 🛠️ Programmatic Access

```kotlin
// Read values
val apiUrl = RunfigCache.getString("runfig_prefs", "API_BASE_URL", "default")
val timeout = RunfigCache.getInt("runfig_prefs", "NETWORK_TIMEOUT", 5000)
val enabled = RunfigCache.getBoolean("runfig_prefs", "ENABLE_FEATURE", false)

// Update values
RunfigCache.putString("runfig_prefs", "API_BASE_URL", "https://new-api.com")
RunfigCache.putInt("runfig_prefs", "NETWORK_TIMEOUT", 60000)

// Get all configurations
val allConfig = RunfigCache.getAll("runfig_prefs")
```

## 🎨 Debug Interface

The debug overlay provides three tabs:

- **Build Parameters**: Edit configuration values with type-safe controls
- **Info**: View system and app information (extensible)
- **Actions**: Execute debug actions (extensible)

## 🔧 Requirements

- Android Gradle Plugin 7.0+
- Gradle 7.0+
- Android API 24+
- The `runfig-android` dependency (plugin will warn if missing)

## 📦 Installation with Version Catalog

```toml
# libs.versions.toml
[versions]
runfig = "6.0.0"

[libraries]
runfig-android = { module = "dev.supersam.runfig:runfig-android", version.ref = "runfig" }

[plugins]
runfig = { id = "dev.supersam.runfig.gradle", version.ref = "runfig" }
```

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.runfig)
}

dependencies {
    implementation(libs.runfig.android)
}
```

## 🔧 Troubleshooting

**Plugin warning about missing dependency:**
```
⚠️ Runfig plugin is applied but 'dev.supersam.runfig:runfig-android' dependency is missing!
```
Add the `runfig-android` dependency to your app module.

**Fields not appearing in debug interface:**
- Check they're not in `excludeFields`
- Verify correct `preferencesName`
- Check the generated BuildConfig.java to confirm transformation

## 📝 License

MIT License - see LICENSE file for details.

---

<div align="center">
  <p>Built for Android developers who want runtime configuration flexibility</p>
</div>