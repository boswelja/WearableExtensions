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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.common.appmanager.AppPackageInfo
import com.boswelja.devicemanager.common.appmanager.AppPackageInfoList
import com.boswelja.devicemanager.common.appmanager.References.PACKAGE_ADDED
import com.boswelja.devicemanager.common.appmanager.References.PACKAGE_REMOVED
import com.boswelja.devicemanager.common.appmanager.References.REQUEST_OPEN_PACKAGE
import com.boswelja.devicemanager.common.appmanager.References.REQUEST_UNINSTALL_PACKAGE
import com.boswelja.devicemanager.databinding.FragmentAppManagerBinding
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable

class AppManagerFragment : Fragment() {

    private lateinit var messageClient: MessageClient
    private lateinit var activity: AppManagerActivity
    private lateinit var binding: FragmentAppManagerBinding

    private var allAppsCache: AppPackageInfoList? = null
    private var watchId: String? = null

    private val messageListener = MessageClient.OnMessageReceivedListener {
        when (it.path) {
            PACKAGE_ADDED -> {
                val appPackageInfo = AppPackageInfo.fromByteArray(it.data)
                handlePackageAdded(appPackageInfo)
            }
            PACKAGE_REMOVED -> {
                val appPackageName = String(it.data, Charsets.UTF_8)
                handlePackageRemoved(appPackageName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messageClient = Wearable.getMessageClient(context!!)
        activity = getActivity() as AppManagerActivity

        watchId = activity.watchId
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_app_manager, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appsRecyclerview.apply {
            layoutManager = LinearLayoutManager(
                    context!!,
                    LinearLayoutManager.VERTICAL,
                    false)
            adapter = AppsAdapter(this@AppManagerFragment)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    activity.elevateToolbar(recyclerView.canScrollVertically(-1))
                }
            })
        }
        if (allAppsCache != null) {
            setAllApps(allAppsCache!!)
        }
    }

    override fun onResume() {
        super.onResume()
        messageClient.addListener(messageListener)
    }

    override fun onPause() {
        super.onPause()
        messageClient.removeListener(messageListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            APP_INFO_ACTIVITY_REQUEST_CODE -> {
                activity.canStopAppManagerService = true
                when (resultCode) {
                    AppInfoActivity.RESULT_REQUEST_UNINSTALL -> {
                        val app = data?.extras?.getSerializable(AppInfoActivity.EXTRA_APP_INFO) as AppPackageInfo
                        sendUninstallRequestMessage(app)
                        activity.createSnackBar(getString(R.string.app_manager_continue_on_watch))
                    }
                    AppInfoActivity.RESULT_REQUEST_OPEN -> {
                        val app = data?.extras?.getSerializable(AppInfoActivity.EXTRA_APP_INFO) as AppPackageInfo
                        sendOpenRequestMessage(app)
                        activity.createSnackBar(getString(R.string.app_manager_continue_on_watch))
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
        (binding.appsRecyclerview.adapter as AppsAdapter).add(appInfo)
        activity.createSnackBar(getString(R.string.app_manager_installed_prefix, appInfo.packageLabel))
    }

    /**
     * Removes an [AppPackageInfo] from the [AppsAdapter] and notifies the user.
     * @param appPackageName The package name of the [AppPackageInfo] to remove.
     */
    private fun handlePackageRemoved(appPackageName: String) {
        val adapter = (binding.appsRecyclerview.adapter as AppsAdapter)
        adapter.remove(appPackageName)
        activity.createSnackBar(getString(
                R.string.app_manager_uninstalled_prefix,
                adapter.getFromPackageName(appPackageName)?.packageLabel))
    }

    /**
     * Request uninstalling an app from the connected watch.
     * @param appPackageInfo The [AppPackageInfo] to request an uninstall for.
     */
    private fun sendUninstallRequestMessage(appPackageInfo: AppPackageInfo) {
        messageClient.sendMessage(
                watchId!!, REQUEST_UNINSTALL_PACKAGE,
                appPackageInfo.packageName.toByteArray(Charsets.UTF_8))
    }

    /**
     * Request opening an app's launch activity on the connected watch.
     * @param appPackageInfo The [AppPackageInfo] to open the launch activity for.
     */
    private fun sendOpenRequestMessage(appPackageInfo: AppPackageInfo) {
        messageClient.sendMessage(
                watchId!!, REQUEST_OPEN_PACKAGE,
                appPackageInfo.packageName.toByteArray(Charsets.UTF_8))
    }

    /**
     * Sets the list of apps to show in the [AppsAdapter].
     * @param apps The new list of [AppPackageInfo] to display.
     */
    fun setAllApps(apps: AppPackageInfoList) {
        allAppsCache = try {
            (binding.appsRecyclerview.adapter as AppsAdapter).setAllApps(apps)
            null
        } catch (e: Exception) {
            apps
        }
    }

    /**
     * Launches an [AppInfoActivity] for a given [AppPackageInfo].
     * @param appPackageInfo The [AppPackageInfo] object to pass on to the [AppInfoActivity].
     */
    fun launchAppInfoActivity(appPackageInfo: AppPackageInfo) {
        activity.canStopAppManagerService = false
        val intent = Intent(context, AppInfoActivity::class.java)
        intent.putExtra(AppInfoActivity.EXTRA_APP_INFO, appPackageInfo)
        startActivityForResult(intent, APP_INFO_ACTIVITY_REQUEST_CODE)
    }

    companion object {
        private const val APP_INFO_ACTIVITY_REQUEST_CODE = 22668
    }
}
