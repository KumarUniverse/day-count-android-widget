package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class CountdownMode {
  TARGET_DATE,
  DAY_COUNT
}

enum class DateDisplayMode(val displayName: String) {
  ENDING_DATE("Display ending date"),
  DAYS_FRACTION("Display fraction of days completed")
}

enum class WidgetShapeStyle(val displayName: String) {
  SQUIRCLE("Squircle"),
  ROUNDED("Rounded"),
  PILL("Capsule"),
  CIRCLE("Circle"),
  HEXAGON("Hexagon"),
  FLOWER("Flower")
}

enum class WidgetThemeColor(
  val id: String,
  val displayName: String,
  val primaryHex: Long,
  val surfaceHex: Long,
  val containerHex: Long,
  val onSurfaceHex: Long
) {
  DYNAMIC("DYNAMIC", "Device Dynamic (MUI)", 0xFF38BDF8, 0xFF0F172A, 0xFF1E3A5F, 0xFFF0F9FF),
  WHITE("WHITE", "Pure White", 0xFF0F172A, 0xFFFFFFFF, 0xFFF1F5F9, 0xFF0F172A),
  CHARCOAL("CHARCOAL", "Grayish-Black", 0xFFE2E8F0, 0xFF18181B, 0xFF27272A, 0xFFFAFAFA),
  OCEAN("OCEAN", "Ocean Azure", 0xFF38BDF8, 0xFF0C4A6E, 0xFF0284C7, 0xFFF0F9FF),
  CORAL("CORAL", "Coral Sunset", 0xFFFB923C, 0xFF7C2D12, 0xFFEA580C, 0xFFFFF7ED),
  EMERALD("EMERALD", "Emerald Mint", 0xFF34D399, 0xFF064E3B, 0xFF059669, 0xFFECFDF5),
  VIOLET("VIOLET", "Cosmic Violet", 0xFFA78BFA, 0xFF4C1D95, 0xFF7C3AED, 0xFFF5F3FF),
  AMBER("AMBER", "Golden Glow", 0xFFFBBF24, 0xFF78350F, 0xFFD97706, 0xFFFEF3C7),
  ROSE("ROSE", "Rose Quartz", 0xFFFB7185, 0xFF881337, 0xFFE11D48, 0xFFFFF1F2),
  SLATE("SLATE", "Midnight Minimal", 0xFF94A3B8, 0xFF0F172A, 0xFF334155, 0xFFF8FAFC);

  companion object {
    fun fromId(id: String): WidgetThemeColor {
      return entries.find { it.id.equals(id, ignoreCase = true) } ?: DYNAMIC
    }
  }
}

object CountdownDateUtils {
  const val MIN_SPECIFIED_DAYS = 1
  const val MAX_SPECIFIED_DAYS = 36500

  /**
   * Sanitizes raw numeric keypad input for Specified Days:
   * - Strips any non-digit characters (letters, spaces, negative signs, decimals)
   * - Removes leading zeros ("0045" -> "45")
   * - Caps value at MAX_SPECIFIED_DAYS (36,500 days / 100 years)
   */
  fun sanitizeDaysInput(rawInput: String): String {
    val digitsOnly = rawInput.substringBefore('.').filter { it.isDigit() }.trimStart('0')
    if (digitsOnly.isEmpty()) return ""
    val cappedLength = digitsOnly.take(5)
    val parsed = cappedLength.toIntOrNull() ?: return ""
    return if (parsed > MAX_SPECIFIED_DAYS) {
      MAX_SPECIFIED_DAYS.toString()
    } else {
      parsed.toString()
    }
  }

  /**
   * Parses and clamps a raw day-count string into a valid [MIN_SPECIFIED_DAYS..MAX_SPECIFIED_DAYS] integer.
   */
  fun parseSanitizedDays(rawInput: String, fallback: Int = MIN_SPECIFIED_DAYS): Int {
    val sanitized = sanitizeDaysInput(rawInput)
    val value = sanitized.toIntOrNull() ?: fallback
    return value.coerceIn(MIN_SPECIFIED_DAYS, MAX_SPECIFIED_DAYS)
  }

