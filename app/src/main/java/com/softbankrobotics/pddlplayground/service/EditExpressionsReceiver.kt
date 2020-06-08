package com.softbankrobotics.pddlplayground.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.softbankrobotics.pddlplayground.model.Expression
import com.softbankrobotics.pddlplayground.ui.main.MainFragment

class EditExpressionsReceiver(private val mListener: OnEditExpressionsListener): BroadcastReceiver() {

    interface OnEditExpressionsListener {
        fun onEditExpressionRequested(expression: Expression)
    }

    override fun onReceive(p0: Context, p1: Intent) {
        val expression: Expression = p1.getParcelableExtra(MainFragment.EDIT_EXPRESSION)
        mListener.onEditExpressionRequested(expression)
    }
}