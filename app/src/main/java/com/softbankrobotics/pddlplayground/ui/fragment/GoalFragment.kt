package com.softbankrobotics.pddlplayground.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.R
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.databinding.FragmentEditGoalBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.ui.main.MainFragment.Companion.EDIT_EXPRESSION
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import timber.log.Timber

class GoalFragment: DialogFragment() {
    companion object {
        fun newInstance(expression: Expression, action: String): GoalFragment {
            val args = Bundle()
            args.putParcelable("expression_extra", expression)
            args.putString("action", action)
            val fragment = GoalFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var goal: Expression? = null
    private var action: String? = null
    private val inits = mutableListOf<String?>()
    private var manual = false

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

        // get view elements
        val keywordSpinner = binding.keywordSpinner
        val initSpinner = binding.initSpinner
        val initSpinner2 = binding.initSpinner2

        // populate keyword spinner
        val keywords = listOf(
            "",
            "imply" //TODO: add more e.g. exists, forall
        )
        keywordSpinner.adapter =
            ArrayAdapter(context!!,  R.layout.support_simple_spinner_dropdown_item, keywords)

        if (goal != null) { // if filled out before
            // fill in keyword
            val keyword = goal?.getLabel()?.substringBefore(' ')
            if (keywords.any { it == keyword }) {
                keywordSpinner.setSelection(keywords.indexOf(keyword))
            }

            // recover inits from Database and add negation
            val init  = DatabaseHelper.getInstance(context!!).getExpressions()
                .filter { it.getCategory() == PDDLCategory.INIT.ordinal }
                .map { it.getLabel() }
            init.forEach {
                inits.add(it)
                inits.add("not($it)")
            }

            // fill in
            when (keyword) {
                "imply" -> {
                    initSpinner.adapter =
                        ArrayAdapter(context!!,  R.layout.support_simple_spinner_dropdown_item, inits)
                    initSpinner2.adapter =
                        ArrayAdapter(context!!,  R.layout.support_simple_spinner_dropdown_item, inits)
                    binding.expressionLayout.visibility = View.VISIBLE
                    binding.expressionLayout2.visibility = View.VISIBLE
                }
                else -> { // for now, just null
                    initSpinner.adapter =
                        ArrayAdapter(context!!,  R.layout.support_simple_spinner_dropdown_item, inits)
                    binding.expressionLayout.visibility = View.VISIBLE
                    binding.expressionLayout2.visibility = View.GONE
                }
            }
        }

        // set the object adapter(s) depending on the predicate chosen
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
                when (keywords[position]) {
                    "imply" -> {
                        initSpinner.adapter =
                            ArrayAdapter(context!!,  R.layout.support_simple_spinner_dropdown_item, inits)
                        initSpinner2.adapter =
                            ArrayAdapter(context!!,  R.layout.support_simple_spinner_dropdown_item, inits)
                        binding.expressionLayout.visibility = View.VISIBLE
                        binding.expressionLayout2.visibility = View.VISIBLE
                    }
                    else -> { // for now, just null
                        initSpinner.adapter =
                            ArrayAdapter(context!!,  R.layout.support_simple_spinner_dropdown_item, inits)
                        binding.expressionLayout.visibility = View.VISIBLE
                        binding.expressionLayout2.visibility = View.GONE
                    }
                }
            }
        }

        binding.okButton.setOnClickListener {
            goal?.apply {
                var expression: String
                if (manual) {
                    expression = binding.goalText.text.toString()
                } else {
                    expression = keywordSpinner.selectedItem as String
                    if (binding.expressionLayout.visibility == View.VISIBLE)
                        expression += " (${initSpinner.selectedItem})"
                    if (binding.expressionLayout2.visibility == View.VISIBLE)
                        expression += " (${initSpinner2.selectedItem})"
                }
                setLabel(expression)
                DatabaseHelper.getInstance(context!!).updateExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
                action = EDIT_EXPRESSION
            }
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.manualButton.setOnClickListener {
            binding.autoFillGoal.visibility = View.GONE
            binding.goalText.visibility = View.VISIBLE
            binding.goalText.setText(goal?.getLabel())
            manual = true
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
}