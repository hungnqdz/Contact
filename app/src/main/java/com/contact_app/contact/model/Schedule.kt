package com.contact_app.contact.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Schedule(
    val id: Int? = null,
    var title: String? = null,
    var content: String? = null,
    var type: String? = null,
    var dateTime: Date? = null
) {
    fun getDate(): String {
        return if (dateTime != null) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateTime) else ""
    }

    fun getTime(): String {
        return if (dateTime != null) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(dateTime) else ""
    }
}