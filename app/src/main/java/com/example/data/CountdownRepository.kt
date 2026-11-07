package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class CountdownRepository(private val dao: CountdownDao) {

  val allTimers: Flow<List<CountdownTimer>> = dao.getAllTimers()

  suspend fun insert(timer: CountdownTimer): Long = dao.insertTimer(timer)

  suspend fun update(timer: CountdownTimer) = dao.updateTimer(timer)

  suspend fun delete(timer: CountdownTimer) = dao.deleteTimer(timer)

  suspend fun seedSampleDataIfEmpty() {
    if (dao.getCount() == 0) {
      val now = System.currentTimeMillis()

      val samples = listOf(
        CountdownTimer(
          title = "Summer Beach Vacation",
          subtitle = "",
          mode = CountdownMode.TARGET_DATE.name,
          targetDateEpochMs = now + TimeUnit.DAYS.toMillis(42) + TimeUnit.HOURS.toMillis(6),
          startDateEpochMs = now - TimeUnit.DAYS.toMillis(18),
          colorSchemeId = WidgetThemeColor.DYNAMIC.id,
          shapeStyle = WidgetShapeStyle.SQUIRCLE.name,
          showTargetDate = true,
          dateDisplayMode = DateDisplayMode.ENDING_DATE.name
        ),
        CountdownTimer(
          title = "100 Days of Code",
          subtitle = "",
          mode = CountdownMode.DAY_COUNT.name,
          targetDaysTotal = 100,
          startDateEpochMs = now - TimeUnit.DAYS.toMillis(34),
          colorSchemeId = WidgetThemeColor.VIOLET.id,
          shapeStyle = WidgetShapeStyle.HEXAGON.name,
          showTargetDate = true,
          dateDisplayMode = DateDisplayMode.DAYS_FRACTION.name
        ),
        CountdownTimer(
          title = "Major Product Launch",
          subtitle = "",
          mode = CountdownMode.TARGET_DATE.name,
          targetDateEpochMs = now + TimeUnit.DAYS.toMillis(14) + TimeUnit.HOURS.toMillis(18),
          startDateEpochMs = now - TimeUnit.DAYS.toMillis(46),
          colorSchemeId = WidgetThemeColor.CORAL.id,
          shapeStyle = WidgetShapeStyle.FLOWER.name,
          showTargetDate = true,
          dateDisplayMode = DateDisplayMode.ENDING_DATE.name
        ),
        CountdownTimer(
          title = "30-Day Fitness Sprint",
          subtitle = "",
          mode = CountdownMode.DAY_COUNT.name,
          targetDaysTotal = 30,
          startDateEpochMs = now - TimeUnit.DAYS.toMillis(11),
          colorSchemeId = WidgetThemeColor.EMERALD.id,
          shapeStyle = WidgetShapeStyle.CIRCLE.name,
          showTargetDate = false,
          dateDisplayMode = DateDisplayMode.DAYS_FRACTION.name
        ),
        CountdownTimer(
          title = "New Year 2027 Countdown",
          subtitle = "",
          mode = CountdownMode.TARGET_DATE.name,
          targetDateEpochMs = now + TimeUnit.DAYS.toMillis(54),
          startDateEpochMs = now - TimeUnit.DAYS.toMillis(311),
          colorSchemeId = WidgetThemeColor.AMBER.id,
          shapeStyle = WidgetShapeStyle.PILL.name,
          showTargetDate = true,
          dateDisplayMode = DateDisplayMode.ENDING_DATE.name
        )
      )

      samples.forEach { dao.insertTimer(it) }
    }
  }
}
