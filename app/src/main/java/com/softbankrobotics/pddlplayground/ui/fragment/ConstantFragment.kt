package com.softbankrobotics.pddlplayground.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.R
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import kotlinx.android.synthetic.main.fragment_edit_expression.cancelButton
import kotlinx.android.synthetic.main.fragment_edit_expression.okButton

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_constant, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        constant = arguments?.getParcelable("expression_extra")
        action = arguments?.getString("action")
        val constantText = view.findViewById<TextView>(R.id.constantText)
        val spinner = view.findViewById<Spinner>(R.id.typeSpinner)
        // recover types from Database & populate spinner
        val types = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
            .map { it.getLabel() }
        spinner.adapter = ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, types)
        if (constant != null) { // if filled already
            constantText.text = constant?.getLabel()?.substringBefore(" - ")
            val type = constant?.getLabel()?.substringAfter(" - ")
            spinner.setSelection(types.indexOf(type))
        }

        okButton.setOnClickListener {
            constant?.apply {
                setLabel("${constantText.text} - ${spinner.selectedItem as String}")
                DatabaseHelper.getInstance(context!!).updateExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
                action = MainFragment.EDIT_EXPRESSION
            }
            dismiss()
        }

        cancelButton.setOnClickListener {
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