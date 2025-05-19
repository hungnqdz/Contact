package com.contact_app.contact

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityPrivateContactDetailBinding
import com.contact_app.contact.db.CipherContactDatabaseHelper
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact

class ActivityPrivateDetailContact : BaseActivity<ActivityPrivateContactDetailBinding>() {
    override val layoutId: Int
        get() = R.layout.activity_private_contact_detail

    private lateinit var contact: Contact
    private lateinit var dbPrivateHelper: CipherContactDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        dbPrivateHelper = CipherContactDatabaseHelper.getInstance(this)

        super.onCreate(savedInstanceState)
        viewBinding.apply {
            contactPrivate = this@ActivityPrivateDetailContact.contact
            btnBack.setOnClickListener {
                finish()
            }
            btnEdit.setOnClickListener {
                val intent =
                    Intent(this@ActivityPrivateDetailContact, ActivityPrivateFormContact::class.java)
                intent.putExtra("contact", this@ActivityPrivateDetailContact.contact)
                startActivityForResult(intent, 10);
            }
            btnCall.setOnClickListener {
                val phone = this@ActivityPrivateDetailContact.contact.phone
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phone")
                startActivity(intent)
            }

            btnAddToPrivate.setOnClickListener {
                this@ActivityPrivateDetailContact.contact.id?.let { id ->
                    dbPrivateHelper.exportContact(
                        dbHelper,
                        id
                    )
                }
                Toast.makeText(
                    this@ActivityPrivateDetailContact,
                    "Đã xuất khỏi kho ẩn!",
                    Toast.LENGTH_SHORT
                ).show()
                finishActivityWithResult("isUpdate", true)
            }

            btnSendEmail.setOnClickListener {
                val email = this@ActivityPrivateDetailContact.contact.email
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.setData(Uri.parse("mailto:$email")) // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, email)
                intent.putExtra(Intent.EXTRA_SUBJECT, "")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
            btnNote.setOnClickListener {
                val intent = Intent(this@ActivityPrivateDetailContact, ActivityPrivateNote::class.java)
                intent.putExtra("contact", contact)
                startActivity(intent)
            }
            btnDelete.setOnClickListener {
                this@ActivityPrivateDetailContact.contact.id?.let { idContact ->
                    if (dbPrivateHelper.deleteContact(idContact) == 1) {
                        Toast.makeText(
                            this@ActivityPrivateDetailContact,
                            "Xóa liên hệ thành công",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                finishActivityWithResult(value = true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10 && resultCode == RESULT_OK) {
            val result = data?.getBooleanExtra("isUpdate", false)
            if (result == true) {
                val dbPrivateHelper = CipherContactDatabaseHelper.getInstance(this)
                contact = contact.id?.let { dbPrivateHelper.getContactById(it) } ?: contact
                viewBinding.contactPrivate = contact
                finishActivityWithResult(value = true, isFinish = false)
            }
        }
    }

}