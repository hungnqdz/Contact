package com.contact_app.contact.model

import com.contact_app.contact.base.dateToString
import java.io.Serializable
import java.util.Date

data class Note(
    val id: Int? = null,
    val title: String? = null,
    val content: String? = null,
    val comment: String? = null,
    var contactId: Int? = null,
    val dateTime: Date? = Date()
):Serializable{
    fun getDate(): String? {
        return dateToString(this.dateTime)
    }
}