package com.contact_app.contact

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import com.contact_app.contact.base.BaseActivity
import com.contact_app.contact.databinding.ActivityAskPassBinding
import com.contact_app.contact.db.CipherContactDatabaseHelper

class AskPasswordActivity : BaseActivity<ActivityAskPassBinding>() {
    override val layoutId: Int
        get() = R.layout.activity_ask_pass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0
        viewBinding.apply {
            btnContinue.setOnClickListener {
                validatePassword()
            }

            btnCancel.setOnClickListener {
                finish()
            }
        }
    }

    private fun validatePassword() {
        val inputPassword = viewBinding.etPassword.text.toString()

        when {
            inputPassword.isBlank() -> {
                showToast("Vui lòng nhập mật khẩu.")
            }

            else -> {
                if (CipherContactDatabaseHelper.isPassphraseValid(this, inputPassword)) {
                    CipherContactDatabaseHelper.getOrCreatePassphrase(inputPassword)
                    showToast("Đăng nhập thành công!")
                    startActivity(Intent(this, ActivityPrivateContact::class.java))
                    finish()
                } else {
                    showToast("Mật khẩu không chính xác!")
                }
            }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
