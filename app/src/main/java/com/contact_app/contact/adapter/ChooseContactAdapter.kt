package com.contact_app.contact.adapter

import android.util.Log
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import com.contact_app.contact.R
import com.contact_app.contact.base.BaseListAdapter
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.databinding.ItemChooseContactBinding
import com.contact_app.contact.model.Contact

class ChooseContactAdapter(
    private val onClickListener: OnItemClickListener<Contact>,
    private val callBackChecked: CallBackChecked? = null
) : BaseListAdapter<Contact>(
    onItemClick = onClickListener,
    callBack = object : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }, layoutId = R.layout.item_choose_contact
) {
    private val preCheckedItems = mutableSetOf<Contact>()

    override fun onBindData(viewBinding: ViewDataBinding, item: Contact, position: Int) {
        super.onBindData(viewBinding, item, position)
        val binding = viewBinding as ItemChooseContactBinding
        binding.apply {
            btnCheck.isChecked = preCheckedItems.contains(item.copy(isFirstOfChar = false))
            btnCheck.setOnCheckedChangeListener { compoundButton, b ->
                callBackChecked?.checkedChange(b, item, position)
            }
        }

    }

    fun setPreCheckedItems(contacts: List<Contact>) {
        preCheckedItems.clear()
        contacts.map {
            it.isFirstOfChar = false
        }
        preCheckedItems.addAll(contacts)
        notifyDataSetChanged()
    }
}

interface CallBackChecked {
    fun checkedChange(isChecked: Boolean, item: Contact, position: Int)
}