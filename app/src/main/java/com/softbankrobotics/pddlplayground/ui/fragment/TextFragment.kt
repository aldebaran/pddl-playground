package com.softbankrobotics.pddlplayground.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.databinding.FragmentEditTextBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment

class TextFragment: DialogFragment() {
    companion object {
        fun newInstance(expression: Expression, action: String): TextFragment {
            val args = Bundle()
            args.putParcelable("expression_extra", expression)
            args.putString("action", action)
            val fragment = TextFragment()
            fragment.arguments = args
            return fragment
        }
    }

    var goal: Expression? = null
    var action: String? = null

    private var _binding: FragmentEditTextBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        goal = arguments?.getParcelable("expression_extra")
        action = arguments?.getString("action")

        if (goal != null) {
            binding.goalText.setText(goal?.getLabel())
        }

        binding.okButton.setOnClickListener {
                val expression = binding.goalText.text.toString()
                if (expression.isEmpty()) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Expression must not contain spaces or have empty components.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@setOnClickListener
                }
            goal?.apply {
                setLabel(expression)
                DatabaseHelper.getInstance(context!!).updateExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
                action = MainFragment.EDIT_EXPRESSION
            }
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (action == MainFragment.ADD_EXPRESSION) {
            goal?.apply {
                DatabaseHelper.getInstance(context!!).deleteExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
            }
        }
        super.onDismiss(dialog)
    }
}