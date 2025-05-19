package com.contact_app.contact.adapter

import androidx.recyclerview.widget.DiffUtil
import com.contact_app.contact.R
import com.contact_app.contact.base.BaseListAdapter
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.model.Note
import com.contact_app.contact.model.Schedule

class NoteAdapter (
    private val onClickListener: OnItemClickListener<Note>,
    private val layoutItem: Int = R.layout.item_note
) :
    BaseListAdapter<Note>(
        onItemClick = onClickListener,
        callBack = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem == newItem
            }
        }, layoutId = layoutItem
    ) {
}