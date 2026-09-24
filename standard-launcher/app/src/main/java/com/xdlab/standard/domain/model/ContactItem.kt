package com.xdlab.standard.domain.model

data class ContactItem(
    val id: String,
    val displayName: String,
    val phoneNumber: String?,
    val initials: String,
    val avatarColorIndex: Int
)
