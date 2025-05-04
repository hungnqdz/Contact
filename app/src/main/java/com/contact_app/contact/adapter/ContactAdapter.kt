package com.contact_app.contact.adapter

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import com.contact_app.contact.R
import com.contact_app.contact.base.BaseListAdapter
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.base.OnLongClickListener
import com.contact_app.contact.model.Contact
import com.contact_app.contact.BR

class ContactAdapter(
    private val onClickListener: OnItemClickListener<Contact>,
    private val onLongClickListener: OnLongClickListener<Contact>
) :
    BaseListAdapter<Contact>(
        onItemClick = onClickListener,
        callBack = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
                return oldItem == newItem
            }

        }, layoutId = R.layout.item_contact
    ) {

    override fun onBindData(viewBinding: ViewDataBinding, item: Contact, position: Int) {
        super.onBindData(viewBinding, item, position)
        viewBinding.apply {
            this.root.setOnLongClickListener {
                onLongClickListener.onItemPositionLongClicked(position)
                false
            }
            this.root.isLongClickable = true
            root.isClickable = true
            setVariable(BR.keySearchFirstName, keySearchFirstName)
            setVariable(BR.keySearchEmail,keySearchEmail)
            setVariable(BR.keySearchPhone, keySearchPhone)
            setVariable(BR.keySearchCompany, keySearchCompany)
        }
    }

    private var keySearchFirstName = ""
    private var keySearchPhone = ""
    private var keySearchCompany = ""
    private var keySearchEmail = ""

    fun setKeySearch(text: String?, searchColumn: SearchColumn?) {
        keySearchFirstName = ""
        keySearchCompany = ""
        keySearchPhone = ""
        keySearchEmail = ""
        when (searchColumn) {
            SearchColumn.NAME -> this.keySearchFirstName = text.toString()
            SearchColumn.PHONE -> this.keySearchPhone = text.toString()
            SearchColumn.COMPANY -> this.keySearchCompany = text.toString()
            SearchColumn.EMAIL -> keySearchEmail = text.toString()
            else -> {}
        }
    }

}

enum class SearchColumn(val column: String) {
    NAME("name"), COMPANY("company"), PHONE("phone"),EMAIL("email");
}