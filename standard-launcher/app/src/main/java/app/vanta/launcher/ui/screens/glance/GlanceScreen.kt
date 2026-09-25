package app.vanta.launcher.ui.screens.glance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vanta.launcher.ui.components.DriftingNoiseOverlay
import app.vanta.launcher.ui.theme.JetBrainsMono
import app.vanta.launcher.ui.theme.LocalAppTheme
import app.vanta.launcher.ui.theme.SpaceGrotesk
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun GlanceScreen(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val fade by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "glanceFadeIn"
    )

    var timeStr by remember {
        mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
    }
    LaunchedEffect(Unit) {
        while (true) {
            timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            delay(1000)
        }
    }

    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE dd MMM")).uppercase()
    val accent = LocalAppTheme.current.accent

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(fade)
            .clickable { onDismiss() }
    ) {
        DriftingNoiseOverlay(modifier = Modifier.fillMaxSize(), alpha = 0.05f, tint = Color.White)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeStr,
                color = Color.White,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Black,
                fontSize = 72.sp
            )
            Text(
                text = dateStr,
                color = Color.White.copy(alpha = 0.6f),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "STANDARD.",
                color = accent,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 32.dp)
            )
            Text(
                text = "TAP TO DISMISS",
                color = Color.White.copy(alpha = 0.3f),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 48.dp)
            )
        }
    }
}

@Composable
fun GlanceOverlay(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        GlanceScreen(onDismiss = onDismiss)
    }
}
