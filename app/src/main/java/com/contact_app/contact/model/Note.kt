package com.contact_app.contact.model

data class Note(
    val id: Int? = null,
    val title: String?,
    val content: String?,
    val comment: String?,
    val contactId: Int?
)