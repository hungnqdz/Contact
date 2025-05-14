package com.contact_app.contact.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.contact_app.contact.model.Contact
import com.contact_app.contact.model.Event
import com.contact_app.contact.model.Note
import com.contact_app.contact.model.Schedule
import com.contact_app.contact.model.ScheduleContact
import java.util.Calendar
import java.util.Date

class ContactDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "contact_database.db"
        private const val DATABASE_VERSION = 2 // Incremented version due to schema change

        // Table names
        private const val TABLE_CONTACT = "contact"
        private const val TABLE_NOTES = "notes"
        private const val TABLE_EVENTS = "events"
        private const val TABLE_SCHEDULE = "schedule"
        private const val TABLE_SCHEDULE_CONTACT = "schedule_contact"

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
        private var instance: ContactDatabaseHelper? = null

        fun getInstance(context: Context): ContactDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: ContactDatabaseHelper(context.applicationContext).also {
                    instance = it
                    it.initDatabase()
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Contact table
        db.execSQL(
            """
            CREATE TABLE $TABLE_CONTACT (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                first_name TEXT NOT NULL,
                last_name TEXT,
                email TEXT,
                phone TEXT NOT NULL UNIQUE,
                company TEXT,
                address TEXT,
                created_at INTEGER,
                gender TEXT NOT NULL,
                birthday INTEGER NOT NULL
            )
            """
        )

        // Create Notes table
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                content TEXT,
                comment TEXT,
                contact_id INTEGER,
                FOREIGN KEY (contact_id) REFERENCES $TABLE_CONTACT($COL_ID) ON DELETE CASCADE
            )
            """
        )

        // Create Events table
        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT,
                date_time INTEGER,
                id_note INTEGER,
                FOREIGN KEY (id_note) REFERENCES $TABLE_NOTES($COL_ID) ON DELETE CASCADE
            )
            """
        )

        // Create Schedule table with date_time
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
            // Create a new table with date_time
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

            // Copy data from old schedule table, combining date and time
            db.execSQL(
                """
                INSERT INTO temp_schedule (id, title, content, type, date_time)
                SELECT id, title, content, type, date AS date_time
                FROM $TABLE_SCHEDULE
                """
            )

            // Drop old table and rename new one
            db.execSQL("DROP TABLE $TABLE_SCHEDULE")
            db.execSQL("ALTER TABLE temp_schedule RENAME TO $TABLE_SCHEDULE")
        }
        initDatabase()
    }

    fun initDatabase() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CONTACT", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()

        if (count > 0) return

        val writeDb = writableDatabase
        writeDb.beginTransaction()
        try {
            // Create sample birthdays
            val calendar = Calendar.getInstance()
            calendar.set(1990, Calendar.JANUARY, 15)
            val birthday1 = calendar.time
            calendar.set(1985, Calendar.MARCH, 22)
            val birthday2 = calendar.time
            calendar.set(1992, Calendar.JULY, 10)
            val birthday3 = calendar.time
            calendar.set(1988, Calendar.NOVEMBER, 5)
            val birthday4 = calendar.time

            // Insert sample Contacts
            val contact1Id = insertContact(
                Contact(
                    firstName = "Haha",
                    lastName = "Funny",
                    email = "haha@lol.com",
                    phone = "1234567890",
                    company = "Comedy Inc",
                    address = "123 Laugh St",
                    createdAt = Date(),
                    gender = "Male",
                    birthday = birthday1
                )
            )
            val contact2Id = insertContact(
                Contact(
                    firstName = "Hehe",
                    lastName = "Joker",
                    email = "hehe@giggle.com",
                    phone = "0987654321",
                    company = "Joke Ltd",
                    address = "456 Chuckle Ave",
                    createdAt = Date(),
                    gender = "Female",
                    birthday = birthday2
                )
            )
            val contact3Id = insertContact(
                Contact(
                    firstName = "chee",
                    lastName = "Smiley",
                    email = "teehee@smile.com",
                    phone = "6677889900",
                    company = "Smile LLC",
                    address = "321 Grin Blvd",
                    createdAt = Date(),
                    gender = "Female",
                    birthday = birthday3
                )
            )
            val contact4Id = insertContact(
                Contact(
                    firstName = "Teehee",
                    lastName = "Smiley",
                    email = "teehee@smile.com",
                    phone = "6677889900",
                    company = "Smile LLC",
                    address = "321 Grin Blvd",
                    createdAt = Date(),
                    gender = "Female",
                    birthday = birthday4
                )
            )
            val contact5Id = insertContact(
                Contact(
                    firstName = "Lol",
                    lastName = "Chuckler",
                    email = "lol@chuckle.com",
                    phone = "5544332211",
                    company = "Chuckle Co",
                    address = "654 Tickle Ln",
                    createdAt = Date(),
                    gender = null,
                    birthday = null
                )
            )

            // Insert sample Notes
            val note1Id = insertNote(
                Note(
                    title = "Hahaha Meeting",
                    content = "Laughing plans",
                    comment = "Super fun!",
                    contactId = contact1Id.toInt()
                )
            )
            val note2Id = insertNote(
                Note(
                    title = "Hahaha Party",
                    content = "Giggle fest",
                    comment = "LOL!",
                    contactId = contact2Id.toInt()
                )
            )
            val note3Id = insertNote(
                Note(
                    title = "Hehe Seminar",
                    content = "Chuckle topics",
                    comment = "Hilar!",
                    contactId = contact3Id.toInt()
                )
            )
            val note4Id = insertNote(
                Note(
                    title = "Hoho Workshop",
                    content = "Joke strategies",
                    comment = "ROFL!",
                    contactId = contact4Id.toInt()
                )
            )
            val note5Id = insertNote(
                Note(
                    title = "Teehee Rally",
                    content = "Silly ideas",
                    comment = "Whee!",
                    contactId = contact5Id.toInt()
                )
            )

            // Insert sample Events
            val eventCalendar = Calendar.getInstance()
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            insertEvent(
                Event(
                    content = "Hahaha Workshop",
                    dateTime = eventCalendar.time,
                    noteId = note1Id.toInt()
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            insertEvent(
                Event(
                    content = "Giggle Event",
                    dateTime = eventCalendar.time,
                    noteId = note2Id.toInt()
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            insertEvent(
                Event(
                    content = "Chuckle Fest",
                    dateTime = eventCalendar.time,
                    noteId = note3Id.toInt()
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            insertEvent(
                Event(
                    content = "Joke Jam",
                    dateTime = eventCalendar.time,
                    noteId = note4Id.toInt()
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            insertEvent(
                Event(
                    content = "Silly Symposium",
                    dateTime = eventCalendar.time,
                    noteId = note5Id.toInt()
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            insertEvent(
                Event(
                    content = "LOL Lecture",
                    dateTime = eventCalendar.time,
                    noteId = note1Id.toInt()
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            insertEvent(
                Event(
                    content = "Hehe Hangout",
                    dateTime = eventCalendar.time,
                    noteId = note2Id.toInt()
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            insertEvent(
                Event(
                    content = "Hoho Huddle",
                    dateTime = eventCalendar.time,
                    noteId = note3Id.toInt()
                )
            )

            // Insert sample Schedules
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            val schedule1Id = insertSchedule(
                Schedule(
                    title = "Hahaha Conference",
                    content = "Annual comedy conference",
                    type = "online",
                    dateTime = eventCalendar.time
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            val schedule2Id = insertSchedule(
                Schedule(
                    title = "LOL Meetup",
                    content = "Local comedian meetup",
                    type = "offline",
                    dateTime = eventCalendar.time
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            val schedule3Id = insertSchedule(
                Schedule(
                    title = "Hehe Briefing",
                    content = "Comedy workshop briefing",
                    type = "online",
                    dateTime = eventCalendar.time
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            val schedule4Id = insertSchedule(
                Schedule(
                    title = "Hoho Forum",
                    content = "Joke writing forum",
                    type = "offline",
                    dateTime = eventCalendar.time
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            val schedule5Id = insertSchedule(
                Schedule(
                    title = "Teehee Summit",
                    content = "Comedy summit",
                    type = "online",
                    dateTime = eventCalendar.time
                )
            )
            eventCalendar.add(Calendar.DAY_OF_MONTH, 1)
            val schedule6Id = insertSchedule(
                Schedule(
                    title = "Chuckle Conclave",
                    content = "Improv comedy conclave",
                    type = "offline",
                    dateTime = eventCalendar.time
                )
            )

            // Insert sample ScheduleContacts
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Haha Funny",
                    scheduleId = schedule1Id.toInt(),
                    contactId = contact1Id.toInt()
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Hehe Joker",
                    scheduleId = schedule2Id.toInt(),
                    contactId = contact2Id.toInt()
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Hoho Giggles",
                    scheduleId = schedule3Id.toInt(),
                    contactId = contact3Id.toInt()
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    scheduleId = schedule4Id.toInt(),
                    contactId = contact4Id.toInt(),
                    contactName = "Teehee Smiley"
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Lol Chuckler",
                    scheduleId = schedule5Id.toInt(),
                    contactId = contact5Id.toInt()
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Haha Funny",
                    scheduleId = schedule6Id.toInt(),
                    contactId = contact1Id.toInt()
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Hehe Joker",
                    scheduleId = schedule1Id.toInt(),
                    contactId = contact2Id.toInt()
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Hoho Giggles",
                    scheduleId = schedule2Id.toInt(),
                    contactId = contact3Id.toInt()
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Teehee Smiley",
                    scheduleId = schedule3Id.toInt(),
                    contactId = contact4Id.toInt()
                )
            )
            insertScheduleContact(
                ScheduleContact(
                    contactName = "Lol Chuckler",
                    scheduleId = schedule4Id.toInt(),
                    contactId = contact5Id.toInt()
                )
            )

            writeDb.setTransactionSuccessful()
        } finally {
            writeDb.endTransaction()
        }
    }

    // Contact CRUD operations
    fun insertContact(contact: Contact): Long {
        val db = writableDatabase
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
        return db.insert(TABLE_CONTACT, null, values)
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
        val db = writableDatabase
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
        val db = writableDatabase
        return db.delete(TABLE_CONTACT, "$COL_ID = ?", arrayOf(id.toString()))
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

    // Note CRUD operations
    fun insertNote(note: Note): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", note.title)
            put("content", note.content)
            put("comment", note.comment)
            put("contact_id", note.contactId)
        }
        return db.insert(TABLE_NOTES, null, values)
    }

    // Event CRUD operations
    fun insertEvent(event: Event): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("content", event.content)
            put("date_time", event.dateTime?.time)
            put("id_note", event.noteId)
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    // Schedule CRUD operations
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

    // ScheduleContact CRUD operations
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

    fun updateScheduleContact(scheduleId: Int, newContacts: List<Contact>, isNew:Boolean? = false) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Xóa các liên kết contact cũ khỏi schedule
            if (isNew == false) db.delete(
                TABLE_SCHEDULE_CONTACT,
                "schedule_id = ?",
                arrayOf(scheduleId.toString())
            )

            // Thêm lại các contact mới từ danh sách Contact
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

}