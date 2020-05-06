package com.boswelja.devicemanager.ui.base

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.databinding.ActivitySettingsBinding
import timber.log.Timber

abstract class BasePreferenceActivity :
        BaseToolbarActivity(),
        PreferenceActivityInterface {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivitySettingsBinding>(this, R.layout.activity_settings)
        setupToolbar(binding.toolbarLayout.toolbar, showUpButton = true)
        showFragments()
    }

    /**
     * Gets both preference fragment and widget fragment and shows them.
     * If [getWidgetFragment] returns null, it's ignored and the layout is cleaned up to remove it.
     */
    private fun showFragments() {
        Timber.d("showFragments() called")
        val preferenceFragment = getPreferenceFragment()
        val widgetFragment = getWidgetFragment()
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_holder, preferenceFragment)
            if (widgetFragment != null) {
                replace(R.id.widget_holder, widgetFragment)
            } else {
                Timber.i("No widget fragment to load")
                findViewById<View>(R.id.widget_divider).visibility = View.GONE
            }
        }.also {
            it.commitNow()
        }
    }
}
