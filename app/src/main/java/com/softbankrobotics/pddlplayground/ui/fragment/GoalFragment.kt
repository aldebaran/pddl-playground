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
import com.softbankrobotics.pddlplayground.databinding.FragmentEditGoalBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.ui.main.MainFragment.Companion.EDIT_EXPRESSION
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import com.softbankrobotics.pddlplayground.util.PDDLUtil
import timber.log.Timber

class GoalFragment : DialogFragment() {
    companion object {
        fun newInstance(expression: Expression, action: String): GoalFragment {
            val args = Bundle()
            args.putParcelable("expression_extra", expression)
            args.putString("action", action)
            val fragment = GoalFragment()
            fragment.arguments = args
            return fragment
        }
        private const val FORALL = "forall"
        private const val IMPLY = "imply"
        private const val EXISTS = "exists"
        private const val NONE = "(none)"
        private val operators = listOf(FORALL, IMPLY, NONE)
        private val expressionOperators = listOf(IMPLY, EXISTS)
    }

    private var goal: Expression? = null
    private var action: String? = null
    private var predicateParams = mapOf<String, List<List<String?>>>()

    // view elements for parameters, accessible by view elements for preconditions and effects
    private val parameterTexts = mutableListOf<EditText>()
    private val parameterCheckBoxes = mutableListOf<CheckBox>()
    private val parameterSpinners = mutableListOf<Spinner>()

    private var _binding: FragmentEditGoalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditGoalBinding.inflate(inflater, container, false)
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

        // view elements for predicates
        val predicateCheckBoxes = mutableListOf<CheckBox>()
        val predicateSpinners = mutableListOf<Spinner>()
        val predicateParamSpinners = mutableListOf<Spinner>()
        val predicateParamSpinners2 = mutableListOf<Spinner>()
        val negateCheckBoxes = mutableListOf<CheckBox>()

        // view elements for expressions
        val expressionCheckBoxes = mutableListOf<CheckBox>()
        val expressionSpinners = mutableListOf<Spinner>()
        val expressionParamSpinners = mutableListOf<Spinner>()
        val expressionParamSpinners2 = mutableListOf<Spinner>()
        val expressionNegateCheckBoxes = mutableListOf<CheckBox>()

        // view elements for goal labels
        val keywordSpinner = Spinner(context)
        val labelSpinner = Spinner(context)
        val labelSpinner2 = Spinner(context)

        // get the parameters
        predicateParams = PDDLUtil.getParamsForPredicates(
            parameterCheckBoxes,
            parameterTexts,
            parameterSpinners,
            requireContext(),
            true
        )

