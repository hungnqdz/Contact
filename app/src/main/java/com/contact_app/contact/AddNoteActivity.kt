package com.contact_app.contact

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.base.BaseListAdapter
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.databinding.ActivityAddNoteBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact
import com.contact_app.contact.model.Event
import com.contact_app.contact.model.Note

class AddNoteActivity : BaseActivity<ActivityAddNoteBinding>(), OnItemClickListener<Event> {
    override val layoutId: Int
        get() = R.layout.activity_add_note
    var note = Note()
    val listEvent = mutableListOf<Event>()
    var contact = Contact()
    val adapter by lazy {
        BaseListAdapter<Event>(
            onItemClick = this,
            callBack = object : DiffUtil.ItemCallback<Event>() {
                override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
                    return oldItem == newItem
                }

            },
            layoutId = R.layout.item_event
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        note = intent.getSerializableExtra("note") as? Note ?: Note()
        contact = intent.getSerializableExtra("contact") as? Contact ?: Contact()
        if (note.id != null) {
            listEvent.clear()
            listEvent.addAll(dbHelper.getEventByNoteId(noteId = note.id!!))
            viewBinding.noteBinding = note
        }
        viewBinding.apply {
            listItem.adapter = adapter
            adapter.submitList(listEvent)
        }

        viewBinding.btnBack.setOnClickListener {
            finish()
        }
        viewBinding.apply {
            btnAddEvent.setOnClickListener {
                startActivityForResultCompat(AddEventActivity::class.java, 2)
            }
        }

        viewBinding.btnSave.setOnClickListener {
            validateAndSaveNote()
        }
    }

    private fun validateAndSaveNote() {
        viewBinding.apply {
            val title = edTitle.text.toString().trim()
            val content = edContent.text.toString().trim()
            val comment = edComment.text.toString().trim()

            when {
                title.isEmpty() -> {
                    showSnackbar("Vui lòng nhập tiêu đề")
                    return
                }

                content.isEmpty() -> {
                    showSnackbar("Vui lòng nhập nội dung")
                    return
                }

                comment.isEmpty() -> {
                    showSnackbar("Vui lòng nhập bình luận")
                    return
                }
            }
            saveNote(title, content, comment)
        }

    }

    private fun saveNote(title: String, content: String, comment: String) {
        if (note.id != null) {
            dbHelper.updateNote(note.copy(title = title, content = content, comment = comment))
            dbHelper.updateListEventByNoteId(noteId = note.id!!, listEvent)
        } else {
            val noteId = dbHelper.insertNote(
                Note(
                    title = title,
                    content = content,
                    comment = comment,
                    contactId = contact.id
                )
            ).toInt()
            dbHelper.updateListEventByNoteId(noteId, listEvent)
        }
        showSnackbar("Ghi chú đã được lưu thành công")
        finishActivityWithResult(value = true)
    }

    private fun showSnackbar(message: String) {
        Toast.makeText(this@AddNoteActivity, message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 2) && resultCode == RESULT_OK) {
            val result = data?.getSerializableExtra("event") as? Event
            if (result != null) {
                if (listEvent.none { it.id == result.id }) {
                    listEvent.add(result)
                } else {
                    listEvent.replaceAll { if (it.id == result.id) result else it }
                }
                adapter.submitList(listEvent)

            }

        }
    }

    override fun onItemClicked(item: Event) {
        super.onItemClicked(item)
        startActivityForResultCompat(AddEventActivity::class.java, 2, "event" to item)
    }


}