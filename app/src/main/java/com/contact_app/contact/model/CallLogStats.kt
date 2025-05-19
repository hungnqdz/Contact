package com.contact_app.contact.model

data class CallLogStats(
    val contactName: String,
    val totalDuration: Long,
    val callCount: Int // Số cuộc gọi
)
