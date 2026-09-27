package app.vanta.launcher.data.remote

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.vanta.launcher.StandardApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Notification access does two jobs: it grants media-session access, and it feeds the Live page
 * with every app's active notifications (the plain NotificationManager only ever returns our own).
 */
class StandardMediaListener : NotificationListenerService() {

    companion object {
        private val _active = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        /** Active notifications of all apps while the listener is bound; empty otherwise. */
        val active: StateFlow<List<StatusBarNotification>> = _active

        private val _connected = MutableStateFlow(false)
        /** True between onListenerConnected and onListenerDisconnected. */
        val connected: StateFlow<Boolean> = _connected
    }

    private fun publish() {
        _active.value = try {
            activeNotifications?.filter { !it.isOngoing || it.notification.actions != null }?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _connected.value = true
        publish()
        (application as? StandardApplication)?.container?.mediaRepository?.refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _connected.value = false
        _active.value = emptyList()
        (application as? StandardApplication)?.container?.mediaRepository?.refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publish()
    }
}
