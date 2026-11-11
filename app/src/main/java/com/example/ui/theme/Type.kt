package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.min

const val GOOGLE_SANS_FLEX_ROUNDNESS = 100f
const val MIN_OPTICAL_SIZE = 6f
const val MAX_OPTICAL_SIZE = 144f

/**
 * Computes the optical size ('opsz') in [6..144] scaled by the widget's current dimensions
 * so letterforms automatically adjust aperture and stroke weight when the widget is resized.
 */
fun computeWidgetOpticalSize(
  baseSp: Float,
  widgetMinWidthDp: Int = 140,
  widgetMinHeightDp: Int = 140
): Float {
  val w = widgetMinWidthDp.coerceAtLeast(60).toFloat()
  val h = widgetMinHeightDp.coerceAtLeast(60).toFloat()
  val resizeScale = min(w / 140f, h / 140f).coerceIn(0.55f, 2.4f)
  return (baseSp * resizeScale).coerceIn(MIN_OPTICAL_SIZE, MAX_OPTICAL_SIZE)
}

/**
 * Formats the OpenType variation settings string for Android Canvas TextPaint:
 * Roundness ('ROND') = 100, Optical Size ('opsz') = [6..144], Weight ('wght') = [100..900].
 */
fun buildGoogleSansFlexVariationSettings(
  opticalSize: Float,
  weight: Int = 500,
  roundness: Float = GOOGLE_SANS_FLEX_ROUNDNESS
): String {
  val opsz = opticalSize.coerceIn(MIN_OPTICAL_SIZE, MAX_OPTICAL_SIZE)
  val wght = weight.coerceIn(100, 900)
  val rond = roundness.coerceIn(0f, 100f)
  return "'ROND' $rond, 'opsz' $opsz, 'wght' $wght"
}

/**
 * Creates a Jetpack Compose FontFamily for Google Sans Flex with Roundness 100 and the given
 * optical size ('opsz') and weight ('wght').
 */
@OptIn(ExperimentalTextApi::class)
fun googleSansFlexFontFamily(
  opticalSizeSp: Float,
  weight: Int = 500,
  roundness: Float = GOOGLE_SANS_FLEX_ROUNDNESS
): FontFamily {
  val opsz = opticalSizeSp.coerceIn(MIN_OPTICAL_SIZE, MAX_OPTICAL_SIZE)
  val wght = weight.coerceIn(100, 900)
  val rond = roundness.coerceIn(0f, 100f)
  return FontFamily(
    Font(
      resId = R.font.google_sans_flex,
      weight = FontWeight(wght),
      variationSettings = FontVariation.Settings(
        FontVariation.Setting("ROND", rond),
        FontVariation.Setting("opsz", opsz),
        FontVariation.weight(wght)
      )
    )
  )
}

val Typography = Typography(
  displayLarge = TextStyle(
    fontFamily = googleSansFlexFontFamily(opticalSizeSp = 57f, weight = 500),
    fontWeight = FontWeight.Medium,
    fontSize = 57.sp,
    lineHeight = 64.sp
  ),
  headlineLarge = TextStyle(
    fontFamily = googleSansFlexFontFamily(opticalSizeSp = 32f, weight = 600),
    fontWeight = FontWeight.SemiBold,
    fontSize = 32.sp,
    lineHeight = 40.sp
  ),
  titleLarge = TextStyle(
    fontFamily = googleSansFlexFontFamily(opticalSizeSp = 22f, weight = 600),
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = googleSansFlexFontFamily(opticalSizeSp = 16f, weight = 400),
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.2.sp
  ),
  labelLarge = TextStyle(
    fontFamily = googleSansFlexFontFamily(opticalSizeSp = 14f, weight = 500),
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp
  )
)
