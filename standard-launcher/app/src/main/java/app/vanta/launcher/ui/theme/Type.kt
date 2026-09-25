package app.vanta.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.vanta.launcher.R

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, declared: FontWeight, wght: Int) = Font(
    resId = resId,
    weight = declared,
    variationSettings = FontVariation.Settings(FontVariation.weight(wght))
)

/** Space Grotesk with true variable-font weights (axis range 300-700). */
val SpaceGrotesk = FontFamily(
    variableFont(R.font.space_grotesk, FontWeight.Medium, 500),
    variableFont(R.font.space_grotesk, FontWeight.Bold, 700),
    variableFont(R.font.space_grotesk, FontWeight.Black, 700)
)

/**
 * Display face for the giant headlines. The 700 instance is declared as Normal so a
 * Black request makes Android add synthetic bold on top of it: the heaviest look the
 * shipped font can give, closest to the mockups' condensed-black headlines.
 */
val SpaceGroteskDisplay = FontFamily(
    variableFont(R.font.space_grotesk, FontWeight.Normal, 700)
)

/** JetBrains Mono with true variable-font weights (axis range 100-800). */
val JetBrainsMono = FontFamily(
    variableFont(R.font.jetbrains_mono, FontWeight.Normal, 400),
    variableFont(R.font.jetbrains_mono, FontWeight.Medium, 500),
    variableFont(R.font.jetbrains_mono, FontWeight.Bold, 700),
    variableFont(R.font.jetbrains_mono, FontWeight.ExtraBold, 800)
)

/** The launcher's type scale. Sizes are chosen per call site; tracking and leading are fixed here. */
object StandardType {
    /** Giant headlines: day name, GOOD APPS BETTER DAYS., tile titles. */
    fun display(size: TextUnit): TextStyle = TextStyle(
        fontFamily = SpaceGroteskDisplay,
        fontWeight = FontWeight.Black,
        fontSize = size,
        lineHeight = size * 0.88f,
        letterSpacing = size * -0.035f
    )

    /** Headlines that should stay true-bold rather than synthesized. */
    fun headline(size: TextUnit): TextStyle = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Black,
        fontSize = size,
        lineHeight = size * 0.98f,
        letterSpacing = size * -0.02f
    )

    /** Mono meta labels: captions, numbers, section labels. Always uppercase at the call site. */
    fun mono(size: TextUnit = 11.sp, weight: FontWeight = FontWeight.Medium): TextStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = weight,
        fontSize = size,
        lineHeight = size * 1.45f,
        letterSpacing = size * 0.08f
    )
}

val StandardTypography = Typography(
    displayLarge = StandardType.display(57.sp),
    headlineLarge = StandardType.headline(32.sp),
    titleLarge = StandardType.headline(22.sp),
    labelSmall = StandardType.mono(11.sp),
    bodyMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp
    )
)
