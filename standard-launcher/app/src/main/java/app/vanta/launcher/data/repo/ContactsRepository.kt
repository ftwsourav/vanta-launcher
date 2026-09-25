package app.vanta.launcher.data.repo

import app.vanta.launcher.domain.model.ContactItem
import kotlinx.coroutines.flow.StateFlow

interface ContactsRepository {
    val contacts: StateFlow<List<ContactItem>>
    val permissionGranted: StateFlow<Boolean>
    fun refresh()
}
