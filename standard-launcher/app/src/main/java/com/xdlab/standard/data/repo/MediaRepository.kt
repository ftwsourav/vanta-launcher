package com.xdlab.standard.data.repo

import com.xdlab.standard.domain.model.MediaInfo
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    val nowPlaying: StateFlow<MediaInfo?>
    val albumArt: StateFlow<android.graphics.Bitmap?>
    fun hasAccess(): Boolean
    fun refresh()
    fun playPause()
    fun next()
    fun previous()
}
