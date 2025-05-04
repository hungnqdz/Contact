package com.contact_app.contact

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter

object AppBindingAdapter {

    @JvmStatic
    @BindingAdapter("isVisible")
    fun View.setVisible(isVisible: Boolean) {
        this.isVisible = isVisible
    }

    @JvmStatic
    @BindingAdapter("keySearch", "textData")
    fun AppCompatTextView.setContent(key: String?, text: String?) {
        key?.let { searchText ->
            text?.let {
                if (it.contains(searchText, true)) {
                    val colorText = SpannableString(it)
                    val startIndex = it.indexOf(searchText, ignoreCase = true)
                    colorText.setSpan(
                        ForegroundColorSpan(resources.getColor(R.color.primary)),
                        startIndex,
                        startIndex + searchText.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    this.text = colorText
                } else {
                    this.text = text.orEmpty()
                }
            }
        } ?: run {
            this.text = text.orEmpty()
        }
    }
}