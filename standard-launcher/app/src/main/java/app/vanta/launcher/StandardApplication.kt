package app.vanta.launcher

import android.app.Application

class StandardApplication : Application() {
    lateinit var container: app.vanta.launcher.di.AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = app.vanta.launcher.di.AppContainer(this)
        container.scheduleWeatherRefresh()
    }
}
