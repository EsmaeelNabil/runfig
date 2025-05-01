package dev.supersam.runfig.android.api

/** Central registry for custom providers and actions. Managed internally. */
internal object DebugOverlayRegistry {
    internal val infoProviders = mutableListOf<DebugInfoProvider>()
    internal val actions = mutableListOf<DebugAction>()

    fun addProvider(provider: DebugInfoProvider) {
        infoProviders.add(provider)
    }

    fun addAction(action: DebugAction) {
        actions.add(action)
    }
}

