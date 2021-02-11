package com.boswelja.devicemanager.managespace.ui.actions

import android.os.Bundle
import android.view.View
import com.boswelja.devicemanager.R
import com.boswelja.devicemanager.managespace.ui.sheets.BaseResetBottomSheet
import com.boswelja.devicemanager.managespace.ui.sheets.ResetExtensionsBottomSheet

/**
 * A [ActionFragment] to handle resetting all extension-related settings.
 */
class ResetExtensionsActionFragment : ActionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            title.text = getString(R.string.reset_extensions_title)
            desc.text = getString(R.string.reset_extensions_desc)
            button.text = getString(R.string.reset_extensions_title)
        }
    }

    override fun onCreateSheet(): BaseResetBottomSheet = ResetExtensionsBottomSheet()
}