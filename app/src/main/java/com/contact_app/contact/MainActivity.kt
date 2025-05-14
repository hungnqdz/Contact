package com.contact_app.contact

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityMainBinding
import java.io.File

class MainActivity : BaseActivity<ActivityMainBinding>() {
    var navController: NavController? = null
    var navHostFragment: NavHostFragment? = null
    override val layoutId: Int
        get() = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getDatabaseFile()
        viewBinding.apply {
            navHostFragment =
                supportFragmentManager.findFragmentById(R.id.frg_container) as NavHostFragment
            navController = navHostFragment?.navController

            navController?.let { navBottom.setupWithNavController(it) }
            navController?.addOnDestinationChangedListener { _, des, _ ->
                when (des.id) {
                    R.id.frg_contact -> {

                    }
                    R.id.frg_schedule -> {

                    }
                    R.id.frg_chart -> {

                    }
                }
            }
        }
    }

    fun getDatabaseFile(): File {
        val dbPath = applicationContext.getDatabasePath("contact_database.db").path
        Log.d("DB",dbPath)
        return File(dbPath)
    }
}