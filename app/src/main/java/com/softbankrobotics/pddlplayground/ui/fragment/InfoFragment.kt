package com.softbankrobotics.pddlplayground.ui.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.R
import java.lang.IllegalStateException

class InfoFragment: DialogFragment() {
    companion object {
        fun newInstance(title: String, message: String): InfoFragment {
            val args = Bundle()
            args.putString("title", title)
            args.putString("message", message)
            val fragment = InfoFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(arguments?.getString("title"))
                .setMessage(arguments?.getString("message"))
            builder.create()
        } ?: throw IllegalStateException("activity cannot be null")
    }
}