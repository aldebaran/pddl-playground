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
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import kotlinx.android.synthetic.main.fragment_edit_action.*
import kotlinx.android.synthetic.main.fragment_edit_expression.cancelButton
import kotlinx.android.synthetic.main.fragment_edit_expression.okButton

class ActionFragment: DialogFragment() {
    companion object {
        fun newInstance(expression: Expression, action: String): ActionFragment {
            val args = Bundle()
            args.putParcelable("expression_extra", expression)
            args.putString("action", action)
            val fragment = ActionFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var paction: Expression? = null
    private var action: String? = null
    private lateinit var predicateLabels: List<String?>
    private val typeLabels = mutableListOf<String?>()
    private val paramLabels = mutableMapOf<String, List<String?>>()
    private val precondCheckBoxes = mutableListOf<CheckBox>()
    private val effectCheckBoxes = mutableListOf<CheckBox>()
    private val negateCheckBoxes = mutableListOf<CheckBox>()
    private val spinners = mutableListOf<Spinner>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_action, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paction = arguments?.getParcelable("expression_extra")
        action = arguments?.getString("action")

        // populate the grid with each predicate
        val predicates  = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
        predicateLabels = predicates.map { it?.substringBefore(' ') }
        val gridLayout = view.findViewById<GridLayout>(R.id.gridLayout)
        var rowCount = 1
        for ((ind, label) in predicateLabels.withIndex()) {
            val predicateText = TextView(context).apply {
                text = label
            }
            gridLayout.addView(predicateText, GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(0, GridLayout.CENTER)
                )
            )
            val type = predicates[ind]
                ?.substringAfter(" - ")?.substringBefore(" ")
            val constsAndParam = mutableListOf<String?>()
            if (type != null && type != predicates[ind]) {
                val consts = DatabaseHelper.getInstance(context!!).getExpressions()
                    .filter { it.getCategory() == PDDLCategory.CONSTANT.ordinal }
                    .map { it.getLabel() }
                    .filter { it!!.contains(type) }
                    .map { it?.substringBefore(' ') }
                typeLabels.add(type) //TODO: actually check that it's a type
                constsAndParam.add("?$type")
                constsAndParam.addAll(consts)
                paramLabels[label!!] = constsAndParam
            }
            val spinner = Spinner(context)
            spinner.adapter =
                ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, constsAndParam)
            spinners += spinner
            gridLayout.addView(spinner,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(1, GridLayout.CENTER)
                )
            )
            val checkbox = CheckBox(context)
            precondCheckBoxes += checkbox
            gridLayout.addView(checkbox,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(2, GridLayout.CENTER)
                )
            )
            val eCheckbox = CheckBox(context)
            effectCheckBoxes += eCheckbox
            gridLayout.addView(eCheckbox,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(3, GridLayout.CENTER)
                )
            )
            val nCheckbox = CheckBox(context)
            negateCheckBoxes += nCheckbox
            gridLayout.addView(nCheckbox,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(4, GridLayout.CENTER)
                )
            )
            rowCount++
        }

        if (paction != null) { // if filled out before
            val label = paction?.getLabel()
            actionText.setText(label?.substringBefore('\n'))
            // fill in checkboxes & spinners
            val preconditions = label?.substringAfter("precondition  (and\n")
                ?.substringBefore(":effect")
            val effects = label?.substringAfter("effect  (and\n")
            // loop through predicates
            for ((predicateInd, predicateLabel) in predicateLabels.withIndex()) {
                if (label?.contains(predicateLabel!!) == true) {
                    // set spinner
                    val paramLabel = label.substringAfter(predicateLabel!!).substringBefore(')')
                    // loop through types and consts (if exists)
                    if (paramLabels[predicateLabel] != null) {
                        for ((pInd, pLabel) in paramLabels[predicateLabel]!!.withIndex()) {
                            if (paramLabel.contains(pLabel!!)) {
                                spinners[predicateInd].setSelection(pInd)
                                break
                            }
                        }
                    }
                    if (label.contains("not($predicateLabel")) {
                        negateCheckBoxes[predicateInd].isChecked = true
                    }
                }
                if (preconditions?.contains(predicateLabel!!) == true) {
                    precondCheckBoxes[predicateInd].isChecked = true
                } else if (effects?.contains(predicateLabel!!) == true) {
                    effectCheckBoxes[predicateInd].isChecked = true
                }
            }
        }

        okButton.setOnClickListener {
            paction?.apply {
                var expression = "${actionText.text}\n"
                // parameters TODO should't include all types by default?
                expression += "    :parameters\n"
                for (typeLabel in typeLabels.toSet()) {
                    expression += "      (?$typeLabel - $typeLabel)\n"
                }
                // preconditions
                expression += "    :precondition (and\n"
                for ((index, precondCheckBox) in precondCheckBoxes.withIndex()) {
                    if (precondCheckBox.isChecked) {
                        val param = (spinners[index].selectedItem as String?)?: ""
                        expression += if (negateCheckBoxes[index].isChecked) {
                            "      (not(${predicateLabels[index]} $param))\n"
                        } else {
                            "      (${predicateLabels[index]} $param)\n"
                        }
                    }
                }
                expression += ")\n"
                // effects
                expression += "    :effect (and\n"
                for ((index, effectCheckBox) in effectCheckBoxes.withIndex()) {
                    if (effectCheckBox.isChecked) {
                        val param = spinners[index].selectedItem as String?
                        expression += if (negateCheckBoxes[index].isChecked) {
                            "      (not(${predicateLabels[index]} $param))\n"
                        } else {
                            "      (${predicateLabels[index]} $param)\n"
                        }
                    }
                }
                expression += ")"
                setLabel(expression)
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
            paction?.apply {
                DatabaseHelper.getInstance(context!!).deleteExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
            }
        }
        super.onDismiss(dialog)
    }
}