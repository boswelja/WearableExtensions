package com.boswelja.smartwatchextensions

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.boswelja.smartwatchextensions.common.R

object GooglePlayUtils {

    /**
     * Gets an [Intent] that launches the Google Play Store to the listing for the package that
     * provided the [Context].
     * @param context [Context].
     * @return The [Intent] to launch the Play Store with.
     */
    fun getPlayStoreIntent(context: Context): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            data = context.getString(R.string.play_store_link).toUri()
            setPackage("com.android.vending")
        }
}
