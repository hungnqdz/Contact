package com.contact_app.contact

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityAddEventBinding
import com.contact_app.contact.model.Event
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEventActivity : BaseActivity<ActivityAddEventBinding>() {
    override val layoutId: Int
        get() = R.layout.activity_add_event

    private val calendar = Calendar.getInstance()
    private var selectedDate: String = ""
    private var isEditMode = false
    var event = Event()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        event = intent.getSerializableExtra("event") as? Event ?: Event()
        isEditMode = event.id != null

        viewBinding.apply {
            event = this@AddEventActivity.event
            Log.d("event","${event?.getDate()}")
            if (isEditMode) {
                selectedDate = this@AddEventActivity.event.getDate() ?: ""

                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    calendar.time = sdf.parse(selectedDate) ?: Calendar.getInstance().time
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }


            layoutTime.setOnClickListener {
                showDatePicker()
            }

            btnSave.setOnClickListener {
                saveDraftEvent()
            }

            btnBack.setOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = sdf.format(calendar.time)
                viewBinding.tvTime.text = selectedDate
            }, year, month, day
        )

        datePickerDialog.show()
    }

    private fun saveDraftEvent() {
        viewBinding.apply {
            val title = tvTitle.text.toString().trim()
            val content = tvContent.text.toString().trim()

            if (selectedDate.isEmpty() || title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this@AddEventActivity, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            if (isEditMode) {
                this@AddEventActivity.event.title = title
                this@AddEventActivity.event.content = content
                this@AddEventActivity.event.setDate(selectedDate)
            } else {
                this@AddEventActivity.event = Event(title = title, content = content)
                this@AddEventActivity.event.setDate(selectedDate)
            }

            finishActivityWithResult("event", this@AddEventActivity.event)
        }
    }

}
