package com.softbankrobotics.pddlplayground.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.R
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.databinding.FragmentEditConstantBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getTypes

class ConstantFragment: DialogFragment() {
    companion object {
        fun newInstance(expression: Expression, action: String): ConstantFragment {
            val args = Bundle()
            args.putParcelable("expression_extra", expression)
            args.putString("action", action)
            val fragment = ConstantFragment()
            fragment.arguments = args
            return fragment
        }
    }

    var constant: Expression? = null
    var action: String? = null

    private var _binding: FragmentEditConstantBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditConstantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        constant = arguments?.getParcelable("expression_extra")
        action = arguments?.getString("action")
        val constantText = binding.constantText
        val spinner = binding.typeSpinner
        // recover types from Database & populate spinner
        val types = getTypes(requireContext())
        spinner.adapter = ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, types)
        if (constant?.getLabel()?.isNotEmpty() == true) { // if filled already
            constantText.setText(constant?.getLabel()?.substringBefore(" - "))
            val type = constant?.getLabel()?.substringAfter(" - ")
            spinner.setSelection(types.indexOf(type))
        }

        binding.okButton.setOnClickListener {
            constant?.apply {
                val expression = constantText.text.toString()
                val type = spinner.selectedItem as String
                if (expression.contains(' ') || expression.isEmpty() || type.isEmpty()) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Expression must not contain spaces or have empty components.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@setOnClickListener
                }
                setLabel("$expression - $type")
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
            constant?.apply {
                DatabaseHelper.getInstance(context!!).deleteExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
            }
        }
        super.onDismiss(dialog)
    }
}