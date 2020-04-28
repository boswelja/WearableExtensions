/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui.base

import android.os.Bundle
import android.view.MenuItem
import com.boswelja.devicemanager.R
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

/**
 * An activity that extends [BaseDayNightActivity]. This automatically handles setting up a toolbar
 * with optional elevation.
 */
abstract class BaseToolbarActivity : BaseDayNightActivity() {

    private var toolbarElevated = false

    /**
     * Set the content view ID, must contain a toolbar with ID [R.id.toolbar].
     */
    abstract fun getContentViewId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(getContentViewId())

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            Timber.i("Navigating back")
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Create a [Snackbar] on the base content view.
     * @param message The message to show the [Snackbar] with.
     */
    fun createSnackBar(message: String) {
        Snackbar.make(findViewById(R.id.content_view), message, Snackbar.LENGTH_LONG).show()
    }
}
