package com.xdlab.standard.data.remote

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.xdlab.standard.data.repo.MediaRepository
import com.xdlab.standard.domain.model.MediaInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Now-playing via MediaSessionManager. Needs the user to enable [StandardMediaListener] under
 * notification access; until then [nowPlaying] stays null and [hasAccess] is false. Event-driven, no polling.
 */
class MediaRepositoryImpl(private val context: Context) : MediaRepository {

    private val component = ComponentName(context, StandardMediaListener::class.java)
    private val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _nowPlaying = MutableStateFlow<MediaInfo?>(null)
    override val nowPlaying: StateFlow<MediaInfo?> = _nowPlaying.asStateFlow()

    private val _albumArt = MutableStateFlow<Bitmap?>(null)
    override val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()

    private var controller: MediaController? = null
    private var listening = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() {
            attach(null)
            refresh()
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        attach(pick(controllers))
    }

    init {
        refresh()
    }

    override fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    override fun refresh() {
        if (!hasAccess()) {
            attach(null)
            return
        }
        try {
            if (!listening) {
                manager?.addOnActiveSessionsChangedListener(sessionsListener, component, mainHandler)
                listening = true
            }
            attach(pick(manager?.getActiveSessions(component)))
        } catch (e: SecurityException) {
            Log.w(TAG, "no notification access yet", e)
            attach(null)
        }
    }

    /** Prefer the session that is actually playing, then the first one. */
    private fun pick(controllers: List<MediaController>?): MediaController? =
        controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers?.firstOrNull()

    private fun attach(next: MediaController?) {
        val current = controller
        if (current != null && next != null && current.sessionToken == next.sessionToken) {
            publish()
            return
        }
        current?.unregisterCallback(controllerCallback)
        controller = next
        next?.registerCallback(controllerCallback, mainHandler)
        publish()
    }

    private fun publish() {
        val c = controller
        val md = c?.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (c == null || title.isNullOrBlank()) {
            _nowPlaying.value = null
            _albumArt.value = null
            return
        }
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        _albumArt.value = art
        _nowPlaying.value = MediaInfo(
            title = title,
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST),
            album = md.getString(MediaMetadata.METADATA_KEY_ALBUM),
            isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = c.packageName
        )
    }

    override fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    override fun next() {
        controller?.transportControls?.skipToNext()
    }

    override fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    private companion object {
        const val TAG = "StandardMedia"
    }
}
