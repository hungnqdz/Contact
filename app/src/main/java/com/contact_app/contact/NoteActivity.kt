package com.contact_app.contact

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.contact_app.contact.adapter.NoteAdapter
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.base.dragging
import com.contact_app.contact.databinding.ActivityNoteBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact
import com.contact_app.contact.model.Note

class NoteActivity : BaseActivity<ActivityNoteBinding>(), OnItemClickListener<Note> {
    override val layoutId: Int
        get() = R.layout.activity_note
    private lateinit var contact: Contact
    lateinit var dbHelper: ContactDatabaseHelper
    private val adapter by lazy {
        NoteAdapter(this)
    }
    val listNote = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = ContactDatabaseHelper.getInstance(this)
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        viewBinding.apply {
            listItem.adapter = adapter
            listNote.clear()
            listNote.addAll(dbHelper.getNoteByContactId(contact.id ?: 0))
            adapter.submitList(listNote)
            sizeNote = listNote.size
            btnBack.setOnClickListener {
                finish()
            }

            btnAdd.setOnTouchListener(dragging)
            btnAdd.setOnClickListener {

            }
        }
    }
}