package com.softbankrobotics.pddlplayground

import android.app.Application
import timber.log.Timber

class PlaygroundApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}