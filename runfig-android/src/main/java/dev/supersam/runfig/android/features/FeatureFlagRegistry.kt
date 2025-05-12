package dev.supersam.runfig.android.features

import dev.supersam.runfig.android.features.providers.FeatureFlagProvider

object FeatureFlagRegistry {
    val providers = mutableListOf<FeatureFlagProvider>()
    fun addProvider(provider: FeatureFlagProvider) {
        providers.add(provider)
    }
}