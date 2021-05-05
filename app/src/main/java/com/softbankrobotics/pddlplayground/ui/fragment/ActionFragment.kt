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
import com.softbankrobotics.pddlplayground.databinding.FragmentEditActionBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.util.PDDLCategory

class ActionFragment : DialogFragment() {
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
    private val typeLabels = mutableListOf<String>()
    private val paramLabels = mutableMapOf<String, List<List<String?>>>()
    private val precondCheckBoxes = mutableListOf<CheckBox>()
    private val effectCheckBoxes = mutableListOf<CheckBox>()
    private val negateCheckBoxes = mutableListOf<CheckBox>()
    private val spinners = mutableListOf<Spinner>()
    private val spinners2 = mutableListOf<Spinner>()

    private var _binding: FragmentEditActionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paction = arguments?.getParcelable("expression_extra")
        action = arguments?.getString("action")

        // populate the grid with each predicate
        val predicates = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
        predicateLabels = predicates.map { it?.substringBefore(' ') }
        val gridLayout = binding.gridLayout
        var rowCount = 1
        for ((ind, label) in predicateLabels.withIndex()) {
            val predicateText = TextView(context).apply {
                text = label
            }
            gridLayout.addView(
                predicateText, GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(0, GridLayout.CENTER)
                )
            )
            val type = predicates[ind]
                ?.substringAfter(" - ")?.substringBefore(" ")
            val constsAndParam = mutableListOf<String?>()
            val constsAndParam2 = mutableListOf<String?>()
            val types = DatabaseHelper.getInstance(context!!).getExpressions()
                .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
                .map { it.getLabel() }
            if (type != null && types.contains(type)) { // if it's actually a type
                val consts = DatabaseHelper.getInstance(context!!).getExpressions()
                    .filter { it.getCategory() == PDDLCategory.CONSTANT.ordinal }
                    .map { it.getLabel() }
                val consts1 = consts
                    .filter { it!!.contains(type) }
                    .map { it?.substringBefore(' ') }
                typeLabels.add(type)
                constsAndParam.add("?$type")
                constsAndParam.addAll(consts1)
                val type2 = predicates[ind]?.substringAfter(type)
                    ?.substringAfter(" - ")?.substringBefore(" ")
                if (type2 != null && type2.isNotEmpty() && types.contains(type2)) {
                    val consts2 = consts
                        .filter { it!!.contains(type2) }
                        .map { it?.substringBefore(' ') }
                    typeLabels.add(type2)
                    constsAndParam2.add("?$type2")
                    constsAndParam2.addAll(consts2)
                }
                paramLabels[label!!] = listOf(constsAndParam, constsAndParam2)
            }
            val spinner = Spinner(context)
            spinner.adapter =
                ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    constsAndParam
                )
            spinners += spinner
            gridLayout.addView(
                spinner,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(1, GridLayout.CENTER)
                )
            )
            val spinner2 = Spinner(context)
            spinner2.adapter =
                ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    constsAndParam2
                )
            spinners2 += spinner2
            gridLayout.addView(
                spinner2,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(2, GridLayout.CENTER)
                )
            )
            val checkbox = CheckBox(context)
            precondCheckBoxes += checkbox
            gridLayout.addView(
                checkbox,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(3, GridLayout.CENTER)
                )
            )
            val eCheckbox = CheckBox(context)
            effectCheckBoxes += eCheckbox
            gridLayout.addView(
                eCheckbox,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(4, GridLayout.CENTER)
                )
            )
            val nCheckbox = CheckBox(context)
            negateCheckBoxes += nCheckbox
            gridLayout.addView(
                nCheckbox,
                GridLayout.LayoutParams(
                    GridLayout.spec(rowCount, GridLayout.CENTER),
                    GridLayout.spec(5, GridLayout.CENTER)
                )
            )
            rowCount++
        }

        if (paction != null) { // if filled out before
            val label = paction?.getLabel()
            binding.actionText.setText(label?.substringBefore('\n'))
            // fill in checkboxes & spinners
            val preconditions = label?.substringAfter("precondition  (and\n")
                ?.substringBefore(":effect")
            val effects = label?.substringAfter("effect  (and\n")
            // loop through predicates
            for ((predicateInd, predicateLabel) in predicateLabels.withIndex()) {
                if (label?.contains(predicateLabel!!) == true) {
                    // set spinners
                    val paramLabel = label.substringAfter(predicateLabel!!).substringBefore(')')
                    // loop through types and consts (if exists)
                    if (paramLabels[predicateLabel]?.first() != null) {
                        for ((pInd, pLabel) in paramLabels[predicateLabel]!!.first().withIndex()) {
                            if (paramLabel.contains(pLabel!!)) {
                                spinners[predicateInd].setSelection(pInd)
                                break
                            }
                        }
                    }
                    if (paramLabels[predicateLabel]?.last() != null) {
                        for ((pInd, pLabel) in paramLabels[predicateLabel]!!.last().withIndex()) {
                            if (paramLabel.contains(pLabel!!)) {
                                spinners2[predicateInd].setSelection(pInd)
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

        binding.okButton.setOnClickListener {
            val paramList = mutableListOf<String>()
            // preconditions
            var precondition = "    :precondition (and\n"
            for ((index, precondCheckBox) in precondCheckBoxes.withIndex()) {
                if (precondCheckBox.isChecked) {
                    val param = (spinners[index].selectedItem as String?) ?: ""
                    val param2 = (spinners2[index].selectedItem as String?) ?: ""
                    precondition += if (negateCheckBoxes[index].isChecked) {
                        "      (not(${predicateLabels[index]} $param $param2))\n"
                    } else {
                        "      (${predicateLabels[index]} $param $param2)\n"
                    }
                    paramList.add(param)
                    paramList.add(param2)
                }
            }
            precondition += "    )\n"
            // effects
            var effect = "    :effect (and\n"
            for ((index, effectCheckBox) in effectCheckBoxes.withIndex()) {
                if (effectCheckBox.isChecked) {
                    val param = spinners[index].selectedItem as String? ?: ""
                    val param2 = spinners2[index].selectedItem as String? ?: ""
                    effect += if (negateCheckBoxes[index].isChecked) {
                        "      (not(${predicateLabels[index]} $param $param2))\n"
                    } else {
                        "      (${predicateLabels[index]} $param $param2)\n"
                    }
                    paramList.add(param)
                    paramList.add(param2)
                }
            }
            effect += "    )\n"
            // parameters (only add when it's actually used)
            var parameters = "    :parameters\n"
            for (typeLabel in typeLabels.toSet()) {
                for (param in paramList) {
                    if (param.contains(typeLabel)) {
                        parameters += "      (?$typeLabel - $typeLabel)\n"
                        break
                    }
                }
            }
            paction?.apply {
                setLabel("${binding.actionText.text}\n" + parameters + precondition + effect)
                DatabaseHelper.getInstance(context!!).updateExpression(this)
            }
            LoadExpressionsService.launchLoadExpressionsService(context!!)
            action = MainFragment.EDIT_EXPRESSION
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
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