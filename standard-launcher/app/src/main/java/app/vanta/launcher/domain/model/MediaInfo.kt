package app.vanta.launcher.domain.model

data class MediaInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    val isPlaying: Boolean,
    val packageName: String?
)
