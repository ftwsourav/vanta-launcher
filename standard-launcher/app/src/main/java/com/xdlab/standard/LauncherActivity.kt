package com.xdlab.standard

import android.content.Intent

class LauncherActivity : MainActivity() {

    /** HOME pressed while we are already showing: snap back to page 0 and close overlays. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomePressed()
        }
    }
}
