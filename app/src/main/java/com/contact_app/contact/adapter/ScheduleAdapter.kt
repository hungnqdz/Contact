package com.contact_app.contact.adapter

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import com.contact_app.contact.R
import com.contact_app.contact.base.BaseListAdapter
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.base.OnLongClickListener
import com.contact_app.contact.databinding.ItemScheduleBinding
import com.contact_app.contact.model.Contact
import com.contact_app.contact.model.Schedule

class ScheduleAdapter(
    private val onClickListener: OnItemClickListener<Schedule>,
    private val callbackScheduleAdapter: CallbackScheduleAdapter? = null
) :
    BaseListAdapter<Schedule>(
        onItemClick = onClickListener,
        callBack = object : DiffUtil.ItemCallback<Schedule>() {
            override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
                return oldItem == newItem
            }


        }, layoutId = R.layout.item_schedule
    ) {


    override fun onBindData(viewBinding: ViewDataBinding, item: Schedule, position: Int) {
        super.onBindData(viewBinding, item, position)
        val binding = viewBinding as ItemScheduleBinding
        binding.apply {
            btnDelete.setOnClickListener {
                callbackScheduleAdapter?.onDelete(item, position)
            }
        }
    }
}

interface CallbackScheduleAdapter {
    fun onDelete(item: Schedule, position: Int)
}