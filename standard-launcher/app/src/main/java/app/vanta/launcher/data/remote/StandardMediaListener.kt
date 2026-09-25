package app.vanta.launcher.data.remote

import android.service.notification.NotificationListenerService
import app.vanta.launcher.StandardApplication

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
