package com.contact_app.contact

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityNoteBinding
import com.contact_app.contact.model.Contact

class NoteActivity : BaseActivity<ActivityNoteBinding>() {
    override val layoutId: Int
        get() = R.layout.activity_note
    private lateinit var contact: Contact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        viewBinding.apply {

        }
    }
}