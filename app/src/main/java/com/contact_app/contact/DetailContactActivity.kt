package com.contact_app.contact

import android.R.attr
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityDetailContactBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact


class DetailContactActivity : BaseActivity<ActivityDetailContactBinding>(){
    override val layoutId: Int
        get() = R.layout.activity_detail_contact

    private lateinit var contact: Contact

    override fun onCreate(savedInstanceState: Bundle?) {
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        super.onCreate(savedInstanceState)
        viewBinding.apply {
            contact = this@DetailContactActivity.contact
            btnBack.setOnClickListener {
                finish()
            }
            btnEdit.setOnClickListener {
                val intent = Intent(this@DetailContactActivity,FormContactActivity::class.java)
                intent.putExtra("contact",this@DetailContactActivity.contact)
                startActivityForResult(intent, 1);
            }
            btnCall.setOnClickListener {
                val phone = this@DetailContactActivity.contact.phone
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phone")
                startActivity(intent)
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
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val result = data?.getBooleanExtra("isUpdate",false)
            if (result == true){
                val dbHelper = ContactDatabaseHelper.getInstance(this)
                contact = contact.id?.let { dbHelper.getContactById(it) } ?: contact
                viewBinding.contact = contact
                val resultIntent = Intent()
                resultIntent.putExtra("isUpdate", true);
                setResult(RESULT_OK, resultIntent)
            }
        }
    }


}