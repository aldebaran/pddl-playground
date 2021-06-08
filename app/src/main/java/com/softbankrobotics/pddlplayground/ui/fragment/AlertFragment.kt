package com.softbankrobotics.pddlplayground.ui.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.R
import com.softbankrobotics.pddlplayground.util.PDDLUtil.clearDatabase
import com.softbankrobotics.pddlplayground.util.PDDLUtil.fillInDatabaseWithSample
import java.lang.IllegalStateException

class AlertFragment: DialogFragment() {
    companion object {
        fun newInstance(title: String, message: CharSequence, import: Boolean): AlertFragment {
            val args = Bundle()
            args.putString("title", title)
            args.putCharSequence("message", message)
            args.putBoolean("import", import)
            val fragment = AlertFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = androidx.appcompat.app.AlertDialog.Builder(it)
            builder.setTitle(arguments?.getString("title"))
                .setMessage(arguments?.getCharSequence("message"))
                .setPositiveButton(R.string.ok
                ) { _, _ ->
                    clearDatabase(requireContext())
                    if (arguments?.getBoolean("import") == true) {
                        fillInDatabaseWithSample(requireContext())
                    }
                }
                .setNegativeButton(R.string.cancel
                ) { _, _ ->
                    // User cancelled the dialog
                }
            builder.create()
        } ?: throw IllegalStateException("activity cannot be null")
    }
}