package com.xdlab.standard.domain.model

enum class TileSize(val widthUnits: Int, val heightDp: Int, val full: Boolean) {
    SMALL(1, 76, false),
    MEDIUM(2, 160, false),
    WIDE(4, 160, true),
    LARGE(4, 320, true);

    fun next(): TileSize = when (this) {
        SMALL -> MEDIUM
        MEDIUM -> WIDE
        WIDE -> LARGE
        LARGE -> SMALL
    }
}
