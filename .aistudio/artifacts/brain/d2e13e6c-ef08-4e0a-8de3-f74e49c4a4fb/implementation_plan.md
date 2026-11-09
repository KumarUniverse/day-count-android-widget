# DayCount — Automatic Midnight (24-Hour) Widget & Progress Rollover

This update ensures that as soon as a new calendar day begins immediately after local midnight (`00:00:00`), all countdown widgets on the Android home screen and in the app automatically recalculate and redraw—decreasing the days remaining by 1 and advancing the progress bar and completed-days fraction every 24 hours.

### User Review & Critical Decisions

> [!IMPORTANT]
> Please review the plan below and click **Proceed** to implement the midnight rollover update.

- **Root Cause Identified**:
  1. **No Midnight Scheduler**: Home screen widgets previously relied only on Android's unaligned `updatePeriodMillis` polling rather than a midnight-anchored alarm, so widgets continued showing yesterday's static rendered bitmap after `00:00:00`.
  2. **Missing Date/Time Rollover Broadcasts**: The widget receiver only listened for `APPWIDGET_UPDATE` and ignored date rollover (`ACTION_DATE_CHANGED`), clock/date adjustments (`ACTION_TIME_SET`), timezone changes (`ACTION_TIMEZONE_CHANGED`), and device reboots (`ACTION_BOOT_COMPLETED`).
  3. **Async Broadcast Lifecycle & Final-Day Noon Offset**: Widget bitmap rendering ran inside an unstructured background coroutine without `goAsync()`, allowing the OS to finish the broadcast before updating `AppWidgetManager`. Additionally, `isCompleted` checked `nowMs >= effectiveTargetEpochMs()` (which was anchored to `12:00:00` noon), delaying target-day completion status between midnight and noon.

---

### 1. Overview & Core Concept

- **What It Does**:
  - Schedules an automatic midnight alarm (`00:00:01` local time) that wakes the widget updater immediately after midnight every 24 hours, refreshes every active home screen widget, and chains the next day's midnight alarm.
  - Responds immediately to system date changes, manual clock adjustments, timezone shifts, and device reboots.
  - Synchronizes the in-app countdown list and live preview across midnight boundaries and app resume events.
- **Key Value**: Guaranteed, punctual day-by-day countdown decrements and progress bar updates immediately after midnight across all home screen widgets and in-app views.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  - **Midnight Rollover (`00:00:00` → `00:00:01`)**: Immediately after midnight, every home screen widget redraws its canvas bitmap with the new calendar day:
    - **Days Remaining** decreases by `1`.
    - **Progress Bar** advances proportionally (`elapsedDays / totalDaysSpan`).
    - **Footer Fraction** (when enabled) increments (`completed / total days completed`).
  - **System Date / Timezone Change**: If the user changes the date or time in Android Settings, all home screen widgets and the in-app UI update immediately.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Midnight-Anchored `AlarmManager` Chain + System Broadcasts**
  - *Chosen Approach*: Compute the exact timestamp for the next local midnight (`00:00:00.500` / `00:00:01` of the next calendar day) and schedule a wakeup alarm via `AlarmManager` (`setWindow` / `setExactAndAllowWhileIdle` when permitted, with `setAndAllowWhileIdle` fallback), automatically rescheduling the next 24-hour midnight alarm each time it fires. Combine this with manifest and runtime listeners for `DATE_CHANGED`, `TIME_SET`, `TIMEZONE_CHANGED`, `BOOT_COMPLETED`, and `MY_PACKAGE_REPLACED`.
  - *Why*: Standard `updatePeriodMillis` widget intervals are throttled by Android launchers and never align with `00:00:00`. A midnight-anchored alarm paired with system date/time broadcasts guarantees immediate updates right after midnight without draining battery during the day.
- **Decision 2: Reliable Broadcast Execution via `goAsync()`**
  - *Chosen Approach*: Hold a `PendingResult` via `goAsync()` inside the widget broadcast receiver while querying the local database and pushing updated bitmaps to `AppWidgetManager`.
  - *Why*: Prevents Android from terminating the background receiver process at midnight before all widget bitmaps finish rendering.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Midnight Update Flow**:
```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Midnight & System Time Triggers                      │
│  • AlarmManager RTC_WAKEUP at 00:00:01 Local Time (Every 24 Hours)      │
│  • System Intents: DATE_CHANGED, TIME_SET, TIMEZONE_CHANGED, BOOT       │
│  • Foreground Tick: ACTION_TIME_TICK + Activity onResume                │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                Widget Receiver & Scheduler (goAsync)                    │
│  1. Re-schedule next local midnight alarm (+24h at 00:00:01)            │
│  2. Query all active timers from local SQLite database                  │
│  3. Recompute calendar-day ordinal for new day:                         │
│     • daysRemaining = calendarDaysBetween(now, target)  (-1 day)        │
│     • progressFraction = elapsedDays / totalDaysSpan    (+1 day step)   │
│  4. Render updated Canvas Bitmaps & push to AppWidgetManager            │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Data & Calculation Refinements**:
  - **Next Midnight Calculation**: A helper computes the next local calendar day at `00:00:01.000` in the current device `TimeZone` (refreshing `TimeZone.getDefault()` on timezone shifts) so the alarm always fires immediately after midnight.
  - **Calendar-Day Completion Consistency**: Update `isCompleted(nowMs)` to rely purely on `daysRemaining(nowMs) <= 0L` so the target day transitions immediately at `00:00:00` midnight rather than waiting until `12:00:00` noon.
