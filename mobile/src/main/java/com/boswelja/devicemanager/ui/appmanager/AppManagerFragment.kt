/* Copyright (C) 2020 Jack Boswell <boswelja@outlook.com>
 *
 * This file is part of Wearable Extensions
 *
 * This file, and any part of the Wearable Extensions app/s cannot be copied and/or distributed
 * without permission from Jack Boswell (boswelja) <boswela@outlook.com>
 */
package com.boswelja.devicemanager.ui.appmanager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.common.appmanager.AppPackageInfo
import com.boswelja.devicemanager.common.recyclerview.adapter.ItemClickCallback
import com.boswelja.devicemanager.databinding.FragmentAppManagerBinding
import com.boswelja.devicemanager.ui.appmanager.info.AppInfoActivity
import com.google.android.material.snackbar.Snackbar

class AppManagerFragment : Fragment(), ItemClickCallback<AppPackageInfo> {

    private val viewModel: AppManagerViewModel by activityViewModels()
    private val appsAdapter = AppsAdapter(this)

    private lateinit var binding: FragmentAppManagerBinding

    override fun onClick(item: AppPackageInfo) {
        launchAppInfoActivity(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAppManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.appsRecyclerview.adapter = appsAdapter
        viewModel.allAppsList.observe(viewLifecycleOwner) {
            it?.let { allApps -> appsAdapter.setItems(allApps) }
        }

        viewModel.appAdded.observe(viewLifecycleOwner) {
            it?.let { appPackageInfo -> handlePackageAdded(appPackageInfo) }
        }
        viewModel.appUpdated.observe(viewLifecycleOwner) {
            it?.let { appPackageInfo -> handlePackageUpdated(appPackageInfo) }
        }
        viewModel.appRemoved.observe(viewLifecycleOwner) {
            it?.let { packageName -> handlePackageRemoved(packageName) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            APP_INFO_ACTIVITY_REQUEST_CODE -> {
                viewModel.canStopAppManagerService = true
                when (resultCode) {
                    AppInfoActivity.RESULT_REQUEST_UNINSTALL -> {
                        val app = data?.extras?.getSerializable(AppInfoActivity.EXTRA_APP_INFO) as AppPackageInfo
                        viewModel.sendUninstallRequestMessage(app)
                        createSnackbar(getString(R.string.app_manager_continue_on_watch))
                    }
                    AppInfoActivity.RESULT_REQUEST_OPEN -> {
                        val app = data?.extras?.getSerializable(AppInfoActivity.EXTRA_APP_INFO) as AppPackageInfo
                        viewModel.sendOpenRequestMessage(app)
                        createSnackbar(getString(R.string.app_manager_continue_on_watch))
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Add a new [AppPackageInfo] to the [AppsAdapter] and notifies the user.
     * @param appInfo The [AppPackageInfo] to add.
     */
    private fun handlePackageAdded(appInfo: AppPackageInfo) {
        if (appInfo.isSystemApp) {
            appsAdapter.addItem(1, appInfo)
        } else {
            appsAdapter.addItem(0, appInfo)
        }
        createSnackbar(getString(R.string.app_manager_installed_prefix, appInfo.packageLabel))
    }

    /**
     * Updates an [AppPackageInfo] in the [AppsAdapter] and notifies the user.
     * @param appInfo The [AppPackageInfo] to update.
     */
    private fun handlePackageUpdated(appInfo: AppPackageInfo) {
        appsAdapter.updateItem(appInfo)
        createSnackbar(getString(R.string.app_manager_updated_prefix, appInfo.packageLabel))
    }

    /**
     * Removes an [AppPackageInfo] from the [AppsAdapter] and notifies the user.
     * @param appPackageName The package name of the [AppPackageInfo] to remove.
     */
    private fun handlePackageRemoved(appPackageName: String) {
        val removedApp = appsAdapter.removeByPackageName(appPackageName)
        if (removedApp != null) {
            createSnackbar(
                getString(
                    R.string.app_manager_uninstalled_prefix, removedApp.packageLabel
                )
            )
        }
    }

    private fun createSnackbar(text: String) {
        view?.let { Snackbar.make(it, text, Snackbar.LENGTH_LONG).show() }
    }

    /**
     * Launches an [AppInfoActivity] for a given [AppPackageInfo].
     * @param appPackageInfo The [AppPackageInfo] object to pass on to the [AppInfoActivity].
     */
    private fun launchAppInfoActivity(appPackageInfo: AppPackageInfo) {
        viewModel.canStopAppManagerService = false
        val intent = Intent(context, AppInfoActivity::class.java)
        intent.putExtra(AppInfoActivity.EXTRA_APP_INFO, appPackageInfo)
        startActivityForResult(intent, APP_INFO_ACTIVITY_REQUEST_CODE)
    }

    companion object {
        private const val APP_INFO_ACTIVITY_REQUEST_CODE = 22668
    }
}
