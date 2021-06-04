package com.softbankrobotics.pddlplayground

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import com.softbankrobotics.pddlplanning.IPDDLPlannerService
import com.softbankrobotics.pddlplanning.PlanSearchFunction
import com.softbankrobotics.pddlplanning.createPlanSearchFunctionFromService
import com.softbankrobotics.pddlplayground.ui.fragment.InfoFragment
import com.softbankrobotics.pddlplayground.ui.main.MainFragment
import com.softbankrobotics.pddlplayground.util.PDDLUtil.fillInDatabaseWithSample
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_info -> {
            showInfoFragment(this, R.string.info_title, R.string.info_general_summary)
            true
        }
        R.id.action_glossary -> {
            showInfoFragment(this, R.string.info_glossary_title, R.string.info_glossary_summary)
            true
        }
        R.id.action_help -> {
            showInfoFragment(this, R.string.info_help_title, R.string.info_help_summary)
            true
        }
        R.id.action_attribution -> {
            showInfoFragment(this, R.string.info_attribution_title, R.string.info_attribution_summary)
            true
        }
        R.id.action_import -> {
            fillInDatabaseWithSample(applicationContext)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }


    companion object {
        lateinit var planSearchFunction: PlanSearchFunction
        private const val PROVIDE_INFO = "provide_info"

        fun showInfoFragment(activity: FragmentActivity, title: Int, message: Int) {
            InfoFragment.newInstance(activity.getString(title), activity.getText(message))
                .show(activity.supportFragmentManager, PROVIDE_INFO)
        }
    }
}