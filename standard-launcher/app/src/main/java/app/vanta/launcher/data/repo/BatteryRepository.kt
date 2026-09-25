package app.vanta.launcher.data.repo

import app.vanta.launcher.domain.model.BatteryState
import kotlinx.coroutines.flow.StateFlow

interface BatteryRepository {
    val battery: StateFlow<BatteryState>
}
