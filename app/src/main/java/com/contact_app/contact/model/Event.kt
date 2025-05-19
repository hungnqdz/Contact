package com.contact_app.contact.model

import com.contact_app.contact.base.dateToString
import com.contact_app.contact.base.stringToDate
import java.io.Serializable
import java.util.Date

data class Event(
    val id: Int? = null,
    var content: String? = null,
    var dateTime: Date? = null,
    var title: String? = null,
    var noteId: Int? = null
) : Serializable {
    fun getDate(): String? {
        return dateToString(this.dateTime)
    }

    fun setDate(date: String?) {
        this.dateTime = stringToDate(date)
    }
}