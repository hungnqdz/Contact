package com.contact_app.contact.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.contact_app.contact.AskPasswordActivity
import com.contact_app.contact.DetailContactActivity
import com.contact_app.contact.FormContactActivity
import com.contact_app.contact.MainActivity
import com.contact_app.contact.R
import com.contact_app.contact.SetPasswordActivity
import com.contact_app.contact.adapter.ContactAdapter
import com.contact_app.contact.adapter.SearchColumn
import com.contact_app.contact.adapter.CustomSpinnerAdapter
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.base.OnLongClickListener
import com.contact_app.contact.base.dragging
import com.contact_app.contact.databinding.FragmentHomeBinding
import com.contact_app.contact.dialog.DialogSort
import com.contact_app.contact.dialog.DialogSortCallBack
import com.contact_app.contact.dialog.Order
import com.contact_app.contact.dialog.SortColumn
import com.contact_app.contact.model.Contact
import com.contact_app.contact.sharepref.SharePrefHelper
import com.google.gson.Gson

class HomeFragment : Fragment(), OnItemClickListener<Contact>, OnLongClickListener<Contact> {
    private lateinit var viewBinding: FragmentHomeBinding
    private val adapter by lazy {
        ContactAdapter(this, this)
    }
    private var listContacts = mutableListOf<Contact>()
    private var searchColumn: SearchColumn? = SearchColumn.NAME

    var dialogSort: DialogSort = DialogSort()
    lateinit var sharePrefHelper: SharePrefHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::viewBinding.isInitialized) {
            viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
            viewBinding.apply {
                root.isClickable = true
            }
        }
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        sharePrefHelper = SharePrefHelper(requireContext(), Gson())
        val isPermitted = sharePrefHelper.get("isPermitted",Boolean::class.java,false)
        if (isPermitted == true) {
            listContacts = activity.dbHelper.getAllContacts() as MutableList
            adapter.submitList(listContacts)
            viewBinding.numberContact = activity.dbHelper.countContacts()
        }
        activity.onPermitted = {
            listContacts = activity.dbHelper.getAllContacts() as MutableList
            adapter.submitList(listContacts)
            sharePrefHelper.put("isPermitted", true)
            viewBinding.numberContact = activity.dbHelper.countContacts()
        }
        viewBinding.apply {
            adapter = this@HomeFragment.adapter
            searchInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun afterTextChanged(p0: Editable?) {
                    this@HomeFragment.adapter.setKeySearch(p0.toString(), searchColumn)
                    if (p0.toString().isBlank()) {
                        this@HomeFragment.adapter.submitList(activity.dbHelper.getAllContacts())
                        contactNum.visibility = View.VISIBLE
                        contactLabel.visibility = View.VISIBLE
                        return
                    }
                    listContacts = activity.dbHelper.search(
                        p0.toString(),
                        searchColumn?.column ?: "name"
                    ) as MutableList
                    listContacts.map {
                        it.keySearch = p0.toString()
                    }
                    this@HomeFragment.adapter.submitList(listContacts)
                    contactNum.visibility = View.GONE
                    contactLabel.visibility = View.GONE
                }

            })
            val spinnerAdapter = CustomSpinnerAdapter(
                requireContext(), R.layout.item_spinner,
                listOf("Tên", "Công ty", "Số điện thoại", "Email")
            )
            dropdownBtn.adapter = spinnerAdapter
            dropdownBtn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    searchColumn = SearchColumn.entries.getOrNull(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
            dropdownMenu.setOnClickListener {
                val popup = PopupMenu(requireContext(), it)
                popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.sort -> {
                            dialogSort.callBack = object : DialogSortCallBack {
                                override fun onCancel() {
                                    dialogSort.dismiss()
                                }

                                override fun onConfirm(sortBy: SortColumn?, order: Order?) {
                                    this@HomeFragment.adapter.submitList(
                                        activity.dbHelper.sortContacts(
                                            sortBy?.column.toString(),
                                            order?.order.toString()
                                        )
                                    )
                                    dialogSort.dismiss()
                                }

                            }
                            dialogSort.showDialog(
                                requireActivity().supportFragmentManager,
                                javaClass.name
                            )
                            true
                        }

                        R.id.sync -> {
                            true
                        }

                        R.id.private_storage -> {
                            navToPrivate()
                            true
                        }

                        R.id.add_contact -> {
                            val intent = Intent(requireContext(), FormContactActivity::class.java)
                            startActivityForResult(intent, 3)
                            true
                        }

                        else -> false
                    }
                }
                popup.show()
            }
            btnCall.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:")
                startActivity(intent)
            }
            btnCall.setOnTouchListener(dragging)
        }
    }


    override fun onItemPositionLongClicked(position: Int): Boolean {
        super.onItemPositionLongClicked(position)
        listContacts.map {
            it.isLongClicked = false
        }
        listContacts[position] = listContacts[position].copy(isLongClicked = true)
        adapter.submitList(listContacts)
        return false
    }

    override fun onItemClicked(item: Contact) {
        super.onItemClicked(item)
        val intent = Intent(requireContext(), DetailContactActivity::class.java)
        intent.putExtra("contact", item)
        startActivityForResult(intent, 2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 2 || requestCode == 3) && resultCode == RESULT_OK) {
            val result = data?.getBooleanExtra("isUpdate", false)
            val resultCreate = data?.getBooleanExtra("isCreate", false)
            if (result == true || resultCreate == true) {
                val activity = requireActivity() as MainActivity
                adapter.submitList(activity.dbHelper.getAllContacts())
                viewBinding.numberContact = activity.dbHelper.countContacts()
            }
        }
    }

    fun navToPrivate() {
        val isSetPassword = sharePrefHelper.get("isCreatePassword", Boolean::class.java, false)
        val intent = Intent(
            requireContext(),
            if (isSetPassword == true) AskPasswordActivity::class.java else SetPasswordActivity::class.java
        )
        startActivity(intent)
    }
}