package dev.supersam.runfig.plugin

open class RunfigExtension {
    // List of variant names to apply the transformation to
    var variantNames: MutableList<String> = mutableListOf()

    // Helper function to make configuration more readable
    fun variants(vararg names: String) {
        variantNames.addAll(names)
    }
}