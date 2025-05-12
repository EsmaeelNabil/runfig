package dev.supersam.runfig.android.features.actions

import android.content.Context
import android.widget.Toast
import dev.supersam.runfig.android.api.DebugAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ClearCacheAction : DebugAction {
    override val title: String = "Clear Cache"
    override val description: String?
        get() = "Clear app cache directories (internal, external, code cache)."

    override suspend fun onAction(context: Context) {
        var result = false
        withContext(Dispatchers.IO) {
            val internalCache = context.cacheDir?.deleteRecursively() ?: false
            val codeCache = context.codeCacheDir?.deleteRecursively() ?: false
            val externalCache = context.externalCacheDir?.deleteRecursively() ?: false
            result = internalCache || codeCache || externalCache
        }
        withContext(Dispatchers.Main) {
            val message =
                if (result) "Cache cleared" else "Failed to clear some cache directories"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}