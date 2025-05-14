package com.contact_app.contact.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.contact_app.contact.DetailContactActivity
import com.contact_app.contact.R
import com.contact_app.contact.adapter.CallbackScheduleAdapter
import com.contact_app.contact.adapter.ScheduleAdapter
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.base.dragging
import com.contact_app.contact.databinding.FragmentScheduleBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Schedule

class ListScheduleFragment : Fragment(), OnItemClickListener<Schedule> {
    lateinit var viewBinding: FragmentScheduleBinding
    private val adapter by lazy {
        ScheduleAdapter(this, object : CallbackScheduleAdapter {
            override fun onDelete(item: Schedule, position: Int) {
                item.id?.let {
                    if (dbHelper.deleteSchedule(it) == 1) {
                        removeItem(position)
                    }
                }
            }
        })
    }
    var scheduleList = mutableListOf<Schedule>()
    private val bottomSheetScheduleForm = BottomSheetScheduleForm()
    lateinit var dbHelper: ContactDatabaseHelper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::viewBinding.isInitialized) {
            viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_schedule, container, false)
            viewBinding.apply {
                root.isClickable = true
            }
        }
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = ContactDatabaseHelper.getInstance(requireContext())
        bottomSheetScheduleForm.callBack = object : CallbackSchedule {
            override fun onSave() {
                scheduleList = dbHelper.getAllSchedules().toMutableList()
                adapter.submitList(scheduleList)
            }
        }
        viewBinding.apply {
            adapter = this@ListScheduleFragment.adapter
            btnAdd.setOnTouchListener(dragging)
            searchView.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun afterTextChanged(p0: Editable?) {
                    if (!p0.isNullOrBlank()) this@ListScheduleFragment.adapter.submitList(
                        dbHelper.searchSchedules(
                            p0.toString().trim()
                        )
                    )
                }
            })
            btnAdd.setOnClickListener {
                bottomSheetScheduleForm.listChosenContact.clear()
                bottomSheetScheduleForm.schedule =
                    Schedule(type = "Offline", title = "", content = "")
                bottomSheetScheduleForm.show(parentFragmentManager, javaClass.name)
            }
        }
        scheduleList.clear()
        scheduleList.addAll(dbHelper.getAllSchedules())
        adapter.submitList(scheduleList)
    }

    override fun onItemClicked(item: Schedule) {
        super.onItemClicked(item)
        bottomSheetScheduleForm.apply {
            schedule = item
            show(this@ListScheduleFragment.parentFragmentManager, javaClass.name)
        }
    }

    fun removeItem(position: Int) {
        scheduleList.removeAt(position)
        adapter.submitList(scheduleList)
        adapter.notifyItemRemoved(position)
    }
}