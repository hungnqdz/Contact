package com.contact_app.contact.db

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.contact_app.contact.db.ContactDatabaseHelper.Companion
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import com.contact_app.contact.model.Contact
import com.contact_app.contact.model.Event
import com.contact_app.contact.model.Note
import com.contact_app.contact.model.Schedule
import com.contact_app.contact.model.ScheduleContact
import org.mindrot.jbcrypt.BCrypt
import java.util.Date
import java.util.Base64

class CipherContactDatabaseHelper private constructor(
    context: Context
) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "contact_database_cipher.db"
        private const val DATABASE_VERSION = 4

        // Table names
        private const val TABLE_CONTACT = "contact"
        private const val TABLE_NOTES = "notes"
        private const val TABLE_EVENTS = "events"
        private const val TABLE_SCHEDULE = "schedule"
        private const val TABLE_SCHEDULE_CONTACT = "schedule_contact"
        var PASS_PHASE = ""

        // Common column names
        private const val COL_ID = "id"

        // Valid columns for sorting
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
        private var instance: CipherContactDatabaseHelper? = null

        fun getInstance(context: Context): CipherContactDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: run {
                    SQLiteDatabase.loadLibs(context)
                    CipherContactDatabaseHelper(context).also {
                        instance = it
                        it.initDatabase()
                    }
                }
            }
        }

        fun getOrCreatePassphrase(passphrase: String): String {
            if (passphrase.isBlank() || passphrase.length < 8) {
                throw IllegalArgumentException("Password must be at least 8 characters long")
            }
            PASS_PHASE = generatePassword(passphrase)
            return PASS_PHASE
        }

        fun clearPassPhase() {
            PASS_PHASE = ""
        }

        fun isPassphraseValid(context: Context, passphrase: String): Boolean {
            SQLiteDatabase.loadLibs(context)
            return try {
                val db = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(DATABASE_NAME).absolutePath,
                    generatePassword(passphrase),
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CONTACT", null)
                cursor.moveToFirst()
                cursor.close()
                db.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        private fun generatePassword(passphrase: String): String {
            val base64 = Base64.getEncoder().encodeToString(passphrase.toByteArray())
            val saltBody = base64.take(22).padEnd(22, 'A')
            val cost = 12
            val salt = "\$2a\$${cost.toString().padStart(2, '0')}\$${saltBody}"
            val hashedPassphrase = BCrypt.hashpw(passphrase, salt)
            return hashedPassphrase
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

        // Create Notes table with date_time
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

        // Create Events table with title column
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

        // Create Schedule table
        db.execSQL(
            """
            CREATE TABLE $TABLE_SCHEDULE (
                $COL_ID INTEGER PRIMARY KEY,
                title TEXT,
                content TEXT,
                type TEXT CHECK(type IN ('online', 'offline')) NOT NULL,
                date_time INTEGER NOT NULL
            )
            """
        )

        // Create ScheduleContact table
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE temp_schedule (
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
                INSERT INTO temp_schedule (id, title, content, type, date_time)
                SELECT id, title, content, type, date AS date_time
                FROM $TABLE_SCHEDULE
                """
            )
            db.execSQL("DROP TABLE $TABLE_SCHEDULE")
            db.execSQL("ALTER TABLE temp_schedule RENAME TO $TABLE_SCHEDULE")
        }

        if (oldVersion < 3) {
            db.execSQL(
                """
                ALTER TABLE $TABLE_NOTES
                ADD COLUMN date_time INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                """
            )
        }

        if (oldVersion < 4) {
            db.execSQL(
                """
                ALTER TABLE $TABLE_EVENTS
                ADD COLUMN title TEXT
                """
            )
        }
    }

    override fun getWritableDatabase(passphrase: String): SQLiteDatabase {
        return super.getWritableDatabase(passphrase)
    }

    override fun getReadableDatabase(passphrase: String): SQLiteDatabase {
        return super.getReadableDatabase(passphrase)
    }


    fun initDatabase() {
        // No data initialization as per requirement
        val db = getReadableDatabase(PASS_PHASE)
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CONTACT", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
    }

    // Contact CRUD operations
    fun insertContact(contact: Contact): Long {
        val db = getWritableDatabase(PASS_PHASE)
        val values = ContentValues().apply {
            put(COL_ID, contact.id)
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
        return db.insert(TABLE_CONTACT, null, values)
    }

    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val db = getReadableDatabase(PASS_PHASE)
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
        val db = getWritableDatabase(PASS_PHASE)
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
        return db.update(TABLE_CONTACT, values, "$COL_ID = ?", arrayOf(contact.id.toString()))
    }

    fun deleteContact(id: Int): Int {
        val db = getWritableDatabase(PASS_PHASE)
        return db.delete(TABLE_CONTACT, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun getContactById(id: Int): Contact? {
        val db = getReadableDatabase(PASS_PHASE)
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
        val db = getReadableDatabase(PASS_PHASE)
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
        val db = getReadableDatabase(PASS_PHASE)

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
        val db = getReadableDatabase(PASS_PHASE)
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

    // Note CRUD operations
    fun insertNote(note: Note): Long {
        val db = getWritableDatabase(PASS_PHASE)
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
        val db = getReadableDatabase(PASS_PHASE)
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
        val db = getReadableDatabase(PASS_PHASE)
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
        val db = getWritableDatabase(PASS_PHASE)
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
        val db = getWritableDatabase(PASS_PHASE)
        return db.delete(TABLE_NOTES, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun getNoteByContactId(contactId: Int): List<Note> {
        val notes = mutableListOf<Note>()
        val db = getReadableDatabase(PASS_PHASE)
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

    // Event CRUD operations
    fun insertEvent(event: Event): Long {
        val db = getWritableDatabase(PASS_PHASE)
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
        val db = getReadableDatabase(PASS_PHASE)
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

    // Schedule CRUD operations
    fun insertSchedule(schedule: Schedule): Long {
        val db = getWritableDatabase(PASS_PHASE)
        val values = ContentValues().apply {
            put("id",schedule.id)
            put("title", schedule.title)
            put("content", schedule.content)
            put("type", schedule.type)
            put("date_time", schedule.dateTime?.time)
        }
        return db.insert(TABLE_SCHEDULE, null, values)
    }

    fun updateSchedule(schedule: Schedule): Int {
        val db = getWritableDatabase(PASS_PHASE)
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
        val db = getReadableDatabase(PASS_PHASE)
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

    // ScheduleContact CRUD operations
    fun insertScheduleContact(scheduleContact: ScheduleContact): Long {
        val db = getWritableDatabase(PASS_PHASE)
        val values = ContentValues().apply {
            put("contact_name", scheduleContact.contactName)
            put("schedule_id", scheduleContact.scheduleId)
            put("contact_id", scheduleContact.contactId)
        }
        return db.insert(TABLE_SCHEDULE_CONTACT, null, values)
    }

    fun searchSchedules(searchParam: String): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        val db = getReadableDatabase(PASS_PHASE)

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
        val cursor =
            db.rawQuery(query, arrayOf(likeParam, likeParam, likeParam, likeParam, likeParam))

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
        val db = getReadableDatabase(PASS_PHASE)
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

    fun updateScheduleContact(
        scheduleId: Int,
        newContacts: List<Contact>,
        isNew: Boolean? = false
    ) {
        val db = getWritableDatabase(PASS_PHASE)
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
                db.insert(TABLE_SCHEDULE_CONTACT, null, values)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getScheduleById(id: Int): Schedule? {
        val db = getReadableDatabase(PASS_PHASE)
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
        val db = getWritableDatabase(PASS_PHASE)
        return db.delete(TABLE_SCHEDULE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun updateListEventByNoteId(noteId: Int, updatedEvents: List<Event>) {
        val db = getWritableDatabase(PASS_PHASE)
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

    fun encryptContact(unencryptedDb: ContactDatabaseHelper, contactId: Int): Boolean {
        val unencryptedReadableDb = unencryptedDb.readableDatabase
        val encryptedWritableDb = getWritableDatabase(PASS_PHASE)

        encryptedWritableDb.beginTransaction()
        try {
            // Step 1: Get the contact
            val contact = unencryptedDb.getContactById(contactId)
                ?: return false // Contact not found

            val result = insertContact(contact)
            if (result == -1L) return false // Insertion failed

            val notes = unencryptedDb.getNoteByContactId(contactId)
            val noteIdMap = mutableMapOf<Int, Int>()
            for (note in notes) {
                val oldNoteId = note.id
                note.contactId = contact.id
                val newNoteId = insertNote(note)
                if (newNoteId == -1L) return false
                oldNoteId?.let {
                    noteIdMap[it] = newNoteId.toInt()
                }
            }

            // Step 4: Get and transfer events
            for (note in notes) {
                Log.d("NOTE","$note")
                val oldNoteId = note.id
                val events = oldNoteId?.let { unencryptedDb.getEventByNoteId(it) }
                if (events != null) {
                    for (event in events) {
                        event.noteId = noteIdMap[oldNoteId] ?: continue // Update note_id to new ID
                        val newEventId = insertEvent(event)
                        if (newEventId == -1L) return false
                    }
                }
            }

            // Step 5: Get and transfer schedule_contacts (and related schedules if needed)
            val scheduleContacts = unencryptedDb.getScheduleContactsByContactId(contactId)
            for (scheduleContact in scheduleContacts) {
                // Check if the schedule exists in encrypted database
                var schedule = scheduleContact.scheduleId?.let { getScheduleById(it) }
                if (schedule == null) {
                    // Copy schedule from unencrypted database
                    schedule = scheduleContact.scheduleId?.let { unencryptedDb.getScheduleById(it) }
                    if (schedule != null) {
                        if (schedule.id?.let { getScheduleById(it) } == null){
                            val newScheduleId = insertSchedule(schedule)
                            if (newScheduleId == -1L) return false
                            scheduleContact.scheduleId = newScheduleId.toInt()
                        }
                    } else {
                        continue // Skip if schedule not found
                    }
                }
                scheduleContact.contactId = contact.id // Update contact_id to new ID
                val newScheduleContactId = insertScheduleContact(scheduleContact)
                if (newScheduleContactId == -1L) return false
            }

            // Step 6: Delete data from unencrypted database
            unencryptedDb.writableDatabase.beginTransaction()
            try {
                unencryptedDb.deleteContactWithSync(contactId) // Cascades to notes and schedule_contacts
                unencryptedDb.writableDatabase.setTransactionSuccessful()
            } finally {
                unencryptedDb.writableDatabase.endTransaction()
            }

            encryptedWritableDb.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            encryptedWritableDb.endTransaction()
        }
    }

    fun exportContact(unencryptedDb: ContactDatabaseHelper, contactId: Int): Boolean {
        val unencryptedWritableDb = unencryptedDb.writableDatabase
        val encryptedWritableDb = getWritableDatabase(PASS_PHASE)
        unencryptedWritableDb.beginTransaction()
        try {
            val contact = getContactById(contactId)
                ?: return false

            val newContactId = unencryptedDb.insertContactWithSync(contact)

            val notes = getNoteByContactId(contactId)
            val noteIdMap = mutableMapOf<Int, Int>()
            for (note in notes) {
                Log.d("NOTE","$note")
                val oldNoteId = note.id
                note.contactId = newContactId?.toInt()
                val newNoteId = unencryptedDb.insertNote(note)
                if (newNoteId == -1L) return false
                oldNoteId?.let {
                    noteIdMap[it] = newNoteId.toInt()
                }
            }

            for (note in notes) {
                val oldNoteId = note.id
                val events = oldNoteId?.let { getEventByNoteId(it) }
                if (events != null) {
                    for (event in events) {
                        event.noteId = noteIdMap[oldNoteId] ?: continue // Update note_id to new ID
                        val newEventId = unencryptedDb.insertEvent(event)
                        if (newEventId == -1L) return false
                    }
                }
            }

            // Step 5: Get and transfer schedule_contacts (and related schedules if needed)
            val scheduleContacts = getContactsByScheduleId(contactId).flatMap { schedule ->
                schedule.id?.let {
                    getScheduleById(it)?.let { sched ->
                        sched.id?.let { schedId -> getScheduleContactsByScheduleId(schedId) }
                    }
                } ?: emptyList()
            }.filter { it.contactId == contactId }
            for (scheduleContact in scheduleContacts) {
                // Check if the schedule exists in unencrypted database
                var schedule = scheduleContact.scheduleId?.let { unencryptedDb.getScheduleById(it) }
                if (schedule == null) {
                    // Copy schedule from encrypted database
                    schedule = scheduleContact.scheduleId?.let { getScheduleById(it) }
                    if (schedule != null) {
                        val newScheduleId = unencryptedDb.insertSchedule(schedule)
                        if (newScheduleId == -1L) return false
                        scheduleContact.scheduleId = newScheduleId.toInt()
                    } else {
                        continue // Skip if schedule not found
                    }
                }
                scheduleContact.contactId = newContactId?.toInt() // Update contact_id to new ID
                val newScheduleContactId = unencryptedDb.insertScheduleContact(scheduleContact)
                if (newScheduleContactId == -1L) return false
            }

            // Step 6: Delete data from encrypted database
            encryptedWritableDb.beginTransaction()
            try {
                deleteContact(contactId) // Cascades to notes and schedule_contacts
                encryptedWritableDb.setTransactionSuccessful()
            } finally {
                encryptedWritableDb.endTransaction()
            }

            unencryptedWritableDb.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            return false
        } finally {
            unencryptedWritableDb.endTransaction()
        }
    }

    fun getScheduleContactsByScheduleId(scheduleId: Int): List<ScheduleContact> {
        val scheduleContacts = mutableListOf<ScheduleContact>()
        val db = getReadableDatabase(PASS_PHASE)
        val cursor = db.query(
            TABLE_SCHEDULE_CONTACT,
            null,
            "schedule_id = ?",
            arrayOf(scheduleId.toString()),
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
}