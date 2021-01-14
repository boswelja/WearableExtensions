@file:Suppress("SpellCheckingInspection")

object DebugInfo {
    const val idSuffix = ".debug"
}

object PackageInfo {
    const val packageName = "com.boswelja.devicemanager"
    const val versionName = "2.5.6"

    fun getVersionCode(): Int {
        // We need an offset to ensure it's newer than the old versions
        return ((System.currentTimeMillis() / 1000) + 413000000).toInt()
    }
}
