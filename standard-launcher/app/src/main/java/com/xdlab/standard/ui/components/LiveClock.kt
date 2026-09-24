package com.xdlab.standard.ui.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.xdlab.standard.domain.model.ClockFormat
import com.xdlab.standard.ui.theme.LocalSettings
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/** Ticks on the minute unless the pattern shows seconds; only readers of the value recompose. */
@Composable
fun liveClock(): LocalDateTime = produceState(LocalDateTime.now()) {
    while (true) {
        val now = LocalDateTime.now()
        delay(60_000L - (now.second * 1000L + now.nano / 1_000_000L))
        value = LocalDateTime.now()
    }
}.value

@Composable
fun liveTimeFormatted(pattern: String): String {
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH) }
    return liveClock().format(formatter).uppercase()
}

/** HH:mm or h:mm a following the CLOCK setting (AUTO = the system 24-hour preference). */
@Composable
fun liveClockFormatted(): String {
    val context = LocalContext.current
    val format = LocalSettings.current.clockFormat
    val is24 = when (format) {
        ClockFormat.H24 -> true
        ClockFormat.H12 -> false
        ClockFormat.AUTO -> DateFormat.is24HourFormat(context)
    }
    return liveTimeFormatted(if (is24) "HH:mm" else "h:mm a")
}
