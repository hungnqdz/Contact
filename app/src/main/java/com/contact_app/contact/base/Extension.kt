package com.contact_app.contact.base

import java.text.SimpleDateFormat
import java.util.*

fun dateToString(date: Date?): String? {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return date?.let { formatter.format(it) }
}

fun stringToDate(dateStr: String?): Date? {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return try {
        dateStr?.let { formatter.parse(it) }
    } catch (e: Exception) {
        null
    }
}
