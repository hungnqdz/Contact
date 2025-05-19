package com.contact_app.contact

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import com.contact_app.contact.adapter.NoteAdapter
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.base.dragging
import com.contact_app.contact.databinding.ActivityNoteBinding
import com.contact_app.contact.databinding.ActivityPrivateNoteBinding
import com.contact_app.contact.db.CipherContactDatabaseHelper
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact
import com.contact_app.contact.model.Note

class ActivityPrivateNote : BaseActivity<ActivityPrivateNoteBinding>(), OnItemClickListener<Note> {
    override val layoutId: Int
        get() = R.layout.activity_private_note
    private lateinit var contact: Contact
    lateinit var dbPrivateHelper: CipherContactDatabaseHelper
    private val adapter by lazy {
        NoteAdapter(this, R.layout.item_private_note)
    }
    val listNote = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0
        dbPrivateHelper = CipherContactDatabaseHelper.getInstance(this)
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
                startActivityForResultCompat(ActivityPrivateAddNote::class.java, requestCode = 2, "contact" to contact)
            }
        }
    }

    override fun onItemClicked(item: Note) {
        super.onItemClicked(item)
        startActivityForResultCompat(ActivityPrivateAddNote::class.java, requestCode = 2, "note" to item, "contact" to contact)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 2) && resultCode == RESULT_OK) {
            val result = data?.getBooleanExtra("isUpdate", false)
            if (result == true) {
                listNote.clear()
                listNote.addAll(dbPrivateHelper.getNoteByContactId(contact.id ?: 0))
                adapter.submitList(listNote)
                viewBinding.sizeNote = listNote.size
            }
        }
    }
}