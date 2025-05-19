package com.contact_app.contact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.contact_app.contact.adapter.CallBackChecked
import com.contact_app.contact.adapter.ChooseContactAdapter
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.base.OnItemClickListener
import com.contact_app.contact.databinding.ActivityChooseContactBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.Contact
import java.io.Serializable

class ChooseContactActivity : BaseActivity<ActivityChooseContactBinding>(), OnItemClickListener<Contact> {

    override val layoutId: Int
        get() = R.layout.activity_choose_contact
    private var listContacts = mutableListOf<Contact>()
    private lateinit var chosenContacts: MutableList<Contact>
    private val adapter by lazy {
        ChooseContactAdapter(this, object : CallBackChecked {
            override fun checkedChange(isChecked: Boolean, item: Contact, position: Int) {
                if (isChecked) {
                    if (!chosenContacts.contains(item)) {
                        chosenContacts.add(item)
                    }
                } else {
                    chosenContacts.remove(item)
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding.apply {
            listContacts = dbHelper.getAllContacts().toMutableList()
            chosenContacts = (intent.getSerializableExtra("CHOSEN_CONTACTS") as? List<Contact>)?.toMutableList() ?: mutableListOf()
            Log.d("CHOSEN","$chosenContacts")
            adapter = this@ChooseContactActivity.adapter
            this@ChooseContactActivity.adapter.submitList(listContacts)
            this@ChooseContactActivity.adapter.setPreCheckedItems(chosenContacts)

            btnSave.setOnClickListener {
                val resultIntent = Intent().apply {
                    putExtra("SELECTED_CONTACTS", chosenContacts as Serializable)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            btnCancel.setOnClickListener {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chosenContacts.clear()
    }

}