package com.contact_app.contact.fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.contact_app.contact.ChooseContactActivity
import com.contact_app.contact.R
import com.contact_app.contact.base.BaseBottomSheet
import com.contact_app.contact.databinding.BottomLayoutScheduleBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact
import com.contact_app.contact.model.Schedule
import com.contact_app.contact.model.ScheduleContact
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class BottomSheetScheduleForm : BaseBottomSheet<BottomLayoutScheduleBinding>() {
    override val layoutId: Int
        get() = R.layout.bottom_layout_schedule
    var schedule = Schedule(type = "Offline", title = "", content = "")
    var callBack: CallbackSchedule? = null
    var listChosenContact = mutableListOf<Contact>()
    private val REQUEST_CODE_CHOOSE_CONTACT = 1001
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var tempDateTime: Calendar? =
        Calendar.getInstance()
    lateinit var dbHelper: ContactDatabaseHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = ContactDatabaseHelper.getInstance(requireContext())
        if (schedule.id != null) {
            listChosenContact = dbHelper.getContactsByScheduleId(schedule.id!!) as MutableList
            viewBinding.tvContactName.text = generateTextName()
            tempDateTime?.time = schedule.dateTime ?: Calendar.getInstance().time
        }

        viewBinding.apply {
            // Bind schedule to layout
            scheduleBinding = this@BottomSheetScheduleForm.schedule
            radioGroupType.check(if (schedule.type == "Online") R.id.radioOnline else R.id.radioOffice)

            // Initialize date/time display
            if (schedule.dateTime != null) {
                tvDate.text = schedule.getDate()
                tvTime.text = schedule.getTime()
            }

            // Date picker
            tvDate.setOnClickListener {
                showDatePicker()
            }

            // Time picker
            tvTime.setOnClickListener {
                showTimePicker()
            }

            // Contact selection
            btnContact.setOnClickListener {
                val intent = Intent(requireActivity(), ChooseContactActivity::class.java).apply {
                    putExtra("CHOSEN_CONTACTS", listChosenContact as Serializable)
                }
                startActivityForResult(intent, REQUEST_CODE_CHOOSE_CONTACT)
            }

            // Save button
            btnSave.setOnClickListener {
                if (validateForm()) {
                    // Update schedule object
                    schedule.title = etTitle.text.toString()
                    schedule.content = etContent.text.toString()
                    schedule.type =
                        if (radioGroupType.checkedRadioButtonId == R.id.radioOnline) "online" else "offline"
                    schedule.dateTime = tempDateTime?.time
                    onSaveSchedule()
                    Toast.makeText(requireContext(), "Lưu thành công", Toast.LENGTH_SHORT).show()
                    callBack?.onSave()
                    dismiss()
                }
            }

            // Cancel button
            btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE_CONTACT && resultCode == Activity.RESULT_OK) {
            data?.getSerializableExtra("SELECTED_CONTACTS")?.let { selectedContacts ->
                listChosenContact.clear()
                listChosenContact.addAll(selectedContacts as List<Contact>)
                viewBinding.tvContactName.text = generateTextName()
            }
        }
    }

    private fun generateTextName(): String {
        return listChosenContact.joinToString(", ") { it.getFullName() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (tempDateTime != null) {
            calendar.time = tempDateTime!!.time
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            tempDateTime = (tempDateTime ?: Calendar.getInstance()).apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay)
            }
            viewBinding.tvDate.text = dateFormat.format(tempDateTime!!.time)
        }, year, month, day).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000 // Prevent past dates
            show()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        if (tempDateTime != null) {
            calendar.time = tempDateTime!!.time
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            tempDateTime = (tempDateTime ?: Calendar.getInstance()).apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
            }
            viewBinding.tvTime.text = timeFormat.format(tempDateTime!!.time)
        }, hour, minute, true).show()
    }

    private fun validateForm(): Boolean {
        viewBinding.apply {
            // Title validation
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show()
                return false
            }
            if (title.length > 100) {
                Toast.makeText(
                    requireContext(),
                    "Tiêu đề không được vượt quá 100 ký tự",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            // DateTime validation
            if (tempDateTime == null || tvDate.text.isNullOrBlank() || tvTime.text.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Vui lòng chọn ngày và giờ", Toast.LENGTH_SHORT)
                    .show()
                return false
            }

            // Content validation
            val content = etContent.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập nội dung", Toast.LENGTH_SHORT)
                    .show()
                return false
            }
            if (content.length > 500) {
                Toast.makeText(
                    requireContext(),
                    "Nội dung không được vượt quá 500 ký tự",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            // Participants validation
            if (listChosenContact.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Vui lòng chọn ít nhất một người tham gia",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            // Meeting type validation
            if (radioGroupType.checkedRadioButtonId == -1) {
                Toast.makeText(requireContext(), "Vui lòng chọn kiểu họp", Toast.LENGTH_SHORT)
                    .show()
                return false
            }

            return true
        }
    }

    private fun onSaveSchedule() {
        if (schedule.id == null) {
            val idSchedule = dbHelper.insertSchedule(schedule).toInt()
            if (idSchedule != -1) {
                dbHelper.updateScheduleContact(scheduleId = idSchedule, listChosenContact, isNew = true)
            }
        } else {
            if (dbHelper.updateSchedule(schedule.copy(title = viewBinding.etTitle.text.toString(), content = viewBinding.etContent.text.toString())).toInt() != -1) {
                dbHelper.updateScheduleContact(scheduleId = schedule.id!!, listChosenContact)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding.apply {
            tvTime.text = ""
            tvContactName.text = ""
            tvDate.text = ""
            etTitle.setText("")
            listChosenContact.clear()
            schedule = Schedule(type = "Offline", title = "", content = "")
        }
    }
}

interface CallbackSchedule {
    fun onSave()
}