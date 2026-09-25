package app.vanta.launcher.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import app.vanta.launcher.data.repo.ContactsRepository
import app.vanta.launcher.domain.model.ContactItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsRepositoryImpl(private val context: Context) : ContactsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    override val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _permissionGranted = MutableStateFlow(hasPermission())
    override val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    init {
        refresh()
    }

    fun setPermissionGranted(granted: Boolean) {
        _permissionGranted.value = granted
        if (granted) refresh()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    override fun refresh() {
        scope.launch {
            _permissionGranted.value = hasPermission()
            if (!_permissionGranted.value) return@launch
            try {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon()
                    .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, "24")
                    .build()
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.STARRED
                )
                val sort = "${ContactsContract.CommonDataKinds.Phone.STARRED} DESC, " +
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                val seen = mutableSetOf<Long>()
                val list = mutableListOf<ContactItem>()
                context.contentResolver.query(uri, projection, null, null, sort)?.use { c ->
                    val idIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (c.moveToNext()) {
                        val contactId = c.getLong(idIdx)
                        if (!seen.add(contactId)) continue
                        val name = c.getString(nameIdx) ?: continue
                        val initials = name.split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .take(2).joinToString("").ifEmpty { "?" }
                        list.add(
                            ContactItem(
                                id = contactId.toString(),
                                displayName = name,
                                phoneNumber = c.getString(numIdx),
                                initials = initials,
                                avatarColorIndex = (name.hashCode() and 0x7fffffff) % 8
                            )
                        )
                        if (list.size >= 6) break
                    }
                }
                _contacts.value = list
            } catch (e: Exception) {
                Log.w("StandardContacts", "contacts query failed", e)
            }
        }
    }
}
