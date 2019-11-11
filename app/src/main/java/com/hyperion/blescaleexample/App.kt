package com.hyperion.blescaleexample

import android.app.Application
import com.hyperion.blescaleexample.core.OpenScale
import timber.log.Timber


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        OpenScale.createInstance(applicationContext)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}