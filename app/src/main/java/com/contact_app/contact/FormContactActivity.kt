package com.contact_app.contact

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityAddContactBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact
import java.util.Calendar


class FormContactActivity : BaseActivity<ActivityAddContactBinding>() {
    override val layoutId: Int get() = R.layout.activity_add_contact
    private lateinit var contact: Contact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        val dbHelper = ContactDatabaseHelper.getInstance(this)
        viewBinding.apply {
            // Set header text
            tvHeader.text =
                if (this@FormContactActivity.contact.id == null) getString(R.string.new_contact) else getString(
                    R.string.edit_contact
                )
            if (this@FormContactActivity.contact.id != null) contact =
                this@FormContactActivity.contact


            // Birthday picker
            tvBirthday.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this@FormContactActivity,
                    { _, year, month, dayOfMonth ->
                        val selectedDate =
                            String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                        tvBirthday.setText(selectedDate)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            // Back button
            btnBack.setOnClickListener { finish() }

            btnSave.setOnClickListener {
                if (validateForm()) {
                    if (this@FormContactActivity.contact.id == null) {
                        Log.d("TAGG","${dbHelper.insertContact(this@FormContactActivity.contact)}")
                        val resultIntent = Intent()
                        resultIntent.putExtra("isCreate", true);
                        setResult(RESULT_OK, resultIntent);
                    } else {
                        dbHelper.updateContact(this@FormContactActivity.contact)
                        val resultIntent = Intent()
                        resultIntent.putExtra("isUpdate", true);
                        setResult(RESULT_OK, resultIntent);
                    }
                    finish()
                    Toast.makeText(
                        this@FormContactActivity,
                        "Dữ liệu đã được cập nhật",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        viewBinding.apply {
            // Phone validation
            val phone = phoneEditText.text?.toString()?.trim()
            if (phone.isNullOrEmpty()) {
                phoneInputLayout.error = getString(R.string.phone_required)
                isValid = false
            } else if (!phone.matches(Regex("^[0-9]{10,12}$"))) {
                phoneInputLayout.error = getString(R.string.invalid_phone)
                isValid = false
            } else {
                phoneInputLayout.error = null
                this@FormContactActivity.contact.phone = phone
            }

            // First name validation
            val firstName = firstNameEditText.text?.toString()?.trim()
            if (firstName.isNullOrEmpty()) {
                firstNameInputLayout.error = getString(R.string.first_name_required)
                isValid = false
            } else {
                firstNameInputLayout.error = null
                this@FormContactActivity.contact.firstName = firstName
            }

            // Last name (optional)
            this@FormContactActivity.contact.lastName = lastNameEditText.text?.toString()?.trim()

            // Email validation
            val email = emailEditText.text?.toString()?.trim()
            if (!email.isNullOrEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                    .matches()
            ) {
                emailInputLayout.error = getString(R.string.invalid_email)
                isValid = false
            } else {
                emailInputLayout.error = null
                this@FormContactActivity.contact.email = email
            }

            // Birthday validation
            val birthday = tvBirthday.text?.toString()?.trim()
            if (!birthday.isNullOrEmpty() && !birthday.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))) {
                birthdayInputLayout.error = getString(R.string.invalid_date)
                isValid = false
            } else {
                birthdayInputLayout.error = null
                this@FormContactActivity.contact.setBirthContact(birthday)
            }
            val gender = genderEditText.text?.toString()?.trim()
            Log.d("CONTACT", "$gender")
            if (gender != "Nam" && gender != "Nữ") {
                genderInputLayout.error = "Vui lòng nhập Nam hoặc Nữ"
                isValid = false
            } else {
                genderEditText.error = null
                this@FormContactActivity.contact.setGenderText(gender)
            }
            this@FormContactActivity.contact.address = addressEditText.text?.toString()?.trim()
            this@FormContactActivity.contact.company = companyEditText.text?.toString()?.trim()
            Log.d("CONTACT", "${this@FormContactActivity.contact}")
            if (!isValid) {
                Toast.makeText(
                    this@FormContactActivity,
                    getString(R.string.validation_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return isValid
    }
}