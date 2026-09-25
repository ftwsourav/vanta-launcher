package app.vanta.launcher.di

import android.content.Context
import app.vanta.launcher.data.local.BatteryRepositoryImpl
import app.vanta.launcher.data.local.ContactsRepositoryImpl
import app.vanta.launcher.data.repo.AppRepository
import app.vanta.launcher.data.repo.BatteryRepository
import app.vanta.launcher.data.repo.ContactsRepository
import app.vanta.launcher.data.repo.MediaRepository
import app.vanta.launcher.data.repo.SettingsRepository
import app.vanta.launcher.data.repo.ThemeRepository
import app.vanta.launcher.data.repo.WeatherRepository
import app.vanta.launcher.data.remote.MediaRepositoryImpl
import app.vanta.launcher.data.local.SettingsRepositoryImpl
import app.vanta.launcher.data.local.AppRepositoryImpl
import app.vanta.launcher.data.remote.WeatherRepositoryImpl
import app.vanta.launcher.ui.theme.ThemeRepositoryImpl

class AppContainer(private val context: Context) {
    val themeRepository: ThemeRepository = ThemeRepositoryImpl()
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(context)
    val appRepository: AppRepository = AppRepositoryImpl(context)
    val weatherRepository: WeatherRepository = WeatherRepositoryImpl(context, settingsRepository)
    val contactsRepository: ContactsRepository = ContactsRepositoryImpl(context)
    val mediaRepository: MediaRepository = MediaRepositoryImpl(context)
    val batteryRepository: BatteryRepository = BatteryRepositoryImpl(context)

    fun scheduleWeatherRefresh() {
        app.vanta.launcher.data.remote.WeatherWorker.enqueue(context)
    }
}