  /**
   * Converts a UTC midnight timestamp returned by Material 3 DatePicker into a local timestamp
   * representing the exact same calendar date (YEAR, MONTH, DAY_OF_MONTH) at 12:00:00 local time.
   * This prevents negative UTC offsets (e.g. PST/EST) from shifting the selected date back by 1 day.
   */
  fun utcPickerMillisToLocalMillis(
    utcMillis: Long,
    localTimeZone: TimeZone = TimeZone.getDefault()
  ): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = utcMillis
    }
    val localCal = Calendar.getInstance(localTimeZone).apply {
      clear()
      set(
        utcCal.get(Calendar.YEAR),
        utcCal.get(Calendar.MONTH),
        utcCal.get(Calendar.DAY_OF_MONTH),
        12,
        0,
        0
      )
      set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
  }

  /**
   * Converts a local timestamp into the corresponding 00:00:00 UTC timestamp expected by
   * Material 3 rememberDatePickerState so the picker opens on the exact same local calendar date.
   */
  fun localMillisToUtcPickerMillis(
    localMillis: Long,
    localTimeZone: TimeZone = TimeZone.getDefault()
  ): Long {
    val localCal = Calendar.getInstance(localTimeZone).apply {
      timeInMillis = localMillis
    }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(
        localCal.get(Calendar.YEAR),
        localCal.get(Calendar.MONTH),
        localCal.get(Calendar.DAY_OF_MONTH),
        0,
        0,
        0
      )
      set(Calendar.MILLISECOND, 0)
    }
    return utcCal.timeInMillis
  }

  /**
   * Returns a local timestamp at 12:00:00 noon shifted by [dayOffset] calendar days from [baseLocalMillis].
   */
  fun addLocalCalendarDays(
    baseLocalMillis: Long,
    dayOffset: Int,
    localTimeZone: TimeZone = TimeZone.getDefault()
  ): Long {
    val localCal = Calendar.getInstance(localTimeZone).apply {
      timeInMillis = baseLocalMillis
      set(Calendar.HOUR_OF_DAY, 12)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.DAY_OF_YEAR, dayOffset)
    }
    return localCal.timeInMillis
  }

  /**
   * Converts a local timestamp to its integer calendar day index (days since 1970-01-01)
   * based on its local (YEAR, MONTH, DAY_OF_MONTH), completely immune to time-of-day or DST shifts.
   */
  fun toLocalDayOrdinal(
    epochMs: Long,
    localTimeZone: TimeZone = TimeZone.getDefault()
  ): Long {
    val utcEquivalent = localMillisToUtcPickerMillis(epochMs, localTimeZone)
    return TimeUnit.MILLISECONDS.toDays(utcEquivalent)
  }

  /**
   * Computes the exact number of calendar days from [fromEpochMs] to [toEpochMs] in local time.
   */
  fun calendarDaysBetween(
    fromEpochMs: Long,
    toEpochMs: Long,
    localTimeZone: TimeZone = TimeZone.getDefault()
  ): Long {
    return toLocalDayOrdinal(toEpochMs, localTimeZone) - toLocalDayOrdinal(fromEpochMs, localTimeZone)
  }

  /**
   * Computes the timestamp immediately after the next local midnight (00:00:01.000 of the next
   * calendar day) so 24-hour widget alarms fire right as the new day begins.
   */
  fun nextMidnightEpochMs(
    nowMs: Long = System.currentTimeMillis(),
    localTimeZone: TimeZone = TimeZone.getDefault()
  ): Long {
    val cal = Calendar.getInstance(localTimeZone).apply {
      timeInMillis = nowMs
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 1)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }
}

