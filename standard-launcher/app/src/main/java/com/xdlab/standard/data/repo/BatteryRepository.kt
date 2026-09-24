package com.xdlab.standard.data.repo

import com.xdlab.standard.domain.model.BatteryState
import kotlinx.coroutines.flow.StateFlow

interface BatteryRepository {
    val battery: StateFlow<BatteryState>
}
