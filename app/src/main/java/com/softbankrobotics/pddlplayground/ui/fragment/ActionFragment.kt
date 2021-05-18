package com.softbankrobotics.pddlplayground.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
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
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getTypesAndParamsForPredicates
import timber.log.Timber

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

        // view elements
        val precondCheckBoxes = mutableListOf<CheckBox>()
        val effectCheckBoxes = mutableListOf<CheckBox>()
        val precondPredicateSpinners = mutableListOf<Spinner>()
        val effectPredicateSpinners = mutableListOf<Spinner>()
        val preconditionSpinners = mutableListOf<Spinner>()
        val preconditionSpinners2 = mutableListOf<Spinner>()
        val effectSpinners = mutableListOf<Spinner>()
        val effectSpinners2 = mutableListOf<Spinner>()
        val pnCheckBoxes = mutableListOf<CheckBox>()
        val enCheckBoxes = mutableListOf<CheckBox>()

        // for each predicate, save its argument types and constants
        val (typeLabels, paramLabels) = getTypesAndParamsForPredicates(requireContext())

        // populate the precondition and effect grid (first row)
        val predicateLabels = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
            .map { it?.substringBefore(' ') }
        var pRowCount = 1
        var eRowCount = 1
        addRow(
            binding.gridLayout,
            predicateLabels,
            paramLabels,
            precondCheckBoxes,
            precondPredicateSpinners,
            preconditionSpinners,
            preconditionSpinners2,
            pnCheckBoxes,
            pRowCount
        )
        addRow(
            binding.gridLayout2,
            predicateLabels,
            paramLabels,
            effectCheckBoxes,
            effectPredicateSpinners,
            effectSpinners,
            effectSpinners2,
            enCheckBoxes,
            eRowCount
        )
        pRowCount++
        eRowCount++

        // if action was filled out before, populate the UI
        if (paction != null) {
            val label = paction?.getLabel()
            binding.actionText.setText(label?.substringBefore('\n'))
            // fill in checkboxes & spinners
            val preconditions = label?.substringAfter("precondition (and\n")
                ?.substringBefore(":effect")
            val effects = label?.substringAfter("effect (and\n")

            // loop through preconditions
            pRowCount = fillInRows(
                preconditions,
                binding.gridLayout,
                predicateLabels,
                paramLabels,
                precondCheckBoxes,
                precondPredicateSpinners,
                preconditionSpinners,
                preconditionSpinners2,
                pnCheckBoxes,
                pRowCount
            )

            // loop through effects
            eRowCount = fillInRows(
                effects,
                binding.gridLayout2,
                predicateLabels,
                paramLabels,
                effectCheckBoxes,
                effectPredicateSpinners,
                effectSpinners,
                effectSpinners2,
                enCheckBoxes,
                eRowCount
            )
        }

        // add a precondition row
        binding.addPreconditionButton.setOnClickListener {
            addRow(
                binding.gridLayout,
                predicateLabels,
                paramLabels,
                precondCheckBoxes,
                precondPredicateSpinners,
                preconditionSpinners,
                preconditionSpinners2,
                pnCheckBoxes,
                pRowCount
            )
            pRowCount++
        }

        // add an effect row
        binding.addEffectButton.setOnClickListener {
            addRow(
                binding.gridLayout2,
                predicateLabels,
                paramLabels,
                effectCheckBoxes,
                effectPredicateSpinners,
                effectSpinners,
                effectSpinners2,
                enCheckBoxes,
                eRowCount
            )
            eRowCount++
        }

        binding.okButton.setOnClickListener {
            val paramList = mutableListOf<String>()
            // preconditions
            var precondition = "    :precondition (and\n"
            for ((index, precondCheckBox) in precondCheckBoxes.withIndex()) {
                if (precondCheckBox.isChecked) {
                    val pPredicate = (precondPredicateSpinners[index].selectedItem as String?) ?: ""
                    if (pPredicate.isNotEmpty()) {
                        val param = (preconditionSpinners[index].selectedItem as String?) ?: ""
                        val param2 = (preconditionSpinners2[index].selectedItem as String?) ?: ""
                        precondition += if (pnCheckBoxes[index].isChecked) {
                            "      (not($pPredicate $param $param2))\n"
                        } else {
                            "      ($pPredicate $param $param2)\n"
                        }
                        paramList.add(param)
                        paramList.add(param2)
                    }
                }
            }
            precondition += "    )\n"
            // effects
            var effect = "    :effect (and\n"
            for ((index, effectCheckBox) in effectCheckBoxes.withIndex()) {
                if (effectCheckBox.isChecked) {
                    val ePredicate = (effectPredicateSpinners[index].selectedItem as String?) ?: ""
                    if (ePredicate.isNotEmpty()) {
                        val param = effectSpinners[index].selectedItem as String? ?: ""
                        val param2 = effectSpinners2[index].selectedItem as String? ?: ""
                        effect += if (enCheckBoxes[index].isChecked) {
                            "      (not($ePredicate $param $param2))\n"
                        } else {
                            "      ($ePredicate $param $param2)\n"
                        }
                        paramList.add(param)
                        paramList.add(param2)
                    }
                }
            }
            effect += "    )\n"
            // parameters (only add when it's actually used)
            var parameters = "    :parameters\n"
            for (typeLabel in typeLabels.toSet()) {
                for (param in paramList.toSet()) {
                    if (param.contains(typeLabel)) {
                        parameters += "      ($param - $typeLabel)\n"
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

    private fun addRow(
        grid: GridLayout,
        predicateLabels: List<String?>,
        paramLabels: Map<String, List<List<String?>>>,
        checkBoxes: MutableList<CheckBox>,
        predicateSpinners: MutableList<Spinner>,
        paramSpinners: MutableList<Spinner>,
        paramSpinners2: MutableList<Spinner>,
        negateCheckBoxes: MutableList<CheckBox>,
        rowCount: Int
    ) {
        // column 1
        val precondCheckbox = CheckBox(context)
        checkBoxes += precondCheckbox
        grid.addView(
            precondCheckbox,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(0, GridLayout.CENTER)
            )
        )

        // column 2
        val predicateSpinner = Spinner(context)
        predicateSpinner.adapter =
            ArrayAdapter(
                context!!,
                R.layout.support_simple_spinner_dropdown_item,
                predicateLabels.plus("")
            )
        predicateSpinners += predicateSpinner
        grid.addView(
            predicateSpinner,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(1, GridLayout.CENTER)
            )
        )

        // column 3
        val paramSpinner = Spinner(context)
        paramSpinner.adapter =
            ArrayAdapter(
                context!!,
                R.layout.support_simple_spinner_dropdown_item,
                listOf<String>()
            )
        paramSpinners += paramSpinner
        grid.addView(
            paramSpinner,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(2, GridLayout.CENTER)
            )
        )

        // column 4
        val paramSpinner2 = Spinner(context)
        paramSpinner2.adapter =
            ArrayAdapter(
                context!!,
                R.layout.support_simple_spinner_dropdown_item,
                listOf<String>()
            )
        paramSpinners2 += paramSpinner2
        grid.addView(
            paramSpinner2,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(3, GridLayout.CENTER)
            )
        )

        // column 5
        val pnCheckBox = CheckBox(context)
        negateCheckBoxes += pnCheckBox
        grid.addView(
            pnCheckBox,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(4, GridLayout.CENTER)
            )
        )

        // listener for predicate spinner
        predicateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Timber.i("Nothing selected.")
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position >= predicateLabels.size) {
                    Timber.d("Selected empty item.")
                    return
                }
                val predicates = DatabaseHelper.getInstance(context!!).getExpressions()
                    .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
                    .map { it.getLabel() }
                val predicateLabel = predicates[position]?.substringBefore(' ')

                paramSpinner.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    paramLabels[predicateLabel!!]?.first() ?: listOf()
                )
                paramSpinner2.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    paramLabels[predicateLabel]?.last() ?: listOf()
                )
            }
        }
    }

    private fun fillInRows(
        label: String?,
        grid: GridLayout,
        predicateLabels: List<String?>,
        paramLabels: Map<String, List<List<String?>>>,
        checkBoxes: MutableList<CheckBox>,
        predicateSpinners: MutableList<Spinner>,
        paramSpinners: MutableList<Spinner>,
        paramSpinners2: MutableList<Spinner>,
        negateCheckBoxes: MutableList<CheckBox>,
        rowCount: Int
    ): Int {
        var updatedRowCount = rowCount
        val preconditionLabels = label?.split("\n")?.dropLast(2)?.map {
            it.substringAfter('(').substringBefore(')')
        }
        if (!preconditionLabels.isNullOrEmpty()) {
            for ((preconditionInd, preconditionLabel) in preconditionLabels.withIndex()) {
                if (preconditionInd != 0) {
                    addRow(
                        grid,
                        predicateLabels,
                        paramLabels,
                        checkBoxes,
                        predicateSpinners,
                        paramSpinners,
                        paramSpinners2,
                        negateCheckBoxes,
                        updatedRowCount
                    )
                    updatedRowCount++
                }
                val predicateLabel = preconditionLabel
                    .substringBefore(' ')
                    .substringAfter("not(")
                predicateSpinners[preconditionInd].setSelection(
                    predicateLabels.indexOfFirst { it == predicateLabel }
                )
                // set the consts and params
                val paramLabel = preconditionLabel.substringAfter(predicateLabel)
                // loop through types and consts (if exists)
                if (paramLabels[predicateLabel]?.first() != null) {
                    for ((pInd, pLabel) in paramLabels[predicateLabel]!!.first().withIndex()) {
                        if (paramLabel.contains(pLabel!!)) {
                            paramSpinners[preconditionInd].adapter =
                                ArrayAdapter(
                                    context!!,
                                    R.layout.support_simple_spinner_dropdown_item,
                                    paramLabels[predicateLabel]?.first() ?: listOf()
                                )
                            Handler().postDelayed({
                                paramSpinners[preconditionInd].setSelection(pInd)
                            }, 100)
                            break
                        }
                    }
                }
                if (paramLabels[predicateLabel]?.last() != null) {
                    for ((pInd, pLabel) in paramLabels[predicateLabel]!!.last().withIndex()) {
                        if (paramLabel.contains(pLabel!!)) {
                            paramSpinners2[preconditionInd].adapter =
                                ArrayAdapter(
                                    context!!,
                                    R.layout.support_simple_spinner_dropdown_item,
                                    paramLabels[predicateLabel]?.last() ?: listOf()
                                )
                            Handler().postDelayed({
                                paramSpinners2[preconditionInd].setSelection(pInd)
                            }, 100)
                            break
                        }
                    }
                }
                checkBoxes[preconditionInd].isChecked = true
                if (preconditionLabel.contains("not($predicateLabel")) {
                    negateCheckBoxes[preconditionInd].isChecked = true
                }
            }
        }
        return updatedRowCount
    }
}