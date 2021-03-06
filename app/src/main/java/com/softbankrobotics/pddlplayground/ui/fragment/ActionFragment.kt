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
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getParamsForPredicates
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getTypes
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
    private var predicateParams = mapOf<String, List<Set<String?>>>()

    // view elements for parameters, accessible by view elements for preconditions and effects
    private val parameterTexts = mutableListOf<EditText>()
    private val parameterCheckBoxes = mutableListOf<CheckBox>()
    private val parameterSpinners = mutableListOf<Spinner>()

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

        // view elements for preconditions and effects
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

        // populate the parameter grid (first row)
        val typeLabels = getTypes(requireContext())
        var paramRowCount = 1
        addParameterRow(
            binding.gridLayout3,
            typeLabels,
            paramRowCount
        )
        paramRowCount++

        // if action was filled out before, populate the parameter interface first
        if (paction?.getLabel()?.isNotEmpty() == true) {
            val label = paction?.getLabel()
            binding.actionText.setText(label?.substringBefore('\n'))
            // fill in checkboxes & spinners
            val params = label?.substringAfter("parameters (\n")
                ?.substringBefore(":precondition")

            // loop through parameters
            paramRowCount = fillInParameterRows(
                params,
                binding.gridLayout3,
                typeLabels,
                paramRowCount
            )
        }

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
            effectCheckBoxes,
            effectPredicateSpinners,
            effectSpinners,
            effectSpinners2,
            enCheckBoxes,
            eRowCount
        )
        pRowCount++
        eRowCount++

        if (paction?.getLabel()?.isNotEmpty() == true) {
            val label = paction?.getLabel()
            val preconditions = label?.substringAfter("precondition (and\n")
                ?.substringBefore(":effect")
            val effects = label?.substringAfter("effect (and\n")

            // loop through preconditions
            pRowCount = fillInRows(
                preconditions,
                binding.gridLayout,
                predicateLabels,
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
                effectCheckBoxes,
                effectPredicateSpinners,
                effectSpinners,
                effectSpinners2,
                enCheckBoxes,
                eRowCount
            )
        }

        // add a parameter row
        binding.addParamButton.setOnClickListener {
            addParameterRow(
                binding.gridLayout3,
                typeLabels,
                paramRowCount
            )
            paramRowCount++
        }

        // add a precondition row
        binding.addPreconditionButton.setOnClickListener {
            addRow(
                binding.gridLayout,
                predicateLabels,
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
            val actionLabel = binding.actionText.text
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
            var effectExists = false
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
                        effectExists = true
                    }
                }
            }
            effect += "    )\n"
            // parameters (only add when it's actually used)
            var parameters = "    :parameters (\n"
            for ((index, paramCheckBox) in parameterCheckBoxes.withIndex()) {
                if (paramCheckBox.isChecked) {
                    val paramText = "?${parameterTexts[index].text}"
                    if (paramList.any {it == paramText}) {
                        val typeLabel = parameterSpinners[index].selectedItem as String? ?: ""
                        parameters += "      $paramText - $typeLabel\n"
                    }
                }
            }
            parameters += "    )\n"
            if (actionLabel.isEmpty() || !effectExists) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "An action must not have an empty label or effect.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@setOnClickListener
            }
            paction?.apply {
                setLabel("$actionLabel\n" + parameters + precondition + effect)
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
        predicateSpinner.setSelection(predicateLabels.size)
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

                // get the latest parameters
                predicateParams = getParamsForPredicates(
                    parameterCheckBoxes,
                    parameterTexts,
                    parameterSpinners,
                    requireContext()
                )

                val predicates = DatabaseHelper.getInstance(context!!).getExpressions()
                    .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
                    .map { it.getLabel() }
                val predicateLabel = predicates[position]?.substringBefore(' ')

                paramSpinner.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    predicateParams[predicateLabel!!]?.first()?.toList() ?: listOf()
                )
                paramSpinner2.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    predicateParams[predicateLabel]?.last()?.toList() ?: listOf()
                )
            }
        }
    }

    private fun fillInRows(
        label: String?,
        grid: GridLayout,
        predicateLabels: List<String?>,
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
                if (predicateParams[predicateLabel]?.first() != null) {
                    for ((pInd, pLabel) in predicateParams[predicateLabel]!!.first().withIndex()) {
                        if (paramLabel.contains(pLabel!!)) {
                            paramSpinners[preconditionInd].adapter =
                                ArrayAdapter(
                                    context!!,
                                    R.layout.support_simple_spinner_dropdown_item,
                                    predicateParams[predicateLabel]?.first()?.toList() ?: listOf()
                                )
                            Handler().postDelayed({
                                paramSpinners[preconditionInd].setSelection(pInd)
                            }, 200)
                            break
                        }
                    }
                }
                if (predicateParams[predicateLabel]?.last() != null) {
                    for ((pInd, pLabel) in predicateParams[predicateLabel]!!.last().withIndex()) {
                        if (paramLabel.contains(pLabel!!)) {
                            paramSpinners2[preconditionInd].adapter =
                                ArrayAdapter(
                                    context!!,
                                    R.layout.support_simple_spinner_dropdown_item,
                                    predicateParams[predicateLabel]?.last()?.toList() ?: listOf()
                                )
                            Handler().postDelayed({
                                paramSpinners2[preconditionInd].setSelection(pInd)
                            }, 200)
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

    private fun fillInParameterRows(
        label: String?,
        grid: GridLayout,
        typeLabels: List<String?>,
        rowCount: Int
    ): Int {
        var updatedRowCount = rowCount
        val paramLabels = label?.split("\n")?.dropLast(1)
        if (!paramLabels.isNullOrEmpty()) {
            for ((index, paramLabel) in paramLabels.withIndex()) {
                if (paramLabel.contains('?')) { // don't include constants
                    if (index != 0) {
                        addParameterRow(
                            grid,
                            typeLabels,
                            updatedRowCount
                        )
                        updatedRowCount++
                    }
                    // fill in the param text
                    parameterTexts[index].setText(
                        paramLabel.substringAfter('?').substringBefore(" - ")
                    )
                    // fill in the type
                    val typeLabel = paramLabel.substringAfter(" - ")
                    parameterSpinners[index].setSelection(
                        typeLabels.indexOfFirst { it == typeLabel }
                    )
                    parameterCheckBoxes[index].isChecked = true

                    // update predicateParams once all parameters are filled
                    predicateParams = getParamsForPredicates(
                        parameterCheckBoxes,
                        parameterTexts,
                        parameterSpinners,
                        requireContext()
                    )
                }
            }
        }
        return updatedRowCount
    }

    private fun addParameterRow(
        grid: GridLayout,
        typeLabels: List<String?>,
        rowCount: Int
    ) {
        // column 1
        val checkBox = CheckBox(context)
        parameterCheckBoxes += checkBox
        grid.addView(
            checkBox,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(0, GridLayout.CENTER)
            )
        )

        // column 2
        val paramText = EditText(context)
        paramText.hint = "e.g. human1"
        parameterTexts += paramText
        grid.addView(
            paramText,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(1, GridLayout.FILL)
            )
        )

        // column 3
        val paramSpinner = Spinner(context)
        paramSpinner.adapter =
            ArrayAdapter(
                context!!,
                R.layout.support_simple_spinner_dropdown_item,
                typeLabels
            )
        parameterSpinners += paramSpinner
        grid.addView(
            paramSpinner,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(2, GridLayout.CENTER)
            )
        )

    }
}