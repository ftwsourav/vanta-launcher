package com.xdlab.standard.data.remote

import android.service.notification.NotificationListenerService
import com.xdlab.standard.StandardApplication

/**
 * Exists only so the system grants us media-session access once the user enables it under
 * Settings > Notification access. We never read notifications.
 */
class StandardMediaListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        (application as? StandardApplication)?.container?.mediaRepository?.refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        (application as? StandardApplication)?.container?.mediaRepository?.refresh()
    }
}
