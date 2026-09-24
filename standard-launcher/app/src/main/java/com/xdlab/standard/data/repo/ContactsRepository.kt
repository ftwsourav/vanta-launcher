package com.xdlab.standard.data.repo

import com.xdlab.standard.domain.model.ContactItem
import kotlinx.coroutines.flow.StateFlow

interface ContactsRepository {
    val contacts: StateFlow<List<ContactItem>>
    val permissionGranted: StateFlow<Boolean>
    fun refresh()
}
