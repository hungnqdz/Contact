package com.contact_app.contact.base

import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheet <ViewBinding : ViewDataBinding> : BottomSheetDialogFragment() {
    private var isShowing = false
    protected lateinit var viewBinding: ViewBinding

    @get:LayoutRes
    protected abstract val layoutId: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        viewBinding.apply {
            root.isClickable = true
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
        }
        if (savedInstanceState != null) {
            if (isAdded) {
                dismiss()
            }
        }
        return viewBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.decorView?.background = ColorDrawable(Color.TRANSPARENT)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isShowing) return
        isShowing = true
        super.show(manager, tag)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        isShowing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isShowing = false
    }

    open fun showDialog(
        fragmentManager: FragmentManager?,
        tag: String?
    ) {
        fragmentManager?.let {
            show(fragmentManager, tag)
        }
    }
}