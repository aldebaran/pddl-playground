package com.softbankrobotics.pddlplayground.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.R
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.databinding.FragmentEditPredicateBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import timber.log.Timber

class PredicateFragment: DialogFragment() {
    companion object {
        fun newInstance(expression: Expression, action: String): PredicateFragment {
            val args = Bundle()
            args.putParcelable("expression_extra", expression)
            args.putString("action", action)
            val fragment = PredicateFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var predicate: Expression? = null
    private var action: String? = null
    private var _binding: FragmentEditPredicateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPredicateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // recover arguments
        predicate = arguments?.getParcelable<Expression>("expression_extra")
        action = arguments?.getString("action")

        // get view elements
        val predicateText = binding.predicateText
        val typeSpinner = binding.typeSpinner
        val typeSpinner2 = binding.typeSpinner2
        val parameterButton = binding.parameterButton
        val parameterButton2 = binding.parameterButton2
        val parameterLayout = binding.parameterLayout
        val parameterLayout2 = binding.parameterLayout2

        // recover types from Database & populate spinners
        val types = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
            .map { it.getLabel() }
        val types2 = types.toList()
        typeSpinner.adapter =
            ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, types)
        typeSpinner2.adapter =
            ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, types2)
        if (predicate != null) { // if filled in already
            predicateText.setText(predicate?.getLabel()?.substringBefore(" "))
            // search types
            val subExpression = predicate?.getLabel()?.substringAfter(" - ")
            val type = subExpression?.substringBefore(' ')
            if (types.any { it == type }) {
                parameterLayout.visibility = View.VISIBLE
                parameterButton.isChecked = true
                typeSpinner.setSelection(types.indexOf(type))
            }
            val type2 = subExpression?.substringAfter(" - ", "")
                ?.substringBefore(' ')
            if (types2.any { it == type2 }) {
                parameterLayout2.visibility = View.VISIBLE
                parameterButton2.isChecked = true
                typeSpinner2.setSelection(types2.indexOf(type2))
            }
        }

        // toggle type spinner view
        parameterButton.setOnClickListener {
            parameterLayout.visibility = when {
                (it as ToggleButton).isChecked -> View.VISIBLE
                else -> View.GONE
            }
        }

        // toggle type spinner view 2
        parameterButton2.setOnClickListener {
            parameterLayout2.visibility = when {
                (it as ToggleButton).isChecked -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.okButton.setOnClickListener {
            predicate?.apply {
                var expression = predicateText.text.toString()
                if (parameterLayout.visibility == View.VISIBLE)
                    expression += " ?p1 - ${typeSpinner.selectedItem as String} "
                if (parameterLayout2.visibility == View.VISIBLE)
                    expression += "?p2 - ${typeSpinner2.selectedItem as String}"
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
            predicate?.apply {
                DatabaseHelper.getInstance(context!!).deleteExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
            }
        }
        super.onDismiss(dialog)
    }
}