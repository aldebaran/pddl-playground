package com.softbankrobotics.pddlplayground.ui.main

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.softbankrobotics.pddlplanning.PDDLPlanningException
import com.softbankrobotics.pddlplanning.PDDLTranslationException
import com.softbankrobotics.pddlplayground.MainActivity.Companion.showInfoFragment
import com.softbankrobotics.pddlplayground.R
import com.softbankrobotics.pddlplayground.adapter.ExpressionAdapter
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.databinding.MainFragmentBinding
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.EditExpressionsReceiver
import com.softbankrobotics.pddlplayground.service.LoadExpressionsReceiver
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.fragment.*
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getDomainPDDLFromDatabase
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getPlanFromDatabase
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getProblemPDDLFromDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.ArrayList

//TODO: make a view for requirements? With checkbox for available requirements & info
class MainFragment : Fragment(),
    LoadExpressionsReceiver.OnExpressionsLoadedListener,
    EditExpressionsReceiver.OnEditExpressionsListener {

    companion object {
        fun newInstance() = MainFragment()

        const val EDIT_EXPRESSION = "edit_expression"
        const val ADD_EXPRESSION = "add_expression"
        const val SHOW_PDDL = "show_pddl"
        const val EDIT_PDDL = "edit_pddl"
    }

    private lateinit var mReceiver: LoadExpressionsReceiver
    private lateinit var mBroadcastReceiver: EditExpressionsReceiver
    private lateinit var expressionAdapter: ExpressionAdapter
    private lateinit var constantAdapter: ExpressionAdapter
    private lateinit var objectAdapter: ExpressionAdapter
    private lateinit var predicateAdapter: ExpressionAdapter
    private lateinit var initAdapter: ExpressionAdapter
    private lateinit var goalAdapter: ExpressionAdapter
    private lateinit var actionAdapter: ExpressionAdapter

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mReceiver = LoadExpressionsReceiver(this)
        mBroadcastReceiver = EditExpressionsReceiver(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        val rootView = binding.root

        // initialize the adapters
        expressionAdapter = ExpressionAdapter()
        constantAdapter = ExpressionAdapter()
        objectAdapter = ExpressionAdapter()
        predicateAdapter = ExpressionAdapter()
        initAdapter = ExpressionAdapter()
        actionAdapter = ExpressionAdapter()
        goalAdapter = ExpressionAdapter()

        // type
        binding.typeRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = expressionAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // constant
        binding.constantRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = constantAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // object
        binding.objectRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = objectAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // predicate
        binding.predicateRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = predicateAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // init
        binding.initRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = initAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // action
        binding.actionRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = actionAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // goal
        binding.goalRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = goalAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // callbacks
        binding.requirementsInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_requirements_title, R.string.info_requirements_summary)
        }
        binding.requirementsPDDL.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.requirements_pddl, R.string.requirements_pddl_content)
        }
        binding.domainInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_domain_title, R.string.info_domain_summary)
        }
        binding.domainPDDL.setOnClickListener {
            showPDDLFragment(getString(R.string.domain_pddl), getDomainPDDLFromDatabase(context!!))
        }
        binding.problemInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_problem_title, R.string.info_problem_summary)
        }
        binding.problemPDDL.setOnClickListener {
            showPDDLFragment(getString(R.string.problem_pddl), getProblemPDDLFromDatabase(context!!))
        }
        binding.addType.setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.TYPE)
        }
        binding.typeInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_type_title, R.string.info_type_summary)
        }
        binding.addConstant.setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.CONSTANT)
        }
        binding.constantInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_const_title, R.string.info_constant_summary)
        }
        binding.addObject.setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.OBJECT)
        }
        binding.objectInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_object_title, R.string.info_object_summary)
        }
        binding.addPredicate.setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.PREDICATE)
        }
        binding.predicateInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_predicate_title, R.string.info_predicate_summary)
        }
        binding.addInit.setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.INIT)
        }
        binding.initInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_init_title, R.string.info_init_summary)
        }
        binding.addAction.setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.ACTION)
        }
        binding.actionInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_action_title, R.string.info_action_summary)
        }
        binding.addGoal.setOnClickListener {
            IntermediateGoalFragment().show(requireActivity().supportFragmentManager, EDIT_PDDL)
        }
        binding.goalInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_goal_title, R.string.info_goal_summary)
        }
        binding.planInfo.setOnClickListener {
            showInfoFragment(this.requireActivity(), R.string.info_plan_title, R.string.info_plan_summary)
        }
        binding.runPlan.setOnClickListener {
            GlobalScope.launch {
                var toastText = "Plan success!"
                val plan = try {
                    getPlanFromDatabase(requireContext())
                } catch (e: PDDLTranslationException) {
                    Timber.e(e)
                    toastText = "Plan failed during PDDL translation."
                    null
                } catch (e: PDDLPlanningException) {
                    Timber.e(e)
                    toastText = "Plan failed; planner could not find a solution."
                    null
                }  catch (t: Throwable) {
                    Timber.e(t)
                    toastText = "Plan failed; unknown error."
                    null
                }
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), toastText, Toast.LENGTH_LONG).show()
                    binding.planContent.text = when {
                        plan == null -> { "Planning failed." }
                        plan.isEmpty() -> { "Plan is empty." }
                        else -> { plan.joinToString("\n") }
                    }
                }
            }
        }
        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        IntentFilter(LoadExpressionsService.ACTION_COMPLETE).also {
            LocalBroadcastManager.getInstance(context!!).registerReceiver(mReceiver, it)
        }
        IntentFilter(EDIT_EXPRESSION).also {
            LocalBroadcastManager.getInstance(context!!).registerReceiver(mBroadcastReceiver, it)
        }
        LoadExpressionsService.launchLoadExpressionsService(context!!)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(mReceiver)
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(mBroadcastReceiver)
    }

    override fun onExpressionsLoaded(expressions: ArrayList<Expression>) {
        expressionAdapter.setExpressions(expressions, PDDLCategory.TYPE)
        constantAdapter.setExpressions(expressions, PDDLCategory.CONSTANT)
        objectAdapter.setExpressions(expressions, PDDLCategory.OBJECT)
        predicateAdapter.setExpressions(expressions, PDDLCategory.PREDICATE)
        initAdapter.setExpressions(expressions, PDDLCategory.INIT)
        goalAdapter.setExpressions(expressions, PDDLCategory.GOAL)
        actionAdapter.setExpressions(expressions, PDDLCategory.ACTION)
    }

    override fun onEditExpressionRequested(expression: Expression) {
        showExpressionFragment(expression, EDIT_EXPRESSION)
    }

    private fun showExpressionFragment(context: Context, category: PDDLCategory) {
        DatabaseHelper.getInstance(context).addExpression().also {
            showExpressionFragment(
                Expression(it, "", category.ordinal, true)
            )
        }
    }

    private fun showExpressionFragment(expression: Expression, action: String = ADD_EXPRESSION) {
        val fragment = when (expression.getCategory()) {
            PDDLCategory.CONSTANT.ordinal, PDDLCategory.OBJECT.ordinal -> {
                ConstantFragment.newInstance(expression, action)
            }
            PDDLCategory.PREDICATE.ordinal -> {
                PredicateFragment.newInstance(expression, action)
            }
            PDDLCategory.INIT.ordinal -> {
                InitFragment.newInstance(expression, action)
            }
            PDDLCategory.ACTION.ordinal -> {
                ActionFragment.newInstance(expression, action)
            }
            PDDLCategory.GOAL.ordinal -> {
                TextFragment.newInstance(expression, action)
            }
            else -> { // type
                TypeFragment.newInstance(expression, action)
            }
        }
        fragment.show(activity!!.supportFragmentManager, EDIT_PDDL)
    }

    private fun showPDDLFragment(title: String, message: String) {
        InfoFragment.newInstance(title, message)
            .show(requireActivity().supportFragmentManager, SHOW_PDDL)
    }
}
