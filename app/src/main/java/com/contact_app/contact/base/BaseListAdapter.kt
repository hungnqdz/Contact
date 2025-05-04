package com.contact_app.contact.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.contact_app.contact.BR

open class BaseListAdapter<Item : Any>(
    private val onItemClick: OnItemClickListener<Item>? = null,
    private val onItemLongClickListener: OnLongClickListener<Item>? = null,
    callBack: DiffUtil.ItemCallback<Item>,
    @LayoutRes private val layoutId: Int
) : ListAdapter<Item, BaseViewHolder<ViewDataBinding>>(
    callBack
) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<ViewDataBinding> {
        return BaseViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                layoutId,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ViewDataBinding>, position: Int) {
        val item: Item? = getItem(position)
        holder.binding.apply {
            item?.apply {
                setVariable(BR.item, item)
                setVariable(BR.position,position)
                onBindData(holder.binding, item, position)
            }
            onItemClick?.apply {
                setVariable(BR.onItemClick, this)
            }
            onItemLongClickListener?.apply {
                setVariable(BR.onItemLongClick,this)
            }
            executePendingBindings()
        }

    }

    protected open fun onBindData(viewBinding: ViewDataBinding, item: Item, position: Int) {}

    override fun submitList(list: List<Item>?) {
        if (this.currentList == list && list.isNotEmpty()) {
            notifyDataSetChanged()
        } else {
            super.submitList(list)
        }
    }
}

interface OnItemClickListener<Item : Any> {
    fun onItemClicked(item: Item) {}

    fun onItemPositionClicked(position: Int) {}
    fun onItemWithPositionClicked(item: Item, position: Int) {}
}

interface OnLongClickListener<Item : Any> {
    fun onItemLongClicked(item: Item) {}

    fun onItemPositionLongClicked(position: Int):Boolean {return true}
}


open class BaseViewHolder<ViewBinding : ViewDataBinding>(
    val binding: ViewBinding
) : RecyclerView.ViewHolder(binding.root)