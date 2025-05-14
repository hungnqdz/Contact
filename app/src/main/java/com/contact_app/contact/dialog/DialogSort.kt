package com.contact_app.contact.dialog

import android.os.Bundle
import android.view.View
import com.contact_app.contact.R
import com.contact_app.contact.base.BaseDialog
import com.contact_app.contact.databinding.DialogSortBinding

class DialogSort() : BaseDialog<DialogSortBinding>() {
    override val layoutId: Int
        get() = R.layout.dialog_sort

    var callBack: DialogSortCallBack? = null
    var column: SortColumn? = null
    var order: Order? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.apply {
            radioGroup.setOnCheckedChangeListener { group, checkedId ->
                column = when (checkedId) {
                    R.id.btn_radio_fname -> SortColumn.FIRST_NAME
                    R.id.btn_radio_lname -> SortColumn.LAST_NAME
                    R.id.btn_radio_create_at -> SortColumn.CREATE_AT
                    R.id.btn_radio_company -> SortColumn.COMPANY
                    else -> null
                }
            }
            radioGroupOrder.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.btn_radio_asc -> order = Order.ASC
                    R.id.btn_radio_desc -> order = Order.DESC
                    else -> null
                }
            }

            btnAccept.setOnClickListener {
                callBack?.onConfirm(column,order)
            }
            btnCancel.setOnClickListener {
                callBack?.onCancel()
            }
        }
    }

}

interface DialogSortCallBack {
    fun onCancel()
    fun onConfirm(sortBy: SortColumn?, order: Order?)
}

enum class SortColumn(val column: String) {
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    CREATE_AT("created_at"),
    COMPANY("company");

    companion object {
        fun getTypeUser(key: String?) = entries.firstOrNull { it.column == key } ?: FIRST_NAME
    }
}

enum class Order(val order: String) {
    ASC("asc"), DESC("desc")
}

