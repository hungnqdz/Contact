package com.contact_app.contact

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private val adapter by lazy {
        NoteAdapter(this)
    }
    val listNote = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        viewBinding.apply {
            listItem.adapter = adapter
            listNote.clear()
            listNote.addAll(dbHelper.getNoteByContactId(contact.id ?: 0))
            adapter.submitList(listNote)
            contactBinding = contact
            sizeNote = listNote.size
            btnBack.setOnClickListener {
                finish()
            }

            btnAdd.setOnTouchListener(dragging)
            btnAdd.setOnClickListener {
                startActivityForResultCompat(AddNoteActivity::class.java, requestCode = 2, "contact" to contact)
            }
        }
    }

    override fun onItemClicked(item: Note) {
        super.onItemClicked(item)
        startActivityForResultCompat(AddNoteActivity::class.java, requestCode = 2, "note" to item, "contact" to contact)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 2) && resultCode == RESULT_OK) {
            val result = data?.getBooleanExtra("isUpdate", false)
            if (result == true) {
                listNote.clear()
                listNote.addAll(dbHelper.getNoteByContactId(contact.id ?: 0))
                adapter.submitList(listNote)
                viewBinding.sizeNote = listNote.size
            }
        }
    }
}