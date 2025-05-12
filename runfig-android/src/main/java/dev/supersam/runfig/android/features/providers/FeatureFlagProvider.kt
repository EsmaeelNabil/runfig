package dev.supersam.runfig.android.features.providers

import androidx.compose.runtime.MutableState

interface FeatureFlagProvider {
    val featureFlags: Map<String, MutableState<Boolean>>
}