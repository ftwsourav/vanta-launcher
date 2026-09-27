package app.vanta.launcher.data.remote

import android.service.notification.NotificationListenerService
import android.app.Notification
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

        private val _summaries = MutableStateFlow<Map<String, AppNotifSummary>>(emptyMap())
        /** One line per package: count plus the newest title/text, for live tiles and badges. */
        val summaries: StateFlow<Map<String, AppNotifSummary>> = _summaries

        private val _connected = MutableStateFlow(false)
        /** True between onListenerConnected and onListenerDisconnected. */
        val connected: StateFlow<Boolean> = _connected
    }

    private fun publish() {
        val list = try {
            activeNotifications?.filter { !it.isOngoing || it.notification.actions != null }?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        _active.value = list
        _summaries.value = list
            .filter { it.packageName != packageName }
            .groupBy { it.packageName }
            .mapValues { (_, sbns) ->
                val newest = sbns.maxBy { it.postTime }
                val extras = newest.notification.extras
                AppNotifSummary(
                    count = sbns.size,
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                    text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty(),
                    postTime = newest.postTime,
                    key = newest.key,
                    replyAction = newest.notification.actions?.firstOrNull { a -> a.remoteInputs?.isNotEmpty() == true }
                )
            }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _connected.value = true
        // Lets the Live page dismiss a notification for real (swipe / CLEAR).
        app.vanta.launcher.ui.components.NotificationActions.canceller = { key -> runCatching { cancelNotification(key) } }
        publish()
        (application as? StandardApplication)?.container?.mediaRepository?.refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _connected.value = false
        app.vanta.launcher.ui.components.NotificationActions.canceller = null
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

/** Summary of one app's active notifications. [replyAction] is the first action that accepts inline text. */
data class AppNotifSummary(
    val count: Int,
    val title: String,
    val text: String,
    val postTime: Long,
    val key: String,
    val replyAction: Notification.Action?
)
