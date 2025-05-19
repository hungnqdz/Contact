import android.Manifest.permission.READ_CALL_LOG
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.provider.CallLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class CallLogEntry(
    val number: String,
    val name: String?,
    val date: String,
    val type: String,
    val duration: String
)


fun addContact(context: Context, name: String, phone: String, email: String) {
    if (ContextCompat.checkSelfPermission(context, WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            context as androidx.appcompat.app.AppCompatActivity,
            arrayOf(WRITE_CONTACTS, READ_CONTACTS),
            100
        )
        return
    }

    val ops = java.util.ArrayList<ContentProviderOperation>()

    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
        .build())

    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        .build())

    // Thêm số điện thoại
    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
        .build())

    // Thêm email
    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
        .build())

    try {
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        Toast.makeText(context, "Contact added successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error adding contact", Toast.LENGTH_SHORT).show()
    }
}

fun updateContactByName(context: Context, name: String, newPhone: String, newEmail: String) {
    // Kiểm tra quyền
    if (ContextCompat.checkSelfPermission(context, WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            context as androidx.appcompat.app.AppCompatActivity,
            arrayOf(WRITE_CONTACTS, READ_CONTACTS),
            100
        )
        Toast.makeText(context, "Permission required to update contact", Toast.LENGTH_SHORT).show()
        return
    }

    // Tìm contactId theo tên
    val contactId = context.contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(ContactsContract.Contacts._ID),
        "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
        arrayOf(name),
        null
    )?.use {
        if (it.moveToFirst()) {
            it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
        } else {
            null
        }
    }

    if (contactId == null) {
        Toast.makeText(context, "Contact not found", Toast.LENGTH_SHORT).show()
        return
    }

    val ops = ArrayList<ContentProviderOperation>()

    // Cập nhật số điện thoại (nếu đã tồn tại, nếu không thì thêm mới)
    ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
        .withSelection(
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        )
        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone)
        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
        .build())

    // Nếu không có số điện thoại, thêm mới
    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValue(ContactsContract.Data.CONTACT_ID, contactId)
        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone)
        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
        .withYieldAllowed(true)
        .build())

    // Cập nhật email (nếu đã tồn tại, nếu không thì thêm mới)
    ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
        .withSelection(
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
        )
        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, newEmail)
        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
        .build())

    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValue(ContactsContract.Data.CONTACT_ID, contactId)
        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, newEmail)
        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
        .withYieldAllowed(true)
        .build())

    try {
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        Toast.makeText(context, "Contact updated successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error updating contact: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun readCallLog(context: Context): List<CallLogEntry> {
    // Kiểm tra quyền
    if (ContextCompat.checkSelfPermission(context, READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            context as androidx.appcompat.app.AppCompatActivity,
            arrayOf(READ_CALL_LOG),
            101
        )
        Toast.makeText(context, "Permission required to read call log", Toast.LENGTH_SHORT).show()
        return emptyList()
    }

    val callLogs = mutableListOf<CallLogEntry>()
    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION
        ),
        null,
        null,
        "${CallLog.Calls.DATE} DESC"
    )

    cursor?.use {
        val numberIdx = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        val nameIdx = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
        val dateIdx = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
        val typeIdx = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
        val durationIdx = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)

        while (it.moveToNext()) {
            val number = it.getString(numberIdx) ?: "Unknown"
            val name = it.getString(nameIdx)
            val dateMillis = it.getLong(dateIdx)
            val type = it.getInt(typeIdx)
            val duration = it.getString(durationIdx) ?: "0"

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val dateString = dateFormat.format(Date(dateMillis))

            val callType = when (type) {
                CallLog.Calls.INCOMING_TYPE -> "Incoming"
                CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                CallLog.Calls.MISSED_TYPE -> "Missed"
                else -> "Unknown"
            }

            callLogs.add(CallLogEntry(number, name, dateString, callType, duration))
        }
    }

    return callLogs
}

fun deleteContactByPhone(context: Context, phoneNumber: String) {
    // Kiểm tra quyền
    if (ContextCompat.checkSelfPermission(context, WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            context as androidx.appcompat.app.AppCompatActivity,
            arrayOf(WRITE_CONTACTS, READ_CONTACTS),
            100
        )
        Toast.makeText(context, "Permission required to delete contact", Toast.LENGTH_SHORT).show()
        return
    }

    // Chuẩn hóa số điện thoại (loại bỏ khoảng trắng, dấu gạch ngang, v.v.)
    val normalizedPhone = phoneNumber.replace("[^0-9+]".toRegex(), "")

    // Tìm contactId dựa trên số điện thoại
    val cursor = context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.Data.CONTACT_ID),
        "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
        arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, normalizedPhone),
        null
    )

    var contactId: String? = null
    cursor?.use {
        if (it.moveToFirst()) {
            contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
        }
    }

    if (contactId == null) {
        Toast.makeText(context, "Contact not found for phone number: $phoneNumber", Toast.LENGTH_SHORT).show()
        return
    }

    // Xóa contact
    try {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
            .withSelection(
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId)
            )
            .build())

        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        Toast.makeText(context, "Contact deleted successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error deleting contact: ${e.message}", Toast.LENGTH_SHORT).show()
    }


}

