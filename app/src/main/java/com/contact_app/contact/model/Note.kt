package com.contact_app.contact.model

import com.contact_app.contact.base.dateToString
import java.util.Date

data class Note(
    val id: Int? = null,
    val title: String?,
    val content: String?,
    val comment: String?,
    val contactId: Int?,
    val dateTime: Date? = Date()
){
    fun getDate(): String? {
        return dateToString(this.dateTime)
    }
}