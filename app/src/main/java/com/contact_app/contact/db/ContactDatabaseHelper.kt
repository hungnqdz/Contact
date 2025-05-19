package com.contact_app.contact.db

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.ContactsContract
import android.provider.CallLog
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import android.widget.Toast
import com.contact_app.contact.model.CallLogStats
import com.contact_app.contact.model.Contact
import com.contact_app.contact.model.Event
import com.contact_app.contact.model.Note
import com.contact_app.contact.model.Schedule
import com.contact_app.contact.model.ScheduleContact
import com.contact_app.contact.model.ScheduleStats
import com.contact_app.contact.model.TimeRange
import java.util.Calendar
import java.util.Date

class ContactDatabaseHelper private constructor(private val context: Context, private val askingPermission: () -> Unit? = {}) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "contact_database.db"
        private const val DATABASE_VERSION = 6

        private const val TABLE_CONTACT = "contact"
        private const val TABLE_NOTES = "notes"
        private const val TABLE_EVENTS = "events"
        private const val TABLE_SCHEDULE = "schedule"
        private const val TABLE_SCHEDULE_CONTACT = "schedule_contact"
        private const val TABLE_CALL_LOG = "call_log"

        private const val COL_ID = "id"

        private val VALID_CONTACT_COLUMNS = setOf(
            "id",
            "first_name",
            "last_name",
            "email",
            "phone",
            "company",
            "address",
            "created_at",
            "gender",
            "birthday"
        )

        @Volatile
        private var instance: ContactDatabaseHelper? = null

        fun getInstance(context: Context, askingPermission: () -> Unit): ContactDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: ContactDatabaseHelper(context.applicationContext, askingPermission).also {
                    instance = it
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CONTACT (
                $COL_ID INTEGER PRIMARY KEY,
                first_name TEXT NOT NULL,
                last_name TEXT,
                email TEXT,
                phone TEXT NOT NULL UNIQUE,
                company TEXT,
                address TEXT,
                created_at INTEGER,
                gender TEXT NOT NULL,
                birthday INTEGER
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                content TEXT,
                comment TEXT,
                contact_id INTEGER,
                date_time INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                FOREIGN KEY (contact_id) REFERENCES $TABLE_CONTACT($COL_ID) ON DELETE CASCADE
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                content TEXT,
                date_time INTEGER,
                id_note INTEGER,
                FOREIGN KEY (id_note) REFERENCES $TABLE_NOTES($COL_ID) ON DELETE CASCADE
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SCHEDULE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                content TEXT,
                type TEXT CHECK(type IN ('online', 'offline')) NOT NULL,
                date_time INTEGER NOT NULL
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SCHEDULE_CONTACT (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                contact_name TEXT,
                schedule_id INTEGER,
                contact_id INTEGER,
                FOREIGN KEY (schedule_id) REFERENCES $TABLE_SCHEDULE($COL_ID) ON DELETE CASCADE,
                FOREIGN KEY (contact_id) REFERENCES $TABLE_CONTACT($COL_ID) ON DELETE CASCADE
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CALL_LOG (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                phone TEXT NOT NULL,
                call_type TEXT CHECK(call_type IN ('INCOMING', 'OUTGOING', 'MISSED')) NOT NULL,
                duration INTEGER,
                call_time INTEGER NOT NULL,
                contact_id INTEGER,
                FOREIGN KEY (contact_id) REFERENCES $TABLE_CONTACT($COL_ID) ON DELETE CASCADE
            )
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CALL_LOG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCHEDULE_CONTACT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCHEDULE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACT")
        onCreate(db)
        // Không gọi initDatabase() ở đây để tránh đệ quy
    }

    fun syncContactsFromDevice() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val deviceContactIds = mutableSetOf<Long>()
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null,
                null,
                null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val contactId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    deviceContactIds.add(contactId)
                }
            }

            val dbContacts = mutableMapOf<Long, ContentValues>()
            val dbCursor = db.query(
                TABLE_CONTACT,
                arrayOf(
                    COL_ID,
                    "first_name",
                    "last_name",
                    "email",
                    "phone",
                    "company",
                    "address",
                    "created_at",
                    "gender",
                    "birthday"
                ),
                null,
                null,
                null,
                null,
                null
            )
            dbCursor?.use {
                while (it.moveToNext()) {
                    val contactId = it.getLong(it.getColumnIndexOrThrow(COL_ID))
                    val values = ContentValues().apply {
                        put("first_name", it.getString(it.getColumnIndexOrThrow("first_name")))
                        put("last_name", it.getString(it.getColumnIndexOrThrow("last_name")))
                        put("email", it.getString(it.getColumnIndexOrThrow("email")))
                        put("phone", it.getString(it.getColumnIndexOrThrow("phone")))
                        put("company", it.getString(it.getColumnIndexOrThrow("company")))
                        put("address", it.getString(it.getColumnIndexOrThrow("address")))
                        put("created_at", it.getLong(it.getColumnIndexOrThrow("created_at")))
                        put("gender", it.getString(it.getColumnIndexOrThrow("gender")))
                        put("birthday", it.getLong(it.getColumnIndexOrThrow("birthday")).takeIf { it > 0 })
                    }
                    dbContacts[contactId] = values
                }
            }

            val dbContactIds = dbContacts.keys
            val contactsToDelete = dbContactIds - deviceContactIds
            for (contactId in contactsToDelete) {
                db.delete(TABLE_CONTACT, "$COL_ID = ?", arrayOf(contactId.toString()))
            }

            val deviceCursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            deviceCursor?.use {
                while (it.moveToNext()) {
                    val contactId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val displayName = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: ""

                    var phone: String? = null
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId.toString()),
                        null
                    )
                    phoneCursor?.use { pc ->
                        if (pc.moveToFirst()) {
                            phone = pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        }
                    }

                    var email: String? = null
                    val emailCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(contactId.toString()),
                        null
                    )
                    emailCursor?.use { ec ->
                        if (ec.moveToFirst()) {
                            email = ec.getString(ec.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
                        }
                    }

                    if (contactId in dbContactIds) {
                        val dbValues = dbContacts[contactId]!!
                        val name = "${dbValues.getAsString("first_name") ?: ""} ${dbValues.getAsString("last_name") ?: ""}".trim()
                        val dbPhone = dbValues.getAsString("phone")
                        val dbEmail = dbValues.getAsString("email")

                        if (name.isNotBlank() || dbPhone != null || dbEmail != null) {
                            updateContactById(contactId.toString(), name, dbPhone ?: "", dbEmail ?: "")
                        }

                        val values = ContentValues().apply {
                            put("first_name", dbValues.getAsString("first_name"))
                            put("last_name", dbValues.getAsString("last_name"))
                            put("email", dbEmail ?: email)
                            put("phone", dbPhone ?: phone)
                            put("company", dbValues.getAsString("company"))
                            put("address", dbValues.getAsString("address"))
                            put("created_at", dbValues.getAsLong("created_at"))
                            put("gender", dbValues.getAsString("gender"))
                            put("birthday", dbValues.getAsLong("birthday"))
                        }
                        db.update(
                            TABLE_CONTACT,
                            values,
                            "$COL_ID = ?",
                            arrayOf(contactId.toString())
                        )
                    } else if (phone != null) {
                        val values = ContentValues().apply {
                            put(COL_ID, contactId)
                            put("first_name", displayName.split(" ").firstOrNull() ?: displayName)
                            put("last_name", displayName.split(" ").drop(1).joinToString(" ").takeIf { it.isNotBlank() } ?: "")
                            put("email", email)
                            put("phone", phone)
                            put("created_at", System.currentTimeMillis())
                            put("gender", "Unknown")
                            put("birthday", null as Long?)
                        }
                        db.insert(TABLE_CONTACT, null, values)
                    }
                }
            }

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error syncing contacts: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            db.endTransaction()
        }
    }

    fun syncCallLogsFromDevice() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_CALL_LOG, null, null)

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
                val dateIdx = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val typeIdx = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationIdx = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                while (it.moveToNext()) {
                    val number = it.getString(numberIdx) ?: continue
                    val dateMillis = it.getLong(dateIdx)
                    val type = it.getInt(typeIdx)
                    val duration = it.getLong(durationIdx)

                    val callType = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        else -> continue
                    }

                    var contactId: Long? = null
                    val contactCursor = db.query(
                        TABLE_CONTACT,
                        arrayOf(COL_ID),
                        "phone = ?",
                        arrayOf(number),
                        null,
                        null,
                        null
                    )
                    contactCursor?.use { cc ->
                        if (cc.moveToFirst()) {
                            contactId = cc.getLong(cc.getColumnIndexOrThrow(COL_ID))
                        }
                    }

                    val values = ContentValues().apply {
                        put("phone", number)
                        put("call_type", callType)
                        put("duration", duration)
                        put("call_time", dateMillis)
                        put("contact_id", contactId)
                    }
                    db.insert(TABLE_CALL_LOG, null, values)
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error syncing call logs: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            db.endTransaction()
        }
    }

    fun insertContactWithSync(contact: Contact): Long? {
        val db = writableDatabase
        var result: Long? = null
        db.beginTransaction()
        try {
            val contactDeviceId = addContactToDevice(
                contact.firstName + " " + (contact.lastName ?: ""),
                contact.phone ?: "",
                contact.email ?: ""
            )?.toLong() ?: throw Exception("Failed to add contact to device")

            val values = ContentValues().apply {
                put(COL_ID, contactDeviceId)
                put("first_name", contact.firstName)
                put("last_name", contact.lastName)
                put("email", contact.email)
                put("phone", contact.phone)
                put("company", contact.company)
                put("address", contact.address)
                put("created_at", contact.createdAt?.time)
                put("gender", contact.gender)
                put("birthday", contact.birthday?.time)
            }
            result = db.insert(TABLE_CONTACT, null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error inserting contact: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            db.endTransaction()
        }
        return result
    }

    fun updateContactWithSync(contact: Contact) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("first_name", contact.firstName)
                put("last_name", contact.lastName)
                put("email", contact.email)
                put("phone", contact.phone)
                put("company", contact.company)
                put("address", contact.address)
                put("created_at", contact.createdAt?.time)
                put("gender", contact.gender)
                put("birthday", contact.birthday?.time)
            }
            db.update(TABLE_CONTACT, values, "$COL_ID = ?", arrayOf(contact.id.toString()))

            updateContactById(
                contact.id.toString(),
                contact.firstName + " " + (contact.lastName ?: ""),
                contact.phone ?: "",
                contact.email ?: ""
            )

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error updating contact: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteContactWithSync(id: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.query(
                TABLE_CONTACT,
                arrayOf("phone"),
                "$COL_ID = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            var phone: String? = null
            cursor?.use {
                if (it.moveToFirst()) {
                    phone = it.getString(it.getColumnIndexOrThrow("phone"))
                }
            }

            db.delete(TABLE_CONTACT, "$COL_ID = ?", arrayOf(id.toString()))

            if (phone != null) {
                deleteContactByPhone(phone ?: "")
            }

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error deleting contact: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            db.endTransaction()
        }
    }

    fun insertContact(contact: Contact): Long {
        val values = ContentValues().apply {
            put("first_name", contact.firstName)
            put("last_name", contact.lastName)
            put("email", contact.email)
            put("phone", contact.phone)
            put("company", contact.company)
            put("address", contact.address)
            put("created_at", contact.createdAt?.time)
            put("gender", contact.gender)
            put("birthday", contact.birthday?.time)
        }
        return writableDatabase.insert(TABLE_CONTACT, null, values)
    }

    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val db = readableDatabase
        val cursor =
            db.query(TABLE_CONTACT, null, null, null, null, null, "first_name COLLATE NOCASE")

        while (cursor.moveToNext()) {
            contacts.add(
                Contact(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                    lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                    company = cursor.getString(cursor.getColumnIndexOrThrow("company")),
                    address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                        .let { if (it > 0) Date(it) else null },
                    gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                    birthday = cursor.getLong(cursor.getColumnIndexOrThrow("birthday"))
                        .let { if (it > 0) Date(it) else null }
                )
            )
        }

        for (i in 0 until contacts.size) {
            if (i == 0 || contacts[i].firstName?.first()
                    ?.toLowerCase() != contacts[i - 1].firstName?.first()?.toLowerCase()
            ) {
                contacts[i].isFirstOfChar = true
            }
        }
        cursor.close()
        return contacts
    }

    fun updateContact(contact: Contact): Int {
        val values = ContentValues().apply {
            put("first_name", contact.firstName)
            put("last_name", contact.lastName)
            put("email", contact.email)
            put("phone", contact.phone)
            put("company", contact.company)
            put("address", contact.address)
            put("created_at", contact.createdAt?.time)
            put("gender", contact.gender)
            put("birthday", contact.birthday?.time)
        }
        return writableDatabase.update(TABLE_CONTACT, values, "$COL_ID = ?", arrayOf(contact.id.toString()))
    }

    fun deleteContact(id: Int): Int {
        return writableDatabase.delete(TABLE_CONTACT, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun getContactById(id: Int): Contact? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CONTACT,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        var contact: Contact? = null
        if (cursor.moveToFirst()) {
            contact = Contact(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                company = cursor.getString(cursor.getColumnIndexOrThrow("company")),
                address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    .let { if (it > 0) Date(it) else null },
                gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                birthday = cursor.getLong(cursor.getColumnIndexOrThrow("birthday"))
                    .let { if (it > 0) Date(it) else null }
            )
        }
        cursor.close()
        return contact
    }

    fun countContacts(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CONTACT", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun search(query: String, column: String): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val db = readableDatabase

        val (selection, selectionArgs, orderBy) = when (column) {
            "name" -> Triple(
                "first_name LIKE ? OR last_name LIKE ?",
                arrayOf("%$query%", "%$query%"),
                "first_name ASC, last_name ASC"
            )
            "first_name", "last_name", "email", "phone", "company", "address", "gender" -> Triple(
                "$column LIKE ?",
                arrayOf("%$query%"),
                "$column ASC"
            )
            else -> throw IllegalArgumentException("Invalid column name: $column")
        }

        val cursor = db.query(
            TABLE_CONTACT,
            null,
            selection,
            selectionArgs,
            null,
            null,
            orderBy
        )

        while (cursor.moveToNext()) {
            contacts.add(
                Contact(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                    lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                    company = cursor.getString(cursor.getColumnIndexOrThrow("company")),
                    address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                        .let { if (it > 0) Date(it) else null },
                    gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                    birthday = cursor.getLong(cursor.getColumnIndexOrThrow("birthday"))
                        .let { if (it > 0) Date(it) else null }
                )
            )
        }
        cursor.close()
        return contacts
    }

    fun sortContacts(column: String, order: String): List<Contact> {
        if (!VALID_CONTACT_COLUMNS.contains(column)) {
            throw IllegalArgumentException("Invalid column name: $column. Must be one of $VALID_CONTACT_COLUMNS")
        }
        if (order.uppercase() !in setOf("ASC", "DESC")) {
            throw IllegalArgumentException("Invalid sort order: $order. Must be 'ASC' or 'DESC'")
        }

        val contacts = mutableListOf<Contact>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CONTACT,
            null,
            null,
            null,
            null,
            null,
            "$column $order"
        )

        while (cursor.moveToNext()) {
            contacts.add(
                Contact(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                    lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                    email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                    company = cursor.getString(cursor.getColumnIndexOrThrow("company")),
                    address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                        .let { if (it > 0) Date(it) else null },
                    gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                    birthday = cursor.getLong(cursor.getColumnIndexOrThrow("birthday"))
                        .let { if (it > 0) Date(it) else null }
                )
            )
        }
        cursor.close()
        return contacts
    }

    fun insertNote(note: Note): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", note.title)
            put("content", note.content)
            put("comment", note.comment)
            put("contact_id", note.contactId)
            put("date_time", note.dateTime?.time ?: System.currentTimeMillis())
        }
        return db.insert(TABLE_NOTES, null, values)
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            null,
            null,
            null,
            null,
            "date_time DESC"
        )

        while (cursor.moveToNext()) {
            notes.add(
                Note(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    comment = cursor.getString(cursor.getColumnIndexOrThrow("comment")),
                    contactId = cursor.getInt(cursor.getColumnIndexOrThrow("contact_id")),
                    dateTime = Date(cursor.getLong(cursor.getColumnIndexOrThrow("date_time")))
                )
            )
        }
        cursor.close()
        return notes
    }

    fun getNoteById(id: Int): Note? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        var note: Note? = null
        if (cursor.moveToFirst()) {
            note = Note(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                comment = cursor.getString(cursor.getColumnIndexOrThrow("comment")),
                contactId = cursor.getInt(cursor.getColumnIndexOrThrow("contact_id")),
                dateTime = Date(cursor.getLong(cursor.getColumnIndexOrThrow("date_time")))
            )
        }
        cursor.close()
        return note
    }

    fun updateNote(note: Note): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", note.title)
            put("content", note.content)
            put("comment", note.comment)
            put("contact_id", note.contactId)
            put("date_time", note.dateTime?.time ?: System.currentTimeMillis())
        }
        return db.update(TABLE_NOTES, values, "$COL_ID = ?", arrayOf(note.id.toString()))
    }

    fun deleteNote(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_NOTES, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun getNoteByContactId(contactId: Int): List<Note> {
        val notes = mutableListOf<Note>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "contact_id = ?",
            arrayOf(contactId.toString()),
            null,
            null,
            "date_time DESC"
        )

        while (cursor.moveToNext()) {
            notes.add(
                Note(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    comment = cursor.getString(cursor.getColumnIndexOrThrow("comment")),
                    contactId = cursor.getInt(cursor.getColumnIndexOrThrow("contact_id")),
                    dateTime = Date(cursor.getLong(cursor.getColumnIndexOrThrow("date_time")))
                )
            )
        }
        cursor.close()
        return notes
    }

    fun insertEvent(event: Event): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", event.title)
            put("content", event.content)
            put("date_time", event.dateTime?.time)
            put("id_note", event.noteId)
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    fun getEventByNoteId(noteId: Int): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "id_note = ?",
            arrayOf(noteId.toString()),
            null,
            null,
            "date_time DESC"
        )

        while (cursor.moveToNext()) {
            events.add(
                Event(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    dateTime = cursor.getLong(cursor.getColumnIndexOrThrow("date_time"))
                        .let { if (it > 0) Date(it) else null },
                    noteId = cursor.getInt(cursor.getColumnIndexOrThrow("id_note"))
                )
            )
        }
        cursor.close()
        return events
    }

    fun insertSchedule(schedule: Schedule): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", schedule.title)
            put("content", schedule.content)
            put("type", schedule.type)
            put("date_time", schedule.dateTime?.time)
        }
        return db.insert(TABLE_SCHEDULE, null, values)
    }

    fun updateSchedule(schedule: Schedule): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", schedule.title)
            put("content", schedule.content)
            put("type", schedule.type)
            put("date_time", schedule.dateTime?.time)
        }
        return db.update(TABLE_SCHEDULE, values, "$COL_ID = ?", arrayOf(schedule.id.toString()))
    }

    fun getAllSchedules(): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SCHEDULE,
            null,
            null,
            null,
            null,
            null,
            "id DESC"
        )

        while (cursor.moveToNext()) {
            schedules.add(
                Schedule(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content")) ?: "",
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    dateTime = cursor.getLong(cursor.getColumnIndexOrThrow("date_time"))
                        .let { Date(it) }
                )
            )
        }
        cursor.close()
        return schedules
    }

    fun insertScheduleContact(scheduleContact: ScheduleContact): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("contact_name", scheduleContact.contactName)
            put("schedule_id", scheduleContact.scheduleId)
            put("contact_id", scheduleContact.contactId)
        }
        return db.insert(TABLE_SCHEDULE_CONTACT, null, values)
    }

    fun searchSchedules(searchParam: String): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        val db = readableDatabase

        val query = """
        SELECT * FROM $TABLE_SCHEDULE
        WHERE 
            title LIKE ? OR
            content LIKE ? OR
            type LIKE ? OR
            strftime('%d/%m/%Y', date_time / 1000, 'unixepoch') LIKE ? OR
            strftime('%H:%M:%S', date_time / 1000, 'unixepoch') LIKE ?
        ORDER BY id DESC
        """

        val likeParam = "%$searchParam%"
        val cursor = db.rawQuery(query, arrayOf(likeParam, likeParam, likeParam, likeParam, likeParam))

        while (cursor.moveToNext()) {
            val schedule = Schedule(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                dateTime = Date(cursor.getLong(cursor.getColumnIndexOrThrow("date_time")))
            )
            schedules.add(schedule)
        }
        cursor.close()
        return schedules
    }

    fun getContactsByScheduleId(scheduleId: Int): List<Contact> {
        val db = readableDatabase
        val contacts = mutableListOf<Contact>()

        val query = """
        SELECT c.* FROM $TABLE_CONTACT c
        INNER JOIN $TABLE_SCHEDULE_CONTACT sc ON c.$COL_ID = sc.contact_id
        WHERE sc.schedule_id = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(scheduleId.toString()))

        while (cursor.moveToNext()) {
            val contact = Contact(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                company = cursor.getString(cursor.getColumnIndexOrThrow("company")),
                address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    .let { if (it > 0) Date(it) else null },
                gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                birthday = cursor.getLong(cursor.getColumnIndexOrThrow("birthday"))
                    .let { if (it > 0) Date(it) else null }
            )
            contacts.add(contact)
        }
        cursor.close()
        return contacts
    }

    fun updateScheduleContact(scheduleId: Int, newContacts: List<Contact>, isNew: Boolean? = false) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (isNew == false) db.delete(
                TABLE_SCHEDULE_CONTACT,
                "schedule_id = ?",
                arrayOf(scheduleId.toString())
            )

            val values = ContentValues()
            for (contact in newContacts) {
                values.clear()
                values.put("schedule_id", scheduleId)
                values.put("contact_id", contact.id)
                values.put("contact_name", "${contact.firstName} ${contact.lastName ?: ""}".trim())
                db.insert(TABLE_SCHEDULE_CONTACT, null, values)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getScheduleById(id: Int): Schedule? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SCHEDULE,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        var schedule: Schedule? = null
        if (cursor.moveToFirst()) {
            schedule = Schedule(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                dateTime = Date(cursor.getLong(cursor.getColumnIndexOrThrow("date_time")))
            )
        }
        cursor.close()
        return schedule
    }

    fun deleteSchedule(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_SCHEDULE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun updateListEventByNoteId(noteId: Int, updatedEvents: List<Event>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_EVENTS, "id_note = ?", arrayOf(noteId.toString()))

            for (event in updatedEvents) {
                val values = ContentValues().apply {
                    put("title", event.title)
                    put("content", event.content)
                    put("date_time", event.dateTime?.time ?: System.currentTimeMillis())
                    put("id_note", noteId)
                }
                db.insert(TABLE_EVENTS, null, values)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getScheduleContactsByContactId(contactId: Int): List<ScheduleContact> {
        val scheduleContacts = mutableListOf<ScheduleContact>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SCHEDULE_CONTACT,
            null,
            "contact_id = ?",
            arrayOf(contactId.toString()),
            null,
            null,
            null
        )

        while (cursor.moveToNext()) {
            scheduleContacts.add(
                ScheduleContact(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    contactName = cursor.getString(cursor.getColumnIndexOrThrow("contact_name")),
                    scheduleId = cursor.getInt(cursor.getColumnIndexOrThrow("schedule_id")),
                    contactId = cursor.getInt(cursor.getColumnIndexOrThrow("contact_id"))
                )
            )
        }
        cursor.close()
        return scheduleContacts
    }

    private fun addContactToDevice(name: String, phone: String, email: String): String? {
        val ops = ArrayList<ContentProviderOperation>()

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                )
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                .withValue(
                    ContactsContract.CommonDataKinds.Email.TYPE,
                    ContactsContract.CommonDataKinds.Email.TYPE_WORK
                )
                .build()
        )

        try {
            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val rawContactUri = results[0].uri
            return rawContactUri?.lastPathSegment
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun updateContactById(contactId: String, name: String, newPhone: String, newEmail: String) {
        val ops = ArrayList<ContentProviderOperation>()

        ops.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        val phoneCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
            null
        )
        var rawContactId: String? = null
        phoneCursor?.use {
            if (it.moveToFirst()) {
                rawContactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID))
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build()
                )
            }
        }
        if (rawContactId == null && newPhone.isNotBlank()) {
            val rawContactCursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )
            rawContactCursor?.use {
                if (it.moveToFirst()) {
                    rawContactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                            .build()
                    )
                }
            }
        }

        val emailCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
            null
        )
        emailCursor?.use {
            if (it.moveToFirst()) {
                rawContactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID))
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(contactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, newEmail)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                        .build()
                )
            }
        }
        if (rawContactId == null && newEmail.isNotBlank()) {
            val rawContactCursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )
            rawContactCursor?.use {
                if (it.moveToFirst()) {
                    rawContactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, newEmail)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                            .build()
                    )
                }
            }
        }

        try {
            if (ops.isNotEmpty()) {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            } else {
                Toast.makeText(context, "No updates applied for contact ID $contactId", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error updating contact: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteContactByPhone(phoneNumber: String) {

        val normalizedPhone = phoneNumber.replace("[^0-9+]".toRegex(), "")

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

        try {
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(contactId)
                    )
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error deleting contact: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getCallLogStatistics(timeRange: TimeRange): List<CallLogStats> {
        val stats = mutableListOf<CallLogStats>()
        val db = readableDatabase
        val calendar = Calendar.getInstance()

        val (selection, selectionArgs) = when (timeRange) {
            TimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                "cl.call_time >= ?" to arrayOf(startOfDay.toString())
            }
            TimeRange.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis
                "cl.call_time >= ?" to arrayOf(startOfWeek.toString())
            }
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                "cl.call_time >= ?" to arrayOf(startOfMonth.toString())
            }
            TimeRange.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfYear = calendar.timeInMillis
                "cl.call_time >= ?" to arrayOf(startOfYear.toString())
            }
            TimeRange.ALL -> "" to emptyArray()
        }

        val query = """
        SELECT 
            COALESCE(c.first_name || ' ' || c.last_name, cl.phone) AS contact_name,
            SUM(cl.duration) AS total_duration,
            COUNT(*) AS call_count
        FROM $TABLE_CALL_LOG cl
        LEFT JOIN $TABLE_CONTACT c ON cl.contact_id = c.$COL_ID
        ${if (selection.isNotEmpty()) "WHERE $selection" else ""}
        GROUP BY cl.contact_id, cl.phone
        ORDER BY total_duration DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, selectionArgs)

        cursor?.use {
            while (it.moveToNext()) {
                val contactName = it.getString(it.getColumnIndexOrThrow("contact_name")) ?: "Unknown"
                val totalDuration = it.getLong(it.getColumnIndexOrThrow("total_duration"))
                val callCount = it.getInt(it.getColumnIndexOrThrow("call_count"))

                stats.add(
                    CallLogStats(
                        contactName = contactName,
                        totalDuration = totalDuration,
                        callCount = callCount
                    )
                )
            }
        }
        cursor.close()
        return stats
    }

    fun getScheduleStatistics(timeRange: TimeRange): List<ScheduleStats> {
        val stats = mutableListOf<ScheduleStats>()
        val db = readableDatabase
        val calendar = Calendar.getInstance()

        val (selection, selectionArgs) = when (timeRange) {
            TimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                "s.date_time >= ?" to arrayOf(startOfDay.toString())
            }
            TimeRange.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis
                "s.date_time >= ?" to arrayOf(startOfWeek.toString())
            }
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                "s.date_time >= ?" to arrayOf(startOfMonth.toString())
            }
            TimeRange.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfYear = calendar.timeInMillis
                "s.date_time >= ?" to arrayOf(startOfYear.toString())
            }
            TimeRange.ALL -> "" to emptyArray()
        }

        val query = """
        SELECT 
            COALESCE(c.first_name || ' ' || c.last_name, sc.contact_name) AS contact_name,
            COUNT(*) AS schedule_count
        FROM $TABLE_SCHEDULE_CONTACT sc
        INNER JOIN $TABLE_SCHEDULE s ON sc.schedule_id = s.$COL_ID
        LEFT JOIN $TABLE_CONTACT c ON sc.contact_id = c.$COL_ID
        ${if (selection.isNotEmpty()) "WHERE $selection" else ""}
        GROUP BY sc.contact_id, sc.contact_name
        ORDER BY schedule_count DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, selectionArgs)

        cursor?.use {
            while (it.moveToNext()) {
                val contactName = it.getString(it.getColumnIndexOrThrow("contact_name")) ?: "Unknown"
                val scheduleCount = it.getInt(it.getColumnIndexOrThrow("schedule_count"))

                stats.add(
                    ScheduleStats(
                        contactName = contactName,
                        scheduleCount = scheduleCount
                    )
                )
            }
        }
        cursor.close()
        return stats
    }
}