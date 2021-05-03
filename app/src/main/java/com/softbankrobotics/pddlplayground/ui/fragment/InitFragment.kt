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
import com.softbankrobotics.pddlplayground.databinding.FragmentEditActionBinding
import com.softbankrobotics.pddlplayground.databinding.FragmentEditInitBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getTypeOfObject
import timber.log.Timber

class InitFragment: DialogFragment() {
    companion object {
        fun newInstance(expression: Expression, action: String): InitFragment {
            val args = Bundle()
            args.putParcelable("expression_extra", expression)
            args.putString("action", action)
            val fragment = InitFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var init: Expression? = null
    private var action: String? = null

    private var _binding: FragmentEditInitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditInitBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init = arguments?.getParcelable("expression_extra")
        action = arguments?.getString("action")
        val predicateSpinner = binding.predicateSpinner
        val objectSpinner = binding.objectSpinner
        val objectSpinner2 = binding.objectSpinner2

        // recover predicates, objects & constants from Database & populate spinners
        val predicates  = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.PREDICATE.ordinal }
            .map { it.getLabel() }
        val objects = DatabaseHelper.getInstance(context!!).getExpressions()
            .filter { it.getCategory() == PDDLCategory.OBJECT.ordinal ||
                    it.getCategory() == PDDLCategory.CONSTANT.ordinal }
            .map { it.getLabel() }
        val predicateLabels = predicates.map { it?.substringBefore(' ') }
        predicateSpinner.adapter =
            ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, predicateLabels)
        if (init != null) { // if filled out before
            // fill in predicate spinner
            val predicate = init?.getLabel()?.substringBefore(' ')
            if (predicateLabels.any { it == predicate }) {
                predicateSpinner.setSelection(predicateLabels.indexOf(predicate))
            }
            // fill in object spinner
            val subExpression = init?.getLabel()?.substringAfter(' ')
            val objLabel = subExpression?.substringBefore(' ')
            if (objLabel != null) { // if parameter exists
                // figure out the type of the object
                val type = getTypeOfObject(objLabel, objects)
                // make a spinner with only the objects of this type
                val objectLabels = objects.filter {
                    it != null && it.contains(type)
                }.map { it?.substringBefore(' ') }
                binding.objectLayout.visibility = View.VISIBLE
                objectSpinner.adapter =
                    ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, objectLabels)
                if (objectLabels.any { it == objLabel }) {
                    objectSpinner.setSelection(objectLabels.indexOf(objLabel))
                }
                val objLabel2 = subExpression.substringAfter(' ')
                if (objLabel2 != subExpression) { // if parameter 2 exists
                    val type2 = getTypeOfObject(objLabel, objects)
                    // make a spinner with only the objects of this type
                    val objectLabels2 = objects.filter {
                        it != null && it.contains(type2)
                    }.map { it?.substringBefore(' ') }
                    binding.objectLayout2.visibility = View.VISIBLE
                    objectSpinner2.adapter =
                        ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, objectLabels2)
                    if (objectLabels2.any { it == objLabel2 }) {
                        objectSpinner2.setSelection(objectLabels2.indexOf(objLabel2))
                    }
                }
            }
        }

        // set the object adapter(s) depending on the predicate chosen
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
                val predicate = predicates[position]
                val numParam = predicate?.count { it == '-' }?: 0
                if (numParam > 0) { // if at least 1 param
                    binding.objectLayout.visibility = View.VISIBLE
                    val type = predicate
                        ?.substringAfter(" - ")
                        ?.substringBefore(' ')
                    val objectLabels = objects.filter {
                        it != null && type != null && it.contains(type)
                    }.map { it?.substringBefore(' ') }
                    objectSpinner.adapter =
                        ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, objectLabels)
                } else {
                    binding.objectLayout.visibility = View.GONE
                }
                if (numParam > 1) { // if 2 params
                    binding.objectLayout2.visibility = View.VISIBLE
                    val type = predicate
                        ?.substringAfterLast(" - ")
                        ?.substringBefore(' ')
                    val objectLabels = objects.filter {
                        it != null && type != null && it.contains(type)
                    }.map { it?.substringBefore(' ') }
                    objectSpinner.adapter =
                        ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, objectLabels)
                } else {
                    binding.objectLayout2.visibility = View.GONE
                }
            }
        }

        binding.okButton.setOnClickListener {
            init?.apply {
                var expression = predicateSpinner.selectedItem as String
                if (binding.objectLayout.visibility == View.VISIBLE)
                    expression += " ${objectSpinner.selectedItem}"
                if (binding.objectLayout2.visibility == View.VISIBLE)
                    expression += " ${objectSpinner2.selectedItem}"
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
            init?.apply {
                DatabaseHelper.getInstance(context!!).deleteExpression(this)
                LoadExpressionsService.launchLoadExpressionsService(context!!)
            }
        }
        super.onDismiss(dialog)
    }
}