package com.contact_app.contact

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivitySetPasswordBinding
import com.contact_app.contact.db.CipherContactDatabaseHelper
import com.contact_app.contact.sharepref.SharePrefHelper
import com.google.gson.Gson

class SetPasswordActivity : BaseActivity<ActivitySetPasswordBinding>() {
    override val layoutId: Int
        get() = R.layout.activity_set_password

    lateinit var sharePrefHelper:SharePrefHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharePrefHelper = SharePrefHelper(applicationContext, Gson())
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0
        viewBinding.btnContinue.setOnClickListener {
            validateAndContinue()
        }

        viewBinding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateAndContinue() {
        val password = viewBinding.etPassword.text.toString()
        val confirm = viewBinding.etConfirm.text.toString()

        when {
            password.isBlank() || confirm.isBlank() -> {
                showToast("Vui lòng nhập đầy đủ mật khẩu và xác nhận.")
            }

            password.length < 6 -> {
                showToast("Mật khẩu phải có ít nhất 6 ký tự.")
            }

            password != confirm -> {
                showToast("Mật khẩu xác nhận không khớp.")
            }

            else -> {
                showToast("Mật khẩu hợp lệ!")
                CipherContactDatabaseHelper.getOrCreatePassphrase(password)
                startActivity(Intent(this, ActivityPrivateContact::class.java))
                sharePrefHelper.put("isCreatePassword", true)
                finish()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
