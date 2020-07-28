/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui.main.appinfo

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import com.boswelja.common.donate.DonationResultInterface
import com.boswelja.common.donate.ui.DonationDialog
import com.boswelja.devicemanager.BuildConfig
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.common.GooglePlayUtils
import com.boswelja.devicemanager.common.GooglePlayUtils.getPlayStoreLink
import com.boswelja.devicemanager.ui.base.BaseWatchPickerPreferenceFragment
import com.boswelja.devicemanager.ui.changelog.ChangelogDialogFragment
import timber.log.Timber

class AppInfoFragment :
    BaseWatchPickerPreferenceFragment(),
    Preference.OnPreferenceClickListener,
    DonationResultInterface {

    private val viewModel: AppInfoViewModel by viewModels()

    private val customTabsIntent: CustomTabsIntent by lazy {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultShareMenuItemEnabled(true)
            .build()
    }

    private lateinit var watchVersionPreference: Preference

    override fun onPreferenceClick(preference: Preference?): Boolean {
        Timber.d("onPreferenceClick() called")
        when (preference?.key) {
            OPEN_PRIVACY_POLICY_KEY -> showPrivacyPolicy()
            SHARE_APP_KEY -> showShareMenu()
            LEAVE_REVIEW_KEY -> showPlayStorePage()
            OPEN_DONATE_DIALOG_KEY -> showDonationDialog()
            OPEN_CHANGELOG_KEY -> showChangelog()
            else -> return false
        }
        return true
    }

    override fun billingSetupFailed() {
        activity.createSnackBar(getString(R.string.donation_connection_failed_message))
    }

    override fun skuQueryFailed() {
        activity.createSnackBar(getString(R.string.donation_connection_failed_message))
    }

    override fun donationFailed() {
        activity.createSnackBar(getString(R.string.donation_failed_message))
    }

    override fun onDonate() {
        activity.createSnackBar(getString(R.string.donation_processed_message))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_about)
        findPreference<Preference>(OPEN_PRIVACY_POLICY_KEY)!!.apply {
            onPreferenceClickListener = this@AppInfoFragment
        }
        findPreference<Preference>(SHARE_APP_KEY)!!.apply {
            isEnabled = !BuildConfig.DEBUG
            title = getString(R.string.pref_about_share_title)
            onPreferenceClickListener = this@AppInfoFragment
        }
        findPreference<Preference>(LEAVE_REVIEW_KEY)!!.apply {
            isEnabled = !BuildConfig.DEBUG
            onPreferenceClickListener = this@AppInfoFragment
        }
        findPreference<Preference>(OPEN_DONATE_DIALOG_KEY)!!.apply {
            isEnabled = !BuildConfig.DEBUG
            onPreferenceClickListener = this@AppInfoFragment
        }
        findPreference<Preference>(OPEN_CHANGELOG_KEY)!!.apply {
            onPreferenceClickListener = this@AppInfoFragment
        }
        findPreference<Preference>(PHONE_VERSION_KEY)!!.apply {
            title = getString(R.string.pref_about_phone_version_title).format(BuildConfig.VERSION_NAME)
            summary = BuildConfig.VERSION_CODE.toString()
        }
        watchVersionPreference = findPreference(WATCH_VERSION_KEY)!!
        getWatchConnectionManager()?.connectedWatch?.observe(viewLifecycleOwner) {
            it?.id?.let { id -> viewModel.requestUpdateWatchVersion(id) }
        }
        viewModel.watchAppVersion.observe(viewLifecycleOwner) {
            setWatchVersionInfo(it)
        }
    }

    /**
     * Sets the watch version info shown to the user to the provided values.
     */
    private fun setWatchVersionInfo(versionInfo: Pair<String, String?>?) {
        if (versionInfo != null) {
            watchVersionPreference.title = versionInfo.first
            watchVersionPreference.summary = versionInfo.second
        } else {
            watchVersionPreference.setTitle(R.string.pref_about_watch_version_failed)
            watchVersionPreference.summary = null
        }
    }

    /**
     * Launches a custom tab that navigates to the Wearable Extensions privacy policy.
     */
    private fun showPrivacyPolicy() {
        Timber.d("showPrivacyPolicy() called")
        customTabsIntent.launchUrl(requireContext(), getString(R.string.privacy_policy_url).toUri())
    }

    /**
     * Shows the system Share sheet to share the Play Store link to Wearable Extensions.
     */
    private fun showShareMenu() {
        val shareTitle = getString(R.string.app_name)
        val shareDataUri = Uri.Builder().apply {
            scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            authority(resources.getResourcePackageName(R.mipmap.ic_launcher))
            appendPath(resources.getResourceTypeName(R.mipmap.ic_launcher))
            appendPath((resources.getResourceEntryName(R.mipmap.ic_launcher)))
        }.build()

        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getPlayStoreLink(context))
            putExtra(Intent.EXTRA_TITLE, shareTitle)
            data = shareDataUri
            type = "text/plain"
        }.also {
            Intent.createChooser(it, null).also { shareIntent ->
                Timber.i("Showing share sheet")
                startActivity(shareIntent)
            }
        }
    }

    /**
     * Opens the Play Store and navigates to the Wearable Extensions listing.
     */
    private fun showPlayStorePage() {
        GooglePlayUtils.getPlayStoreIntent(context).also {
            Timber.i("Opening Play Store")
            startActivity(it)
        }
    }

    /**
     * Create an instance of [DonationDialog] and shows it.
     */
    private fun showDonationDialog() {
        Timber.d("showDonationDialog() called")
        DonationDialog(R.style.AppTheme_AlertDialog).apply {
            setDonationResultInterface(this@AppInfoFragment)
        }.also {
            it.show(activity.supportFragmentManager)
        }
    }

    /**
     * Creates an instance of [ChangelogDialogFragment] and shows it.
     */
    private fun showChangelog() {
        Timber.d("showChangelog() called")
        ChangelogDialogFragment().show(activity.supportFragmentManager)
    }

    companion object {
        const val OPEN_PRIVACY_POLICY_KEY = "privacy_policy"
        const val SHARE_APP_KEY = "share"
        const val LEAVE_REVIEW_KEY = "review"
        const val OPEN_DONATE_DIALOG_KEY = "show_donate_dialog"
        const val OPEN_CHANGELOG_KEY = "show_changelog"
        const val PHONE_VERSION_KEY = "phone_app_version"
        const val WATCH_VERSION_KEY = "watch_app_version"
    }
}