@Entity(tableName = "countdown_timers")
data class CountdownTimer(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val subtitle: String = "",
  val mode: String = CountdownMode.TARGET_DATE.name,
  val targetDateEpochMs: Long = 0L,
  val targetDaysTotal: Int = 30,
  val startDateEpochMs: Long = System.currentTimeMillis(),
  val colorSchemeId: String = WidgetThemeColor.DYNAMIC.id,
  val shapeStyle: String = WidgetShapeStyle.SQUIRCLE.name,
  val showTargetDate: Boolean = true,
  val dateDisplayMode: String = DateDisplayMode.ENDING_DATE.name,
  val opacityPercent: Int = 100,
  val createdAt: Long = System.currentTimeMillis()
) {

  fun getResolvedMode(): CountdownMode {
    return try {
      CountdownMode.valueOf(mode)
    } catch (_: Exception) {
      CountdownMode.TARGET_DATE
    }
  }

  fun getResolvedDateDisplayMode(): DateDisplayMode {
    return try {
      DateDisplayMode.valueOf(dateDisplayMode)
    } catch (_: Exception) {
      DateDisplayMode.ENDING_DATE
    }
  }

  fun getResolvedShape(): WidgetShapeStyle {
    return try {
      WidgetShapeStyle.valueOf(shapeStyle)
    } catch (_: Exception) {
      WidgetShapeStyle.SQUIRCLE
    }
  }

  fun getResolvedTheme(): WidgetThemeColor {
    return WidgetThemeColor.fromId(colorSchemeId)
  }

  fun effectiveTargetEpochMs(): Long {
    return if (getResolvedMode() == CountdownMode.TARGET_DATE) {
      targetDateEpochMs
    } else {
      CountdownDateUtils.addLocalCalendarDays(startDateEpochMs, targetDaysTotal)
    }
  }

  fun daysRemaining(nowMs: Long = System.currentTimeMillis()): Long {
    val target = effectiveTargetEpochMs()
    return CountdownDateUtils.calendarDaysBetween(nowMs, target)
  }

  fun daysPassed(nowMs: Long = System.currentTimeMillis()): Long {
    val diffDays = CountdownDateUtils.calendarDaysBetween(startDateEpochMs, nowMs)
    return max(0L, diffDays)
  }

  fun totalDaysSpan(): Long {
    return if (getResolvedMode() == CountdownMode.DAY_COUNT) {
      max(1L, targetDaysTotal.toLong())
    } else {
      val diffDays = CountdownDateUtils.calendarDaysBetween(startDateEpochMs, effectiveTargetEpochMs())
      max(1L, diffDays)
    }
  }

  fun daysCompleted(nowMs: Long = System.currentTimeMillis()): Long {
    return daysPassed(nowMs).coerceIn(0L, totalDaysSpan())
  }

  fun isCompleted(nowMs: Long = System.currentTimeMillis()): Boolean {
    return daysRemaining(nowMs) <= 0L
  }

  fun progressFraction(nowMs: Long = System.currentTimeMillis()): Float {
    val totalDays = totalDaysSpan()
    if (totalDays <= 0L) return 1.0f
    val elapsedDays = CountdownDateUtils.calendarDaysBetween(startDateEpochMs, nowMs)
    return (elapsedDays.toFloat() / totalDays.toFloat()).coerceIn(0.0f, 1.0f)
  }

  fun formattedTargetDate(): String {
    val date = Date(effectiveTargetEpochMs())
    val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    return formatter.format(date)
  }

  fun formattedCountdownFooter(
    nowMs: Long = System.currentTimeMillis(),
    compact: Boolean = false
  ): String {
    if (!showTargetDate) return ""
    return when (getResolvedDateDisplayMode()) {
      DateDisplayMode.ENDING_DATE -> formattedTargetDate()
      DateDisplayMode.DAYS_FRACTION -> {
        val completed = daysCompleted(nowMs)
        val total = totalDaysSpan()
        if (compact) {
          "$completed / $total days"
        } else {
          "$completed / $total days completed"
        }
      }
    }
  }
}
