package com.softbankrobotics.pddlplayground.ui.main

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softbankrobotics.pddlplayground.MainActivity
import com.softbankrobotics.pddlplayground.MainActivity.Companion.plannerService
import com.softbankrobotics.pddlplayground.R
import com.softbankrobotics.pddlplayground.adapter.ExpressionAdapter
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.service.EditExpressionsReceiver
import com.softbankrobotics.pddlplayground.service.LoadExpressionsReceiver
import com.softbankrobotics.pddlplayground.service.LoadExpressionsService
import com.softbankrobotics.pddlplayground.ui.fragment.*
import com.softbankrobotics.pddlplayground.util.PDDLCategory
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getDomainPDDLFromDatabase
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getPlanFromDatabase
import com.softbankrobotics.pddlplayground.util.PDDLUtil.getProblemPDDLFromDatabase
import kotlinx.android.synthetic.main.main_fragment.*
import java.util.ArrayList

//TODO: make a view for requirements? With checkbox for available requirements & info
//TODO: make an info button in action menu for general explanation
//TODO: fix hard-coded view sizes
class MainFragment : Fragment(),
    LoadExpressionsReceiver.OnExpressionsLoadedListener,
    EditExpressionsReceiver.OnEditExpressionsListener {

    companion object {
        fun newInstance() = MainFragment()
        const val EDIT_EXPRESSION = "edit_expression"
        const val ADD_EXPRESSION = "add_expression"
        const val PROVIDE_INFO = "provide_info"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mReceiver = LoadExpressionsReceiver(this)
        mBroadcastReceiver = EditExpressionsReceiver(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.main_fragment, container, false)

        // initialize the adapters
        expressionAdapter = ExpressionAdapter()
        constantAdapter = ExpressionAdapter()
        objectAdapter = ExpressionAdapter()
        predicateAdapter = ExpressionAdapter()
        initAdapter = ExpressionAdapter()
        actionAdapter = ExpressionAdapter()
        goalAdapter = ExpressionAdapter()

        // type
        rootView.findViewById<RecyclerView>(R.id.type_recyler).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = expressionAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // constant
        rootView.findViewById<RecyclerView>(R.id.constant_recyler).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = constantAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // object
        rootView.findViewById<RecyclerView>(R.id.object_recyler).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = objectAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // predicate
        rootView.findViewById<RecyclerView>(R.id.predicate_recyler).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = predicateAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // init
        rootView.findViewById<RecyclerView>(R.id.init_recyler).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = initAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // action
        rootView.findViewById<RecyclerView>(R.id.action_recyler).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = actionAdapter
            itemAnimator = DefaultItemAnimator()
        }

        // goal
        rootView.findViewById<RecyclerView>(R.id.goal_recyler).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = goalAdapter
            itemAnimator = DefaultItemAnimator()
        }


        // callbacks
        rootView.findViewById<ImageButton>(R.id.domainInfo).setOnClickListener {
            showInfoFragment(R.string.info_domain_title, R.string.info_domain_summary)
        }
        rootView.findViewById<ImageButton>(R.id.domainPDDL).setOnClickListener {
            showPDDLFragment(getString(R.string.domain_pddl), getDomainPDDLFromDatabase(context!!))
        }
        rootView.findViewById<ImageButton>(R.id.problemInfo).setOnClickListener {
            showInfoFragment(R.string.info_problem_title, R.string.info_problem_summary)
        }
        rootView.findViewById<ImageButton>(R.id.problemPDDL).setOnClickListener {
            showPDDLFragment(getString(R.string.problem_pddl), getProblemPDDLFromDatabase(context!!))
        }
        rootView.findViewById<ImageButton>(R.id.addType).setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.TYPE)
        }
        rootView.findViewById<ImageButton>(R.id.typeInfo).setOnClickListener {
            showInfoFragment(R.string.info_type_title, R.string.info_type_summary)
        }
        rootView.findViewById<ImageButton>(R.id.addConstant).setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.CONSTANT)

        }
        rootView.findViewById<ImageButton>(R.id.constantInfo).setOnClickListener {
            showInfoFragment(R.string.info_const_title, R.string.info_constant_summary)
        }
        rootView.findViewById<ImageButton>(R.id.addObject).setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.OBJECT)

        }
        rootView.findViewById<ImageButton>(R.id.objectInfo).setOnClickListener {
            showInfoFragment(R.string.info_object_title, R.string.info_object_summary)
        }
        rootView.findViewById<ImageButton>(R.id.addPredicate).setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.PREDICATE)

        }
        rootView.findViewById<ImageButton>(R.id.predicateInfo).setOnClickListener {
            showInfoFragment(R.string.info_predicate_title, R.string.info_predicate_summary)
        }
        rootView.findViewById<ImageButton>(R.id.addInit).setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.INIT)

        }
        rootView.findViewById<ImageButton>(R.id.initInfo).setOnClickListener {
            showInfoFragment(R.string.info_init_title, R.string.info_init_summary)
        }
        rootView.findViewById<ImageButton>(R.id.addAction).setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.ACTION)
        }
        rootView.findViewById<ImageButton>(R.id.actionInfo).setOnClickListener {
            showInfoFragment(R.string.info_action_title, R.string.info_action_summary)
        }
        rootView.findViewById<ImageButton>(R.id.addGoal).setOnClickListener {
            showExpressionFragment(context!!, PDDLCategory.GOAL)
        }
        rootView.findViewById<ImageButton>(R.id.goalInfo).setOnClickListener {
            showInfoFragment(R.string.info_goal_title, R.string.info_goal_summary)
        }
        rootView.findViewById<ImageButton>(R.id.planInfo).setOnClickListener {
            showInfoFragment(R.string.info_plan_title, R.string.info_plan_summary)
        }
        rootView.findViewById<ImageButton>(R.id.runPlan).setOnClickListener {
            val plan = getPlanFromDatabase(context!!)
            planContent.text = when {
                plan == null -> { "Plan failed." }
                plan.isEmpty() -> { "Plan is empty." }
                else -> { plan }
            }
        }
        return rootView
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
            PDDLCategory.ACTION.ordinal -> { //TODO: make action fragment
                ExpressionFragment.newInstance(expression, action)
            }
            PDDLCategory.GOAL.ordinal -> {
                GoalFragment.newInstance(expression, action)
            }
            else -> { // type
                ExpressionFragment.newInstance(expression, action)
            }
        }
        fragment.show(activity!!.supportFragmentManager, EDIT_PDDL)
    }

    private fun showInfoFragment(title: Int, message: Int) {
        InfoFragment.newInstance(getString(title), getString(message))
            .show(activity!!.supportFragmentManager, PROVIDE_INFO)
    }

    private fun showPDDLFragment(title: String, message: String) {
        InfoFragment.newInstance(title, message)
            .show(activity!!.supportFragmentManager, SHOW_PDDL)
    }
}
