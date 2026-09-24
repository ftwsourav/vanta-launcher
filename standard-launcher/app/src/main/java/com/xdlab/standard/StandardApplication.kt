package com.xdlab.standard

import android.app.Application

class StandardApplication : Application() {
    lateinit var container: com.xdlab.standard.di.AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = com.xdlab.standard.di.AppContainer(this)
        container.scheduleWeatherRefresh()
    }
}
