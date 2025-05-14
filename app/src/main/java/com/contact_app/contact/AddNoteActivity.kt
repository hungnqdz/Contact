package com.contact_app.contact

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityAddNoteBinding

class AddNoteActivity() : BaseActivity<ActivityAddNoteBinding>() {

    override val layoutId: Int
        get() = R.layout.activity_add_note

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

}