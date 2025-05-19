package com.contact_app.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityDetailContactBinding
import com.contact_app.contact.db.CipherContactDatabaseHelper
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact
import com.contact_app.contact.sharepref.SharePrefHelper
import com.google.gson.Gson


class DetailContactActivity : BaseActivity<ActivityDetailContactBinding>() {
    override val layoutId: Int
        get() = R.layout.activity_detail_contact

    private lateinit var contact: Contact
    lateinit var sharePrefHelper: SharePrefHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        sharePrefHelper = SharePrefHelper(applicationContext, Gson())

        super.onCreate(savedInstanceState)
        viewBinding.apply {
            contact = this@DetailContactActivity.contact
            btnBack.setOnClickListener {
                finish()
            }
            btnEdit.setOnClickListener {
                val intent = Intent(this@DetailContactActivity, FormContactActivity::class.java)
                intent.putExtra("contact", this@DetailContactActivity.contact)
                startActivityForResult(intent, 1);
            }
            btnCall.setOnClickListener {
                val phone = this@DetailContactActivity.contact.phone
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phone")
                startActivity(intent)
            }

            btnAddToPrivate.setOnClickListener {
                val isSetPassword =
                    sharePrefHelper.get("isCreatePassword", Boolean::class.java, false)
                if (isSetPassword == true) {
                    showPasswordDialog {
                        if (!CipherContactDatabaseHelper.isPassphraseValid(
                                this@DetailContactActivity,
                                it
                            )
                        ) {
                            Toast.makeText(
                                this@DetailContactActivity,
                                "Sai mật khẩu!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@showPasswordDialog
                        }
                        CipherContactDatabaseHelper.getOrCreatePassphrase(
                            it
                        )
                        val dbPrivateContact =
                            CipherContactDatabaseHelper.getInstance(this@DetailContactActivity)
                        this@DetailContactActivity.contact.id?.let { id ->
                            dbPrivateContact.encryptContact(
                                dbHelper,
                                id
                            )
                        }
                        Toast.makeText(
                            this@DetailContactActivity,
                            "Đã thêm vào kho ẩn!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finishActivityWithResult("isUpdate", true)
                    }
                } else {
                    Toast.makeText(
                        this@DetailContactActivity,
                        "Bạn chưa thiết lập mật khẩu, vui lòng vào phần menu ở màn hình chính và vào kho ẩn để thiết lập!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            btnSendEmail.setOnClickListener {
                val email = this@DetailContactActivity.contact.email
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.setData(Uri.parse("mailto:$email")) // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, email)
                intent.putExtra(Intent.EXTRA_SUBJECT, "")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
            btnNote.setOnClickListener {
                val intent = Intent(this@DetailContactActivity, NoteActivity::class.java)
                intent.putExtra("contact", contact)
                startActivity(intent)
            }
            btnDelete.setOnClickListener {
                this@DetailContactActivity.contact.id?.let { idContact ->
                    dbHelper.deleteContactWithSync(idContact)
                    Toast.makeText(
                        this@DetailContactActivity,
                        "Xóa liên hệ thành công",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                finishActivityWithResult(value = true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val result = data?.getBooleanExtra("isUpdate", false)
            if (result == true) {
                contact = contact.id?.let { dbHelper.getContactById(it) } ?: contact
                viewBinding.contact = contact
                finishActivityWithResult(value = true, isFinish = false)
            }
        }
    }

    private fun showPasswordDialog(onSuccess: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nhập mật khẩu")

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Mật khẩu"
        }

        builder.setView(input)

        builder.setPositiveButton("Xác nhận") { dialog, _ ->
            val enteredPassword = input.text.toString()

            if (enteredPassword.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu.", Toast.LENGTH_SHORT).show()
            } else {
                onSuccess.invoke(enteredPassword)
            }
        }

        builder.setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


}