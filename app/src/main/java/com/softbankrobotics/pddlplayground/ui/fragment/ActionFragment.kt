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
    private val paramLabels = mutableListOf<String?>()
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
                paramLabels.add(type) //TODO: actually check that it's a type
                constsAndParam.add("?$type")
                constsAndParam.addAll(consts)
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
            actionText.setText(paction?.getLabel()?.substringBefore(' '))
            //TODO: fill in checkboxes & spinners
        }

        //TODO: save predicates upon checkbox click

        okButton.setOnClickListener {
            paction?.apply {
                //TODO: populate expression
                var expression = "${actionText.text}\n"
                // parameters TODO include all types by default?
                expression += "    :parameters\n"
                for (paramLabel in paramLabels.toSet()) {
                    expression += "      (?$paramLabel - $paramLabel)\n"
                }
                // preconditions
                expression += "    :precondition\n"
                for ((index, precondCheckBox) in precondCheckBoxes.withIndex()) {
                    if (precondCheckBox.isChecked) {
                        val param = spinners[index].selectedItem as String?
                        expression += if (negateCheckBoxes[index].isChecked) {
                            "      (not(${predicateLabels[index]} $param))\n"
                        } else {
                            "      (${predicateLabels[index]} $param)\n"
                        }
                    }
                }
                // effects
                expression += "    :effect\n"
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