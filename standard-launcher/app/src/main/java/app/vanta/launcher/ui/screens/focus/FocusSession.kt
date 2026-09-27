package app.vanta.launcher.ui.screens.focus

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A timed focus session shared across pages: while [active], Home hides every tile that is not a
 * focus app and the Focus page shows the countdown. Survives a process restart through the
 * "focus_session" prefs; while notification-policy access is granted it switches the interruption
 * filter to PRIORITY on [start] and puts the exact previous filter back on [stop]/[finish].
 * ponytail: process-global singleton; move into the view model if a second consumer needs injection.
 * ponytail: ends only when the launcher draws a frame (no AlarmManager); add one if DND must lift while backgrounded.
 */
object FocusSession {
    private const val PREFS = "focus_session"
    private const val KEY_END = "endAtMillis"
    private const val KEY_MINUTES = "minutes"
    private const val KEY_PREV_FILTER = "prevFilter"
    private val EndTime: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    private val _endAtMillis = MutableStateFlow(0L)
    /** Wall-clock end of the running session, 0 when idle. */
    val endAtMillis: StateFlow<Long> = _endAtMillis

    private val _minutes = MutableStateFlow(25)
    val minutes: StateFlow<Int> = _minutes

    private var app: Context? = null
    private val prefs: SharedPreferences? get() = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val notifications: NotificationManager?
        get() = app?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    /** True when the user granted Do Not Disturb access to the launcher. */
    val dndGranted: Boolean get() = notifications?.isNotificationPolicyAccessGranted == true

    /** Idempotent: keeps the app context and resumes (or closes out) a session persisted before a process restart. */
    fun restore(context: Context) {
        if (app != null) return
        app = context.applicationContext
        val p = prefs ?: return
        _minutes.value = p.getInt(KEY_MINUTES, 25)
        val end = p.getLong(KEY_END, 0L)
        when {
            end > System.currentTimeMillis() -> {
                _endAtMillis.value = end
                _active.value = true
            }
            end != 0L -> {
                // Ran out while the process was dead: still log it and lift DND.
                _endAtMillis.value = end
                finish()
            }
        }
    }

    fun start(minutes: Int) {
        _minutes.value = minutes.coerceIn(1, 180)
        _endAtMillis.value = System.currentTimeMillis() + _minutes.value * 60_000L
        _active.value = true
        prefs?.edit()?.putLong(KEY_END, _endAtMillis.value)?.putInt(KEY_MINUTES, _minutes.value)?.apply()
        silence()
    }

    /** Ends early: nothing logged, previous interruption filter put back. */
    fun stop() {
        _active.value = false
        _endAtMillis.value = 0L
        prefs?.edit()?.remove(KEY_END)?.apply()
        restoreFilter()
    }

    /** The countdown ran out: "FOCUS · 25 MIN · 21:30" is appended to Home's notes, then [stop]. */
    fun finish() {
        val end = _endAtMillis.value
        if (end != 0L) {
            val at = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).format(EndTime)
            appendNote("FOCUS · ${_minutes.value} MIN · $at")
        }
        stop()
    }

    fun remainingMillis(): Long = (_endAtMillis.value - System.currentTimeMillis()).coerceAtLeast(0L)

    /**
     * Priority-only interruptions while the session runs; remembers the filter it replaced. Safe to
     * call again (e.g. after the user grants access mid-session): a no-op once the filter is stored.
     */
    fun silence() {
        val nm = notifications ?: return
        val p = prefs ?: return
        if (!_active.value || !nm.isNotificationPolicyAccessGranted || p.contains(KEY_PREV_FILTER)) return
        p.edit().putInt(KEY_PREV_FILTER, nm.currentInterruptionFilter).apply()
        runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
    }

    private fun restoreFilter() {
        val p = prefs ?: return
        if (!p.contains(KEY_PREV_FILTER)) return
        val prev = p.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        p.edit().remove(KEY_PREV_FILTER).apply()
        val nm = notifications ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        val target = if (prev == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) NotificationManager.INTERRUPTION_FILTER_ALL else prev
        runCatching { nm.setInterruptionFilter(target) }
    }

    /** Same store and key as Home's notes tile ("standard_notes" -> "notes", newline-joined). */
    private fun appendNote(line: String) {
        val notes = app?.getSharedPreferences("standard_notes", Context.MODE_PRIVATE) ?: return
        val existing = notes.getString("notes", "").orEmpty()
        notes.edit().putString("notes", if (existing.isBlank()) line else existing + "\n" + line).apply()
    }
}
