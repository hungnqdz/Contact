package com.contact_app.contact.base

import android.view.MotionEvent
import android.view.View
import com.contact_app.contact.model.TimeRange
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
var PASS_PHASE= ""
fun getTimeRangeLabel(timeRange: TimeRange): String {
    val calendar = Calendar.getInstance()
    val endCalendar = Calendar.getInstance()
    val format = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    return when (timeRange) {
        TimeRange.TODAY -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)

            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)
            endCalendar.set(Calendar.SECOND, 59)

            "${format.format(calendar.time)} - ${format.format(endCalendar.time)}"
        }

        TimeRange.THIS_WEEK -> {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)

            endCalendar.time = calendar.time
            endCalendar.add(Calendar.DAY_OF_WEEK, 6)
            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)

            "${format.format(calendar.time)} - ${format.format(endCalendar.time)}"
        }

        TimeRange.THIS_MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)

            endCalendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)

            "${format.format(calendar.time)} - ${format.format(endCalendar.time)}"
        }

        TimeRange.THIS_YEAR -> {
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)

            endCalendar.set(Calendar.MONTH, Calendar.DECEMBER)
            endCalendar.set(Calendar.DAY_OF_MONTH, 31)
            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)

            "${format.format(calendar.time)} - ${format.format(endCalendar.time)}"
        }

        TimeRange.ALL -> {
            "Tất cả"
        }
    }
}

