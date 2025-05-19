package com.contact_app.contact.model

data class ScheduleContact(
    val id: Int? = null,
    val contactName: String?,
    var scheduleId: Int?,
    var contactId: Int?
)