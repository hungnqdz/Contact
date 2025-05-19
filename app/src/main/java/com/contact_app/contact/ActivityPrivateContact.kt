package com.contact_app.contact

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import com.contact_app.contact.adapter.ContactAdapter
import com.contact_app.contact.adapter.SearchColumn
import com.contact_app.contact.adapter.CustomSpinnerAdapter
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.base.dragging
import com.contact_app.contact.databinding.ActivityPrivateContactBinding
import com.contact_app.contact.db.CipherContactDatabaseHelper
import com.contact_app.contact.dialog.DialogSort
import com.contact_app.contact.model.Contact

class ActivityPrivateContact : BaseActivity<ActivityPrivateContactBinding>(),
    OnItemClickListener<Contact> {
    override val layoutId: Int
        get() = R.layout.activity_private_contact

    private val adapter by lazy {
        ContactAdapter(this, layoutItem = R.layout.item_private_contact)
    }
    private var listContacts = mutableListOf<Contact>()
    private lateinit var dbPrivateHelper: CipherContactDatabaseHelper
    private var searchColumn: SearchColumn? = SearchColumn.NAME

    var dialogSort: DialogSort = DialogSort()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbPrivateHelper = CipherContactDatabaseHelper.getInstance(this)
        listContacts = dbPrivateHelper.getAllContacts() as MutableList
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0
        viewBinding.apply {
            adapter = this@ActivityPrivateContact.adapter
            numberContact = dbPrivateHelper.countContacts()
            searchInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun afterTextChanged(p0: Editable?) {
                    this@ActivityPrivateContact.adapter.setKeySearch(p0.toString(), searchColumn)
                    if (p0.toString().isBlank()) {
                        this@ActivityPrivateContact.adapter.submitList(dbPrivateHelper.getAllContacts())
                        contactNum.visibility = View.VISIBLE
                        contactLabel.visibility = View.VISIBLE
                        return
                    }
                    listContacts = dbPrivateHelper.search(
                        p0.toString(),
                        searchColumn?.column ?: "name"
                    ) as MutableList
                    listContacts.map {
                        it.keySearch = p0.toString()
                    }
                    this@ActivityPrivateContact.adapter.submitList(listContacts)
                    contactNum.visibility = View.GONE
                    contactLabel.visibility = View.GONE
                }

            })
            val spinnerAdapter = CustomSpinnerAdapter(
                this@ActivityPrivateContact, R.layout.item_spinner,
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
            btnCall.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:")
                startActivity(intent)
            }
            btnCall.setOnTouchListener(dragging)
        }
        adapter.submitList(listContacts)
    }


    override fun onItemClicked(item: Contact) {
        super.onItemClicked(item)
        val intent = Intent(this, ActivityPrivateDetailContact::class.java)
        intent.putExtra("contact", item)
        startActivityForResult(intent, 2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 2 || requestCode == 3) && resultCode == RESULT_OK) {
            val result = data?.getBooleanExtra("isUpdate", false)
            val resultCreate = data?.getBooleanExtra("isCreate", false)
            if (result == true || resultCreate == true) {
                adapter.submitList(dbPrivateHelper.getAllContacts())
                viewBinding.numberContact = dbPrivateHelper.countContacts()
            }
        }
    }

}