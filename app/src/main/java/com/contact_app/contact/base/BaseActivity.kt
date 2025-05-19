package com.contact_app.contact.base

import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.sharepref.SharePrefHelper
import com.google.gson.Gson
import java.io.Serializable

abstract class BaseActivity<ViewBinding : ViewDataBinding> : AppCompatActivity() {

    protected lateinit var viewBinding: ViewBinding

    @get:LayoutRes
    protected abstract val layoutId: Int
    private val PERMISSION_REQUEST_CODE = 100

    lateinit var dbHelper: ContactDatabaseHelper

    var onPermitted: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!::viewBinding.isInitialized) {
            viewBinding = DataBindingUtil.setContentView(this, layoutId)
            viewBinding.lifecycleOwner = this
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = arrayOf(
            WRITE_CONTACTS,
            READ_CONTACTS,
            READ_CALL_LOG
        )
        val missingPermissions = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        val sharePrefHelper = SharePrefHelper(this,Gson())
        val isPermitted = sharePrefHelper.get("isPermitted",Boolean::class.java,false)
        if (missingPermissions.isEmpty()) {
            initDbHelper()
            if (isPermitted == true) return
            dbHelper.syncContactsFromDevice()
            dbHelper.syncCallLogsFromDevice()
            onPermitted.invoke()
        } else {
            val sharePrefHelper = SharePrefHelper(this, Gson())
            sharePrefHelper.put("isPermitted", false)
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun initDbHelper() {
        dbHelper = ContactDatabaseHelper.getInstance(this) {
            Toast.makeText(this, "Permissions required for database operations", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initDbHelper()
                dbHelper.syncContactsFromDevice()
                dbHelper.syncCallLogsFromDevice()
                onPermitted.invoke()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    protected open fun startActivityForResultCompat(
        destination: Class<out Activity>,
        requestCode: Int,
        vararg extras: Pair<String, Serializable>
    ) {
        val intent = Intent(this, destination)
        extras.forEach { (key, value) -> intent.putExtra(key, value) }
        startActivityForResult(intent, requestCode)
    }

    protected open fun <ReturnType : Serializable> finishActivityWithResult(
        variableName: String? = "isUpdate",
        value: ReturnType,
        isFinish: Boolean? = true
    ) {
        val resultIntent = Intent()
        resultIntent.putExtra(variableName, value)
        setResult(RESULT_OK, resultIntent)
        if (isFinish == true) finish()
    }
}