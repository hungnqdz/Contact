package com.contact_app.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityDetailContactBinding
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

}