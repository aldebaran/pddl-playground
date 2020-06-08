package com.softbankrobotics.pddlplayground.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.softbankrobotics.pddlplayground.model.Expression
import java.util.ArrayList

class LoadExpressionsReceiver(private val mListener: OnExpressionsLoadedListener): BroadcastReceiver() {

    interface OnExpressionsLoadedListener {
        fun onExpressionsLoaded(expressions: ArrayList<Expression>)
    }

    override fun onReceive(p0: Context, p1: Intent) {
        val expressions: ArrayList<Expression> = p1.getParcelableArrayListExtra(LoadExpressionsService.EXPRESSIONS_EXTRA)
        mListener.onExpressionsLoaded(expressions)
    }
}