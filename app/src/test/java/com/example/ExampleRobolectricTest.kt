package com.example

import android.content.Context
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.example.data.CountdownDateUtils
import com.example.data.CountdownMode
import com.example.data.CountdownTimer
import com.example.data.DateDisplayMode
import com.example.data.WidgetShapeStyle
import com.example.data.WidgetThemeColor
import com.example.ui.components.WallpaperTheme
import com.example.widget.DayCountWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DayCount", appName)
  }

  @Test
  fun `countdown timer calculates remaining days and custom progress bar for target date`() {
    val now = 1_000_000_000_000L
    val target = CountdownDateUtils.addLocalCalendarDays(now, 15)
    val optionalStart = CountdownDateUtils.addLocalCalendarDays(now, -15)

    val timer = CountdownTimer(
      title = "Vacation",
      mode = CountdownMode.TARGET_DATE.name,
      targetDateEpochMs = target,
      startDateEpochMs = optionalStart
    )

    assertEquals(15L, timer.daysRemaining(now))
    assertEquals(0.5f, timer.progressFraction(now), 0.01f)
    assertFalse(timer.isCompleted(now))
  }

  @Test
  fun `specified days numpad input sanitation strips non-digits leading zeros and clamps bounds`() {
    assertEquals("45", CountdownDateUtils.sanitizeDaysInput("0045"))
    assertEquals("120", CountdownDateUtils.sanitizeDaysInput("-120.5abc"))
    assertEquals("", CountdownDateUtils.sanitizeDaysInput("000"))
    assertEquals("36500", CountdownDateUtils.sanitizeDaysInput("99999"))

    assertEquals(1, CountdownDateUtils.parseSanitizedDays("", fallback = 1))
    assertEquals(30, CountdownDateUtils.parseSanitizedDays("000", fallback = 30))
    assertEquals(36500, CountdownDateUtils.parseSanitizedDays("99999", fallback = 30))
    assertEquals(75, CountdownDateUtils.parseSanitizedDays("075 days!", fallback = 30))
  }

  @Test
  fun `home screen widget uses compact vertical bounding box for horizontal shapes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val squircleTimer = CountdownTimer(
      title = "Vacation",
      shapeStyle = WidgetShapeStyle.SQUIRCLE.name
    )
    val circleTimer = CountdownTimer(
      title = "Sprint",
      shapeStyle = WidgetShapeStyle.CIRCLE.name
    )

    val squircleBmp = DayCountWidgetProvider.renderWidgetBitmap(context, squircleTimer)
    assertEquals(540, squircleBmp.width)
    assertEquals(340, squircleBmp.height)

    val circleBmp = DayCountWidgetProvider.renderWidgetBitmap(context, circleTimer)
    assertEquals(460, circleBmp.width)
    assertEquals(460, circleBmp.height)
  }

  @Test
  fun `home screen widget resolves device dynamic MUI color scheme`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("daycount_app_prefs", Context.MODE_PRIVATE)

    prefs.edit().putString("app_theme_mode", "LIGHT").commit()
    val lightDynamic = dynamicLightColorScheme(context)
    val resolvedLight = DayCountWidgetProvider.resolveCanvasWidgetColors(context, WidgetThemeColor.DYNAMIC)
    assertEquals(lightDynamic.primary.toArgb(), resolvedLight.primary)
    assertEquals(lightDynamic.primaryContainer.toArgb(), resolvedLight.container)

    prefs.edit().putString("app_theme_mode", "DARK").commit()
    val darkDynamic = dynamicDarkColorScheme(context)
    val resolvedDark = DayCountWidgetProvider.resolveCanvasWidgetColors(context, WidgetThemeColor.DYNAMIC)
    assertEquals(darkDynamic.primary.toArgb(), resolvedDark.primary)
    assertEquals(darkDynamic.primaryContainer.toArgb(), resolvedDark.container)
  }

  @Test
  fun `countdown timer calculates remaining days correctly for day count challenge`() {
    val now = 1_000_000_000_000L
    val start = CountdownDateUtils.addLocalCalendarDays(now, -20)

    val timer = CountdownTimer(
      title = "100-Day Challenge",
      mode = CountdownMode.DAY_COUNT.name,
      targetDaysTotal = 100,
      startDateEpochMs = start
    )

    // 100 days total, started 20 days ago -> 80 days left
    assertEquals(80L, timer.daysRemaining(now))
    assertEquals(20L, timer.daysPassed(now))
    assertFalse(timer.isCompleted(now))
  }

  @Test
  fun `date display modes format ending date and fraction of days completed for both countdown types`() {
    val now = 1_000_000_000_000L
    val start = CountdownDateUtils.addLocalCalendarDays(now, -10)
    val target = CountdownDateUtils.addLocalCalendarDays(now, 20)

    // 1. Target Date with DAYS_FRACTION
    val targetDateFractionTimer = CountdownTimer(
      title = "Conference",
      mode = CountdownMode.TARGET_DATE.name,
      targetDateEpochMs = target,
      startDateEpochMs = start,
      showTargetDate = true,
      dateDisplayMode = DateDisplayMode.DAYS_FRACTION.name
    )
    assertEquals("10 / 30 days completed", targetDateFractionTimer.formattedCountdownFooter(now, compact = false))
    assertEquals("10 / 30 days", targetDateFractionTimer.formattedCountdownFooter(now, compact = true))

    // 2. Specified Days with ENDING_DATE
    val dayCountEndingDateTimer = CountdownTimer(
      title = "30-Day Streak",
      mode = CountdownMode.DAY_COUNT.name,
      targetDaysTotal = 30,
      startDateEpochMs = start,
      showTargetDate = true,
      dateDisplayMode = DateDisplayMode.ENDING_DATE.name
    )
    assertEquals(dayCountEndingDateTimer.formattedTargetDate(), dayCountEndingDateTimer.formattedCountdownFooter(now, compact = false))

    // 3. Disabled showTargetDate returns empty string regardless of radio option
    val hiddenFooterTimer = targetDateFractionTimer.copy(showTargetDate = false)
    assertEquals("", hiddenFooterTimer.formattedCountdownFooter(now, compact = false))
  }

  @Test
  fun `selecting Nov 8th in UTC DatePicker preserves Nov 8th in US Pacific and Eastern timezones`() {
    // Material 3 DatePicker emits 00:00:00 UTC for Nov 8, 2026
    val utcNov8Cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(2026, Calendar.NOVEMBER, 8, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val utcNov8Millis = utcNov8Cal.timeInMillis

    val timezonesToVerify = listOf(
      TimeZone.getTimeZone("America/Los_Angeles"),
      TimeZone.getTimeZone("America/New_York"),
      TimeZone.getTimeZone("UTC"),
      TimeZone.getTimeZone("Asia/Tokyo")
    )

    for (tz in timezonesToVerify) {
      val localMillis = CountdownDateUtils.utcPickerMillisToLocalMillis(utcNov8Millis, tz)
      val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
        timeZone = tz
      }
      assertEquals(
        "Failed in timezone ${tz.id}",
        "Nov 8, 2026",
        formatter.format(Date(localMillis))
      )

      // Round-trip back to UTC DatePicker millis must still be Nov 8, 2026 00:00:00 UTC
      val roundTripUtc = CountdownDateUtils.localMillisToUtcPickerMillis(localMillis, tz)
      assertEquals(utcNov8Millis, roundTripUtc)
    }
  }

  @Test
  fun `wallpaper theme returns light gradient in light mode and dark gradient in dark mode`() {
    for (wp in WallpaperTheme.entries) {
      val lightColors = wp.gradientForTheme(isLightTheme = true)
      val darkColors = wp.gradientForTheme(isLightTheme = false)
      assertTrue(
        "Expected light wallpaper gradient for ${wp.name}",
        lightColors.first().luminance() > 0.7f
      )
      assertTrue(
        "Expected dark wallpaper gradient for ${wp.name}",
        darkColors.first().luminance() < 0.2f
      )
    }
  }
}
