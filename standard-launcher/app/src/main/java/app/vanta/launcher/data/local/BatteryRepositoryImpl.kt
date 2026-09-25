package app.vanta.launcher.data.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import app.vanta.launcher.data.repo.BatteryRepository
import app.vanta.launcher.domain.model.BatteryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/** Event-driven battery state from the sticky ACTION_BATTERY_CHANGED broadcast; no polling. */
class BatteryRepositoryImpl(private val context: Context) : BatteryRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val battery: StateFlow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let { trySend(it.toBatteryState()) }
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { trySend(it.toBatteryState()) }
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), readOnce())

    private fun readOnce(): BatteryState =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.toBatteryState()
            ?: BatteryState(0, false)

    private fun Intent.toBatteryState(): BatteryState {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else 0
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryState(percent, charging)
    }
}
