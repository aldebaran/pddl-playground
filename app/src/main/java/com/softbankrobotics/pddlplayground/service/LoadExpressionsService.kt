package com.softbankrobotics.pddlplayground.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.softbankrobotics.pddlplayground.data.DatabaseHelper
import com.softbankrobotics.pddlplayground.model.Expression
import java.util.ArrayList

class LoadExpressionsService: JobIntentService() {

    companion object {
        private val TAG = LoadExpressionsService::class.java.simpleName
        val ACTION_COMPLETE = "$TAG.ACTION_COMPLETE"
        const val EXPRESSIONS_EXTRA = "expressions_extra"
        const val JOB_ID = 1000

        fun launchLoadExpressionsService(context: Context) {
            val launchLoadExpressionsServiceIntent = Intent(context, LoadExpressionsService::class.java)
            enqueueWork(context, LoadExpressionsService::class.java, JOB_ID, launchLoadExpressionsServiceIntent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val expressions: List<Expression> = DatabaseHelper.getInstance(this).getExpressions()
        val i = Intent(ACTION_COMPLETE)
        i.putParcelableArrayListExtra(EXPRESSIONS_EXTRA, ArrayList<Expression>(expressions))
        LocalBroadcastManager.getInstance(this).sendBroadcast(i)
    }
}