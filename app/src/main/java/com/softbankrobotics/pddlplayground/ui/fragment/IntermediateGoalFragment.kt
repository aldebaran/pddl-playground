package com.softbankrobotics.pddlplayground.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.databinding.FragmentChooseGoalBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.util.PDDLCategory

class IntermediateGoalFragment: DialogFragment() {

    private var _binding: FragmentChooseGoalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooseGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.basicButton.setOnClickListener {
            DatabaseHelper.getInstance(requireContext()).addExpression().also {
                InitFragment.newInstance(
                    Expression(it, "", PDDLCategory.GOAL.ordinal, true),
                    MainFragment.ADD_EXPRESSION
                ).show(requireActivity().supportFragmentManager, MainFragment.EDIT_PDDL)
            }
            dismiss()
        }
        binding.advancedButton.setOnClickListener {
            DatabaseHelper.getInstance(requireContext()).addExpression().also {
                GoalFragment.newInstance(
                    Expression(it, "", PDDLCategory.GOAL.ordinal, true),
                    MainFragment.ADD_EXPRESSION
                ).show(requireActivity().supportFragmentManager, MainFragment.EDIT_PDDL)
            }
            dismiss()
        }
        binding.manualButton.setOnClickListener {
            DatabaseHelper.getInstance(requireContext()).addExpression().also {
                TextFragment.newInstance(
                    Expression(it, "", PDDLCategory.GOAL.ordinal, true),
                    MainFragment.ADD_EXPRESSION
                ).show(requireActivity().supportFragmentManager, MainFragment.EDIT_PDDL)
            }
            dismiss()
        }
    }
}