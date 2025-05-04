package com.contact_app.contact

import android.os.Bundle
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityAddContactBinding
import com.contact_app.contact.databinding.ActivityDetailContactBinding
import com.contact_app.contact.model.Contact

class FormContactActivity : BaseActivity<ActivityAddContactBinding>() {
    override val layoutId: Int
        get() = R.layout.activity_add_contact

    private lateinit var contact: Contact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        viewBinding.apply {
            if (this@FormContactActivity.contact.id == null) tvHeader.text =
                "Liên hệ mới" else tvHeader.text = "Sửa liên hệ"
            contact = this@FormContactActivity.contact
        }
    }
}