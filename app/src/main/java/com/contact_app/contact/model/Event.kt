package com.contact_app.contact.model

import java.util.Date

data class Event(
    val id: Int? = null,
    val content: String?,
    val dateTime: Date?,
    val noteId: Int?
)