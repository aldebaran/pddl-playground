package com.softbankrobotics.pddlplayground

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import com.softbankrobotics.fastdownwardplanner.IPlannerService
import com.softbankrobotics.pddlplayground.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    private lateinit var plannerServiceConnection: ServiceConnection

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

        plannerServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                println("PlannerService connected")
                plannerService = IPlannerService.Stub.asInterface(service)
            }
            override fun onServiceDisconnected(className: ComponentName) {
                println("Service has unexpectedly disconnected")
            }
        }

        val intent = Intent(ACTION_SEARCH_PLANS_FROM_PDDL).apply {
            `package` = "com.softbankrobotics.fastdownwardplanner"
        }
        bindService(intent, plannerServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        unbindService(plannerServiceConnection)
    }

    companion object {
        const val ACTION_SEARCH_PLANS_FROM_PDDL = "com.softbankrobotics.planning.action.SEARCH_PLANS_FROM_PDDL"
        lateinit var plannerService: IPlannerService
    }
}