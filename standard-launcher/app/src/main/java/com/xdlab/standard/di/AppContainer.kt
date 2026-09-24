package com.xdlab.standard.di

import android.content.Context
import com.xdlab.standard.data.local.BatteryRepositoryImpl
import com.xdlab.standard.data.local.ContactsRepositoryImpl
import com.xdlab.standard.data.repo.AppRepository
import com.xdlab.standard.data.repo.BatteryRepository
import com.xdlab.standard.data.repo.ContactsRepository
import com.xdlab.standard.data.repo.MediaRepository
import com.xdlab.standard.data.repo.SettingsRepository
import com.xdlab.standard.data.repo.ThemeRepository
import com.xdlab.standard.data.repo.WeatherRepository
import com.xdlab.standard.data.remote.MediaRepositoryImpl
import com.xdlab.standard.data.local.SettingsRepositoryImpl
import com.xdlab.standard.data.local.AppRepositoryImpl
import com.xdlab.standard.data.remote.WeatherRepositoryImpl
import com.xdlab.standard.ui.theme.ThemeRepositoryImpl

class AppContainer(private val context: Context) {
    val themeRepository: ThemeRepository = ThemeRepositoryImpl()
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(context)
    val appRepository: AppRepository = AppRepositoryImpl(context)
    val weatherRepository: WeatherRepository = WeatherRepositoryImpl(context, settingsRepository)
    val contactsRepository: ContactsRepository = ContactsRepositoryImpl(context)
    val mediaRepository: MediaRepository = MediaRepositoryImpl(context)
    val batteryRepository: BatteryRepository = BatteryRepositoryImpl(context)

    fun scheduleWeatherRefresh() {
        com.xdlab.standard.data.remote.WeatherWorker.enqueue(context)
    }
}
