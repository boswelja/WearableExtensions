package com.boswelja.devicemanager.dashboard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.boswelja.devicemanager.common.LifecycleAwareTimer
import com.boswelja.devicemanager.dashboard.ui.items.AppManagerDashboardFragment
import com.boswelja.devicemanager.dashboard.ui.items.BatterySyncDashboardFragment
import com.boswelja.devicemanager.dashboard.ui.items.DnDSyncDashboardFragment
import com.boswelja.devicemanager.dashboard.ui.items.PhoneLockingDashboardFragment
import com.boswelja.devicemanager.databinding.FragmentDashboardBinding
import com.boswelja.devicemanager.watchmanager.WatchManager
import kotlin.reflect.full.primaryConstructor
import timber.log.Timber

class DashboardFragment : Fragment() {

    private val watchManager by lazy { WatchManager.getInstance(requireContext()) }
    private val refreshDataTimer = LifecycleAwareTimer(period = 10) {
        watchManager.refreshData()
    }

    private lateinit var binding: FragmentDashboardBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val transaction = childFragmentManager.beginTransaction()
            ALL_FRAGMENTS.forEach {
                Timber.d("Adding $it")
                transaction.add(binding.dashboardItems.id, it.primaryConstructor!!.call())
            }
            transaction.commit()
        }
        viewLifecycleOwner.lifecycle.addObserver(refreshDataTimer)
    }

    override fun onStart() {
        super.onStart()
        watchManager.selectedWatch.observe(this) { watch ->
            if (watch == null) {
                Timber.w("Selected watch is null")
                return@observe
            }
            Timber.d("Got watch status: ${watch.status}")
            binding.watchStatusText.setText(watch.status.stringRes)
            binding.watchStatusIcon.setImageResource(watch.status.iconRes)
        }
    }

    companion object {
        private val ALL_FRAGMENTS = listOf(
            BatterySyncDashboardFragment::class,
            DnDSyncDashboardFragment::class,
            PhoneLockingDashboardFragment::class,
            AppManagerDashboardFragment::class
        )
    }
}
