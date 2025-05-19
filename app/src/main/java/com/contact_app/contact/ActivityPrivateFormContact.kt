package com.contact_app.contact

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityPrivateFormContactBinding
import com.contact_app.contact.db.CipherContactDatabaseHelper
import com.contact_app.contact.model.Contact
import java.util.Calendar

class ActivityPrivateFormContact : BaseActivity<ActivityPrivateFormContactBinding>() {
    override val layoutId: Int get() =
        R.layout.activity_private_form_contact
    private lateinit var contact: Contact
    lateinit var dbPrivateHelper: CipherContactDatabaseHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0
        dbPrivateHelper = CipherContactDatabaseHelper.getInstance(this)

        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        viewBinding.apply {
            if (this@ActivityPrivateFormContact.contact.id != null) contact =
                this@ActivityPrivateFormContact.contact

            tvBirthday.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this@ActivityPrivateFormContact,
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

            btnBack.setOnClickListener { finish() }

            btnSave.setOnClickListener {
                if (validateForm()) {
                    dbPrivateHelper.updateContact(this@ActivityPrivateFormContact.contact)
                    val resultIntent = Intent()
                    resultIntent.putExtra("isUpdate", true);
                    setResult(RESULT_OK, resultIntent);
                    Toast.makeText(
                        this@ActivityPrivateFormContact,
                        "Dữ liệu đã được cập nhật",
                        Toast.LENGTH_SHORT
                    ).show()
                    finishActivityWithResult("isUpdate",true)
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        viewBinding.apply {
            val phone = phoneEditText.text?.toString()?.trim()
            if (phone.isNullOrEmpty()) {
                phoneInputLayout.error = getString(R.string.phone_required)
                isValid = false
            } else if (!phone.matches(Regex("^[0-9]{10,12}$"))) {
                phoneInputLayout.error = getString(R.string.invalid_phone)
                isValid = false
            } else {
                phoneInputLayout.error = null
                this@ActivityPrivateFormContact.contact.phone = phone
            }

            // First name validation
            val firstName = firstNameEditText.text?.toString()?.trim()
            if (firstName.isNullOrEmpty()) {
                firstNameInputLayout.error = getString(R.string.first_name_required)
                isValid = false
            } else {
                firstNameInputLayout.error = null
                this@ActivityPrivateFormContact.contact.firstName = firstName
            }

            // Last name (optional)
            this@ActivityPrivateFormContact.contact.lastName =
                lastNameEditText.text?.toString()?.trim()

            // Email validation
            val email = emailEditText.text?.toString()?.trim()
            if (!email.isNullOrEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                    .matches()
            ) {
                emailInputLayout.error = getString(R.string.invalid_email)
                isValid = false
            } else {
                emailInputLayout.error = null
                this@ActivityPrivateFormContact.contact.email = email
            }

            // Birthday validation
            val birthday = tvBirthday.text?.toString()?.trim()
            if (!birthday.isNullOrEmpty() && !birthday.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))) {
                birthdayInputLayout.error = getString(R.string.invalid_date)
                isValid = false
            } else {
                birthdayInputLayout.error = null
                this@ActivityPrivateFormContact.contact.setBirthContact(birthday)
            }
            val gender = genderEditText.text?.toString()?.trim()
            Log.d("CONTACT", "$gender")
            if (gender != "Nam" && gender != "Nữ") {
                genderInputLayout.error = "Vui lòng nhập Nam hoặc Nữ"
                isValid = false
            } else {
                genderEditText.error = null
                this@ActivityPrivateFormContact.contact.gender = gender
            }
            this@ActivityPrivateFormContact.contact.address =
                addressEditText.text?.toString()?.trim()
            this@ActivityPrivateFormContact.contact.company =
                companyEditText.text?.toString()?.trim()
            Log.d("CONTACT", "${this@ActivityPrivateFormContact.contact}")
            if (!isValid) {
                Toast.makeText(
                    this@ActivityPrivateFormContact,
                    getString(R.string.validation_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return isValid
    }
}