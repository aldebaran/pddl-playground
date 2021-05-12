package com.softbankrobotics.pddlplayground.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.databinding.FragmentEditTypeBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.ui.main.MainFragment.Companion.ADD_EXPRESSION

class TypeFragment: DialogFragment() {
    companion object {
        fun newInstance(expression: Expression, action: String): TypeFragment {
            val args = Bundle()
            args.putParcelable("expression_extra", expression)
            args.putString("action", action)
            val fragment = TypeFragment()
            fragment.arguments = args
            return fragment
        }
    }

    var expression: Expression? = null
    var action: String? = null

    private var _binding: FragmentEditTypeBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expression = arguments?.getParcelable("expression_extra")
        action = arguments?.getString("action")
        if (expression != null) {
            binding.expressionText.setText(expression?.getLabel())
        }

        binding.okButton.setOnClickListener {
            expression?.apply {
                val expression = binding.expressionText.text.toString()
                if (expression.contains(' ') || expression.isEmpty()) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Expression must not contain spaces or be empty.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@setOnClickListener
                }
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
        if (action == ADD_EXPRESSION) {
            expression?.apply {
                DatabaseHelper.getInstance(context!!).deleteExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
            }
        }
        super.onDismiss(dialog)
    }
}