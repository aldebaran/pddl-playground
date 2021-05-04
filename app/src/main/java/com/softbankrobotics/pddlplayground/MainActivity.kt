package com.softbankrobotics.pddlplayground

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.softbankrobotics.pddlplanning.IPDDLPlannerService
import com.softbankrobotics.pddlplanning.PlanSearchFunction
import com.softbankrobotics.pddlplanning.createPlanSearchFunctionFromService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch {
            val plannerServiceIntent = Intent(IPDDLPlannerService.ACTION_SEARCH_PLANS_FROM_PDDL)
            plannerServiceIntent.`package` = "com.softbankrobotics.fastdownward"
            planSearchFunction =
                createPlanSearchFunctionFromService(this@MainActivity, plannerServiceIntent)
        }
    }

    companion object {
        lateinit var planSearchFunction: PlanSearchFunction
    }
}