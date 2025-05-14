package com.contact_app.contact.base

import android.view.MotionEvent
import android.view.View
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

val dragging = object : View.OnTouchListener {
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Record initial position and touch point
                initialX = v.x
                initialY = v.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Calculate new position
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                v.x = initialX + deltaX
                v.y = initialY + deltaY

                return true
            }
            MotionEvent.ACTION_UP -> {
                if (event.eventTime - event.downTime < 200) {
                    v.performClick()
                }
                return true
            }
        }
        return false
    }
}