        // populate the parameter grid (first row)
        val typeLabels = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.TYPE.ordinal }
            .map { it.getLabel()?.substringBefore(" - ") }
        var paramRowCount = 1
        addParameterRow(
            binding.gridLayout,
            typeLabels,
            paramRowCount
        )
        paramRowCount++

        // populate the predicate grid (first row)
        val predicateLabels = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
            .map { it?.substringBefore(' ') }
        var predicateRowCount = 1
        addPredicateRow(
            binding.gridLayout2,
            predicateLabels,
            predicateCheckBoxes,
            predicateSpinners,
            predicateParamSpinners,
            predicateParamSpinners2,
            negateCheckBoxes,
            predicateRowCount
        )
        predicateRowCount++

        // populate the expression grid (first row)
        var expressionRowCount = 1
        addExpressionRow(
            binding.gridLayout4,
            expressionCheckBoxes,
            expressionSpinners,
            expressionParamSpinners,
            expressionParamSpinners2,
            expressionNegateCheckBoxes,
            predicateCheckBoxes,
            predicateSpinners,
            predicateParamSpinners,
            predicateParamSpinners2,
            negateCheckBoxes,
            expressionRowCount
        )
        expressionRowCount++

        if (goal != null) { // if filled out before TODO
            val label = goal?.getLabel()
            when (val operator = label?.substringBefore(' ')) {
                FORALL, IMPLY -> {
                    keywordSpinner.setSelection(operators.indexOf(operator))
                }
                else -> { // simple case
                    predicateRowCount = fillInPredicateRows(
                        label!!,
                        binding.gridLayout2,
                        predicateLabels,
                        predicateCheckBoxes,
                        predicateSpinners,
                        predicateParamSpinners,
                        predicateParamSpinners2,
                        negateCheckBoxes,
                        predicateRowCount
                    )
                    keywordSpinner.setSelection(operators.indexOf(NONE))
                }
            }
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "autofill for goals coming soon.",
                    Toast.LENGTH_LONG).show()
            }
        }

        // populate the goal grid
        addGoalRow(
            binding.gridLayout3,
            keywordSpinner,
            labelSpinner,
            labelSpinner2,
            predicateCheckBoxes,
            predicateSpinners,
            predicateParamSpinners,
            predicateParamSpinners2,
            negateCheckBoxes,
            expressionCheckBoxes,
            expressionSpinners,
            expressionParamSpinners,
            expressionParamSpinners2,
            expressionNegateCheckBoxes
        )

        // add a parameter row
        binding.addParamButton.setOnClickListener {
            addParameterRow(
                binding.gridLayout,
                typeLabels,
                paramRowCount
            )
            paramRowCount++
        }

        // add a predicate row
        binding.addPredicateButton.setOnClickListener {
            addPredicateRow(
                binding.gridLayout2,
                predicateLabels,
                predicateCheckBoxes,
                predicateSpinners,
                predicateParamSpinners,
                predicateParamSpinners2,
                negateCheckBoxes,
                predicateRowCount
            )
            predicateRowCount++
        }

        // add an expression row
        binding.addExpressionButton.setOnClickListener {
            addExpressionRow(
                binding.gridLayout4,
                expressionCheckBoxes,
                expressionSpinners,
                expressionParamSpinners,
                expressionParamSpinners2,
                expressionNegateCheckBoxes,
                predicateCheckBoxes,
                predicateSpinners,
                predicateParamSpinners,
                predicateParamSpinners2,
                negateCheckBoxes,
                expressionRowCount
            )
            expressionRowCount++
        }

        binding.okButton.setOnClickListener {
            val keyword = keywordSpinner.selectedItem as String
            val expression = if (keyword == NONE) {
                labelSpinner.selectedItem
            } else {
                "$keyword (${labelSpinner.selectedItem}) (${labelSpinner2.selectedItem})"
            } as String
            goal?.apply {
                setLabel(expression)
                DatabaseHelper.getInstance(requireContext()).updateExpression(this)
            }
            LoadExpressionsService.launchLoadExpressionsService(requireContext())
            action = EDIT_EXPRESSION
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

    private fun addPredicateRow(
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
                predicateParams = PDDLUtil.getParamsForPredicates(
                    parameterCheckBoxes,
                    parameterTexts,
                    parameterSpinners,
                    requireContext(),
                    true
                )

                val predicates = DatabaseHelper.getInstance(context!!).getExpressions()
                    .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
                    .map { it.getLabel() }
                val predicateLabel = predicates[position]?.substringBefore(' ')

                paramSpinner.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    predicateParams[predicateLabel!!]?.first() ?: listOf()
                )
                paramSpinner2.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    predicateParams[predicateLabel]?.last() ?: listOf()
                )
            }
        }
    }

    private fun fillInPredicateRows(
        label: String,
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
        if (rowCount != 2) {
            addPredicateRow(
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
        val predicateLabel = label.substringBefore(' ')
        predicateSpinners[rowCount-2].setSelection(
            predicateLabels.indexOfFirst { it == predicateLabel }
        )
        // fill in param spinners
        val paramLabel = label.substringAfter(predicateLabel)
        if (predicateParams[predicateLabel]?.first() != null) {
            for ((pInd, pLabel) in predicateParams[predicateLabel]!!.first().withIndex()) {
                if (paramLabel.contains(pLabel!!)) {
                    paramSpinners[rowCount-2].adapter =
                        ArrayAdapter(
                            context!!,
                            R.layout.support_simple_spinner_dropdown_item,
                            predicateParams[predicateLabel]?.first() ?: listOf()
                        )
                    Handler().postDelayed({
                        paramSpinners[rowCount-2].setSelection(pInd)
                    }, 200)
                    break
                }
            }
        }
        if (predicateParams[predicateLabel]?.last() != null) {
            for ((pInd, pLabel) in predicateParams[predicateLabel]!!.last().withIndex()) {
                if (paramLabel.contains(pLabel!!)) {
                    paramSpinners2[rowCount-2].adapter =
                        ArrayAdapter(
                            context!!,
                            R.layout.support_simple_spinner_dropdown_item,
                            predicateParams[predicateLabel]?.last() ?: listOf()
                        )
                    Handler().postDelayed({
                        paramSpinners2[rowCount-2].setSelection(pInd)
                    }, 200)
                    break
                }
            }
        }
        checkBoxes[rowCount-2].isChecked = true
        if (label.contains("not($predicateLabel")) {
            negateCheckBoxes[rowCount-2].isChecked = true
        }
        return updatedRowCount
    }

    private fun addExpressionRow(
        grid: GridLayout,
        checkBoxes: MutableList<CheckBox>,
        keywordSpinners: MutableList<Spinner>,
        paramSpinners: MutableList<Spinner>,
        paramSpinners2: MutableList<Spinner>,
        negateCheckBoxes: MutableList<CheckBox>,
        predicateCheckBoxes: MutableList<CheckBox>,
        predicateSpinners: MutableList<Spinner>,
        predicateParamSpinners: MutableList<Spinner>,
        predicateParamSpinners2: MutableList<Spinner>,
        predicateNegateCheckBoxes: MutableList<CheckBox>,
        rowCount: Int
    ) {
        // column 1
        val checkBox = CheckBox(context)
        checkBoxes += checkBox
        grid.addView(
            checkBox,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(0, GridLayout.CENTER)
            )
        )

        // column 2
        val keywordSpinner = Spinner(context)
        keywordSpinner.adapter =
            ArrayAdapter(
                context!!,
                R.layout.support_simple_spinner_dropdown_item,
                expressionOperators.plus("")
            )
        keywordSpinner.setSelection(expressionOperators.size)
        keywordSpinners += keywordSpinner
        grid.addView(
            keywordSpinner,
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
        val negateCheckBox = CheckBox(context)
        negateCheckBoxes += negateCheckBox
        grid.addView(
            negateCheckBox,
            GridLayout.LayoutParams(
                GridLayout.spec(rowCount, GridLayout.CENTER),
                GridLayout.spec(4, GridLayout.CENTER)
            )
        )

        // listener for keyword spinner
        keywordSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Timber.i("Nothing selected.")
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position >= operators.size) {
                    Timber.d("Selected empty item.")
                    return
                }

                // get the latest parameters
                val paramList = PDDLUtil.getParamsForGoals(
                    parameterCheckBoxes,
                    parameterTexts,
                    parameterSpinners
                )

                // get the latest predicates
                val predicateList = mutableListOf<String>()
                for ((index, predicateCheckBox) in predicateCheckBoxes.withIndex()) {
                    if (predicateCheckBox.isChecked) {
                        val pPredicate = (predicateSpinners[index].selectedItem as String?) ?: ""
                        if (pPredicate.isNotEmpty()) {
                            val param = (predicateParamSpinners[index].selectedItem as String?) ?: ""
                            val param2 = (predicateParamSpinners2[index].selectedItem as String?) ?: ""
                            predicateList += if (predicateNegateCheckBoxes[index].isChecked) {
                                "not($pPredicate $param $param2)"
                            } else {
                                "$pPredicate $param $param2"
                            }
                        }
                    }
                }

                val (labelList, labelList2) = when (position) {
                    0 -> { // imply
                        Pair(predicateList, predicateList)
                    }
                    1 -> { // exists
                        Pair(paramList, predicateList)
                    }
                    else -> {
                        Pair(listOf(), listOf<String>())
                    }
                }
                paramSpinner.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    labelList
                )
                paramSpinner2.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    labelList2
                )
            }
        }
    }

    private fun addGoalRow(
        grid: GridLayout,
        keywordSpinner: Spinner,
        labelSpinner: Spinner,
        labelSpinner2: Spinner,
        predicateCheckBoxes: MutableList<CheckBox>,
        predicateSpinners: MutableList<Spinner>,
        predicateParamSpinners: MutableList<Spinner>,
        predicateParamSpinners2: MutableList<Spinner>,
        negateCheckBoxes: MutableList<CheckBox>,
        expressionCheckBoxes: MutableList<CheckBox>,
        expressionSpinners: MutableList<Spinner>,
        expressionParamSpinners: MutableList<Spinner>,
        expressionParamSpinners2: MutableList<Spinner>,
        expressionNegateCheckBoxes: MutableList<CheckBox>
    ) {
        // populate the keyword spinner
        keywordSpinner.adapter =
            ArrayAdapter(
                requireContext(),
                R.layout.support_simple_spinner_dropdown_item,
                operators.plus("")
            )
        keywordSpinner.setSelection(operators.size)
        grid.addView(
            keywordSpinner,
            GridLayout.LayoutParams(
                GridLayout.spec(1, GridLayout.CENTER),
                GridLayout.spec(0, GridLayout.CENTER)
            )
        )
        grid.addView(
            labelSpinner,
            GridLayout.LayoutParams(
                GridLayout.spec(1, GridLayout.CENTER),
                GridLayout.spec(1, GridLayout.CENTER)
            )
        )
        grid.addView(
            labelSpinner2,
            GridLayout.LayoutParams(
                GridLayout.spec(1, GridLayout.CENTER),
                GridLayout.spec(2, GridLayout.CENTER)
            )
        )

        // listener for keyword spinner
        keywordSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Timber.i("Nothing selected.")
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position >= operators.size) {
                    Timber.d("Selected empty item.")
                    return
                }

                // get the latest parameters
                val paramList = PDDLUtil.getParamsForGoals(
                    parameterCheckBoxes,
                    parameterTexts,
                    parameterSpinners
                )

                // get the latest predicates
                val predicateList = mutableListOf<String>()
                for ((index, checkBox) in predicateCheckBoxes.withIndex()) {
                    if (checkBox.isChecked) {
                        val pPredicate = (predicateSpinners[index].selectedItem as String?) ?: ""
                        if (pPredicate.isNotEmpty()) {
                            val param = (predicateParamSpinners[index].selectedItem as String?) ?: ""
                            val param2 = (predicateParamSpinners2[index].selectedItem as String?) ?: ""
                            predicateList += if (negateCheckBoxes[index].isChecked) {
                                "not($pPredicate $param $param2)"
                            } else {
                                "$pPredicate $param $param2"
                            }
                        }
                    }
                }

                // get the latest expressions
                val expressionList = mutableListOf<String>()
                for ((index, checkBox) in expressionCheckBoxes.withIndex()) {
                    if (checkBox.isChecked) {
                        val keyword = (expressionSpinners[index].selectedItem as String?) ?: ""
                        if (keyword.isNotEmpty()) {
                            val param = (expressionParamSpinners[index].selectedItem as String?) ?: ""
                            val param2 = (expressionParamSpinners2[index].selectedItem as String?) ?: ""
                            expressionList += if (expressionNegateCheckBoxes[index].isChecked) {
                                "not($keyword ($param) ($param2))"
                            } else {
                                "$keyword ($param) ($param2)"
                            }
                        }
                    }
                }

                val (labelList, labelList2) = when (position) {
                    0 -> { // forall
                        Pair(paramList, predicateList.plus(expressionList))
                    }
                    1 -> { // imply
                        Pair(predicateList.plus(expressionList), predicateList.plus(expressionList))

                    }
                    else -> { // no keyword
                        Pair(predicateList.plus(expressionList), listOf())
                    }
                }
                labelSpinner.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    labelList
                )
                labelSpinner2.adapter = ArrayAdapter(
                    context!!,
                    R.layout.support_simple_spinner_dropdown_item,
                    labelList2
                )
            }
        }
    }
}