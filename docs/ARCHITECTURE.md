# DayCount — Comprehensive Codebase & Architecture Guide

**DayCount** (`com.aistudio.daycount.kdypzq`) is a Material Design 3 Android application and Home Screen Widget suite built with **Kotlin**, **Jetpack Compose**, **Room SQLite**, and a custom **Canvas + RemoteViews** widget rendering engine designed to match first-party Google Pixel widgets.

---

## Table of Contents

1. [High-Level System Architecture](#1-high-level-system-architecture)
2. [Directory & Package Breakdown](#2-directory--package-breakdown)
3. [Data Layer & Timezone-Safe Calendar Math](#3-data-layer--timezone-safe-calendar-math)
4. [Widget Customization Options (Shapes & Color Themes)](#4-widget-customization-options-shapes--color-themes)
5. [Home Screen Widget Engine (`DayCountWidgetProvider`)](#5-home-screen-widget-engine-daycountwidgetprovider)
6. [Variable Typography (`Google Sans Flex`)](#6-variable-typography-google-sans-flex)
7. [24-Hour Midnight Rollover & Sync Lifecycle](#7-24-hour-midnight-rollover--sync-lifecycle)
8. [In-App Jetpack Compose UI Layer](#8-in-app-jetpack-compose-ui-layer)
9. [Security, Privacy & Backup Architecture](#9-security-privacy--backup-architecture)
10. [Testing Strategy (`ExampleRobolectricTest`)](#10-testing-strategy-examplerobolectrictest)
11. [Important Notes & Platform Nuances](#11-important-notes--platform-nuances)

---

## 1. High-Level System Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              Persistence Layer                               │
│                                                                              │
│  ┌────────────────────────────────────┐  ┌────────────────────────────────┐  │
│  │ Room Database (CountdownDatabase)  │  │ SharedPreferences              │  │
│  │ • countdown_timers table           │  │ • daycount_app_prefs.xml       │  │
│  │ • CountdownDao / Repository        │  │ • daycount_widget_prefs.xml    │  │
│  └─────────────────┬──────────────────┘  └───────────────┬────────────────┘  │
└────────────────────┼─────────────────────────────────────┼───────────────────┘
                     │                                     │
         ┌───────────┴───────────┐             ┌───────────┴───────────┐
         ▼                       ▼             ▼                       ▼
┌─────────────────────────────────────┐ ┌──────────────────────────────────────┐
│       In-App UI (Jetpack Compose)   │ │    Android Home Screen Widget Host   │
│                                     │ │                                      │
│ • MainActivity                      │ │ • DayCountWidgetProvider             │
│ • CountdownViewModel (1s ticker)    │ │ • MidnightWidgetAlarmReceiver        │
│ • CountdownAppScreen                │ │ • WidgetUpdateHelper                 │
│ • WidgetPreviewShowcase (Studio)    │ │ • Layered RemoteViews:               │
│ • MaterialWidgetCard                │ │   - @id/widget_bg_image (Dynamic     │
│ • CreateEditTimerDialog             │ │     @color/widget_dynamic_bg mask)   │
│ • HomeScreenWidgetGuideDialog       │ │   - @id/widget_canvas_image          │
└─────────────────────────────────────┘ └──────────────────────────────────────┘
```

### How It All Fits Together
1. **Single Source of Truth**: Every countdown timer is stored as a `CountdownTimer` entity in the local Room SQLite database (`countdown_timer_database`).
2. **Real-Time In-App State**: `CountdownViewModel` observes `CountdownRepository.allTimers` as a Kotlin `Flow`, combines it with the active category filter (`ALL`, `TARGET_DATE`, `DAY_COUNT`) and search query, and runs a 1-second coroutine ticker so midnight day rollovers update in real time while the app is open.
3. **Home Screen Widget Synchronization**: Whenever a user creates, edits, quick-styles, or deletes a countdown—or when the clock crosses midnight—`WidgetUpdateHelper.notifyWidgetDataChanged()` triggers `DayCountWidgetProvider` to re-query Room on `Dispatchers.IO`, render a crisp anti-aliased `Bitmap` for each placed widget (`appWidgetId`), and push the updated `RemoteViews` to `AppWidgetManager`.

---

## 2. Directory & Package Breakdown

### `com.example`
- **`MainActivity.kt`**: Single-activity entry point using `enableEdgeToEdge()`. Observes `CountdownViewModel.appThemeMode` (`SYSTEM`, `LIGHT`, `DARK`) and registers a runtime `RECEIVER_NOT_EXPORTED` `BroadcastReceiver` for `ACTION_TIME_TICK`, `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, and `ACTION_TIMEZONE_CHANGED`.

### `com.example.data`
- **`CountdownTimer.kt`**:
  - Defines the `@Entity(tableName = "countdown_timers")` data class and all domain calculation methods (`daysRemaining`, `daysPassed`, `totalDaysSpan`, `progressFraction`, `formattedCountdownFooter`).
  - Defines enums: `CountdownMode`, `DateDisplayMode`, `WidgetShapeStyle`, and `WidgetThemeColor`.
  - Defines `CountdownDateUtils`: timezone-safe date conversion, day-ordinal math, midnight timestamp calculation, and input sanitization (`sanitizeTitleInput`, `sanitizeSubtitleInput`, `sanitizeSearchQuery`, `sanitizeDaysInput`).
- **`CountdownDao.kt`**: Room DAO providing reactive `Flow<List<CountdownTimer>>` queries for the UI and direct `suspend` queries (`getTimerByIdDirect`, `getFirstTimerDirect`) for the background widget updater.
- **`CountdownDatabase.kt`**: Singleton Room database configuration (`version = 5`).
- **`CountdownRepository.kt`**: Mediates DAO access and seeds 5 sample countdown timers on first launch (`seedSampleDataIfEmpty()`).

### `com.example.widget`
- **`DayCountWidgetProvider.kt`**: `AppWidgetProvider` implementation and custom `Canvas` renderer (`renderWidgetBitmap`, `renderWidgetBackgroundMaskBitmap`, `buildShapePath`, `resolveCanvasWidgetColors`, `fitTitleTextPaint`, `scheduleNextMidnightUpdate`).
- **`MidnightWidgetAlarmReceiver`** (in `DayCountWidgetProvider.kt`): Non-exported (`android:exported="false"`) `BroadcastReceiver` dedicated to handling the scheduled `00:00:01` `AlarmManager` broadcast.
- **`WidgetUpdateHelper.kt`**: Manages `appWidgetId` $\rightarrow$ `timerId` mappings in `daycount_widget_prefs`, handles Android O+ `requestPinAppWidget` ("Add to Home Screen"), and triggers widget broadcasts.

### `com.example.ui` & `com.example.ui.components`
- **`CountdownViewModel.kt`**: Manages UI state, theme preferences (`daycount_app_prefs`), search/filter flows, dialog state (`Create`, `Edit`, `HomeGuide`), and the 1-second day-rollover monitor.
- **`CountdownAppScreen.kt`**: Main Material 3 `Scaffold` with top app bar, collapsible search bar, theme mode dropdown, **Interactive Widget Studio** (`WidgetPreviewShowcase`), filter chips, countdown card list, and delete confirmation dialog.
- **`WidgetPreviewShowcase.kt`**: Live wallpaper preview canvas allowing users to preview their selected countdown against simulated Light/Dark wallpapers (`Aurora`, `Ocean`, `Sunset`, `Minimal`), toggle preview aspect ratios (`2×2 Square`, `4×2 Wide`, `3×3 Large`), and one-tap switch shapes, color themes, and opacity.
- **`MaterialWidgetCard.kt`**: Renders each countdown in the main list using the exact same geometric `Shape` silhouette and color palette as the home screen widget, with external action buttons (`Pin to Home`, `Edit`, `Delete`).
- **`CreateEditTimerDialog.kt`**: Full-featured modal dialog for creating and editing countdowns, supporting both **Until Specific Date** (with optional starting date to customize progress bar percentage) and **Specified Days** (with tap-to-edit numeric keypad input from `1` to `36,500` days).
- **`HomeScreenWidgetGuideDialog.kt`**: Step-by-step interactive guide explaining how to pin or drag DayCount widgets onto the Android home screen.

### `com.example.ui.theme`
- **`Type.kt`**: Bundles the variable font `res/font/google_sans_flex.ttf` (`Google Sans Flex`) and provides helper functions (`computeWidgetOpticalSize`, `buildGoogleSansFlexVariationSettings`) for configuring `'ROND'` (Roundness), `'opsz'` (Optical Size), and `'wght'` (Weight) axes.
- **`Color.kt` & `Theme.kt`**: Material 3 dynamic and fallback color schemes for the application UI.

---

## 3. Data Layer & Timezone-Safe Calendar Math

### Two Countdown Modes (`CountdownMode`)
1. **`TARGET_DATE` ("Until Specific Date")**:
   - Counts down toward a specific calendar date (`targetDateEpochMs`).
   - Supports an **optional Starting Date** (`startDateEpochMs`, defaulting to the creation date) so users can control where the progress bar starts.
2. **`DAY_COUNT` ("Specified Days")**:
   - Counts down a fixed number of days (`targetDaysTotal`, from `1` to `36,500` days) starting from a user-selected `startDateEpochMs`.
   - Computes its effective target date via `CountdownDateUtils.addLocalCalendarDays(startDateEpochMs, targetDaysTotal)`.

### Two Footer Display Modes (`DateDisplayMode`)
When **"Display countdown date on widget"** (`showTargetDate`) is enabled, users can choose between two radio options for **either** countdown mode:
- **`ENDING_DATE`**: Displays the formatted ending date (e.g., `Fri, Dec 18, 2026`).
- **`DAYS_FRACTION`**: Displays the fraction of days completed (e.g., `12 / 30 days completed` in wide cards or `12 / 30 days` in compact radial widgets).

### Why UTC-to-Local Noon Normalization Matters (`CountdownDateUtils`)
Material 3's `DatePicker` emits selected dates as **00:00:00 UTC midnight** timestamps. In negative UTC offset timezones (such as US Pacific `UTC-8` or Eastern `UTC-5`), interpreting `00:00:00 UTC` directly in local time shifts the date backward to the previous evening (e.g., picking `Nov 8` yields `Nov 7, 4:00 PM PST`).

To eliminate off-by-one-day bugs and Daylight Saving Time (DST) drift:
- **`utcPickerMillisToLocalMillis()`** extracts the `(YEAR, MONTH, DAY_OF_MONTH)` in `UTC` and constructs a local `Calendar` timestamp at **12:00:00.000 noon local time**.
- **`localMillisToUtcPickerMillis()`** converts a local timestamp back to `00:00:00.000 UTC` for `rememberDatePickerState`.
- **`toLocalDayOrdinal()`** and **`calendarDaysBetween()`** compute integer day counts strictly from local calendar dates `(YEAR, MONTH, DAY_OF_MONTH)`, ensuring day counts decrement by exactly `1` at local midnight regardless of time-of-day or DST transitions.

---

## 4. Widget Customization Options (Shapes & Color Themes)

### 7 Android Widget Shapes (`WidgetShapeStyle`)

| Enum Name | Display Name | Geometry & Aspect Ratio |
| :--- | :--- | :--- |
| `SQUIRCLE` | **Squircle** | Horizontal (`540×340` canvas), `76px` corner radius |
| `ROUNDED` | **Rounded** | Horizontal (`540×340` canvas), `44px` corner radius |
| `PILL` | **Capsule** | Horizontal (`540×340` canvas), full stadium capsule (`height / 2` radius) |
| `CIRCLE` | **Circle** | Radial (`460×460` square canvas), true circle |
| `HEXAGON` | **Hexagon** | Radial (`460×460` square canvas), 6-sided polygon with quadratic Bezier rounded vertices (`smoothing = 0.18`) |
| `CLOVER` | **Clover** | Radial (`460×460` square canvas), 4-petal polar curve matching Google Pixel Clock (`rFactor = (1 - 0.16 * cos(4θ)) / 0.936`) with diagonal lobes at `45°, 135°, 225°, 315°` |
| `FLOWER` | **Flower** | Radial (`460×460` square canvas), 12-petal sinusoidalscalloped badge (`45%` base radius + `5%` cosine wave) |

### 15 Widget Color Themes (`WidgetThemeColor`)

| ID | Display Name | Background (`surfaceHex`) | Foreground (`primaryHex` / `onSurfaceHex`) | Title & Progress Fill (`titleHex`) |
| :--- | :--- | :--- | :--- | :--- |
| `DYNAMIC` | **Device Dynamic (MUI)** | `system_accent2_50` (Light) / `system_accent2_800` (Dark) | `system_accent1_800` (Light) / `system_accent1_100` (Dark) | `system_accent3_800` (Light) / `system_accent3_100` (Dark) |
| `WHITE` | **Snow White** | `#FFFFFF` | `#0F172A` | `#0F172A` |
| `CHARCOAL` | **Charcoal Black** | `#18181B` | `#F4F4F5` | `#F4F4F5` |
| `OCEAN` | **Ocean Azure** | `#0A3055` | `#D8E6FF` | `#D8E6FF` |
| `BABY_BLUE` | **Baby Blue** | `#8FD9FB` | `#002B40` | `#002B40` |
| `CORAL` | **Cinnamon Brown** | `#4C2700` | `#FFDCC1` | `#FFDCC1` |
| `TANGERINE_ORANGE` | **Tangerine Orange** | `#FA8128` | `#230D00` | `#230D00` |
| `AMBER` | **Golden Yellow** | `#FFCA28` | `#3E2700` | `#3E2700` |
| `EMERALD` | **Emerald Green** | `#0B3926` | `#C4F1BE` | `#C4F1BE` |
| `MINT_GREEN` | **Mint Green** | `#98FF98` | `#063B1A` | `#063B1A` |
| `VIOLET` | **Cosmic Violet** | `#331C5E` | `#EADDFF` | `#EADDFF` |
| `COTTON_CANDY_PINK` | **Cotton Candy Pink** | `#FFB7D5` | `#4A0E2E` | `#4A0E2E` |
| `ROSE_RED` | **Rose Red** | `#DE0038` | `#FFE5EC` | `#FFD6A5` |
| `ROSE` | **Mellow Maroon** | `#561329` | `#FFD9DE` | `#FFD9DE` |
| `SLATE` | **Midnight Blue** | `#1B263B` | `#DCE2F9` | `#DCE2F9` |

---

## 5. Home Screen Widget Engine (`DayCountWidgetProvider`)

### Why Layered `RemoteViews` + `Canvas` Is Used
Android `RemoteViews` natively supports only a restricted subset of XML views and does not support arbitrary custom `Path` clipping (like 4-petal Clover, 12-petal Flower, or smoothed Hexagon) or variable font axis settings (`fontVariationSettings`).

At the same time, first-party Google Pixel widgets (Google Clock and Google Weather) derive their **Device Dynamic (MUI)** background surface from the launcher's local `AppWidgetHostView` resource context (`@android:color/system_accent2_50` in Light mode and `@android:color/system_accent2_800` in Dark mode).

To achieve both **custom geometric shapes** and **exact Pixel Launcher dynamic color matching**, `res/layout/widget_daycount.xml` uses a two-layer `FrameLayout`:
1. **Background Mask Layer (`@id/widget_bg_image`)**:
   - When `WidgetThemeColor.DYNAMIC` is active on Android 12+ (`API 31+`), `renderWidgetBackgroundMaskBitmap()` renders a white alpha-mask bitmap of the widget's exact shape and opacity (`0%–100%`).
   - `DayCountWidgetProvider` applies `views.setColorStateList(R.id.widget_bg_image, "setImageTintList", R.color.widget_dynamic_bg)`.
   - Because `@color/widget_dynamic_bg` is a resource reference (`values-v31` $\rightarrow$ `@android:color/system_accent2_50`; `values-night-v31` $\rightarrow$ `@android:color/system_accent2_800`), the launcher evaluates it inside its own `AppWidgetHostView` theme context, guaranteeing a 1:1 match with adjacent Google Clock and Weather widgets.
2. **Foreground Content Layer (`@id/widget_canvas_image`)**:
   - `renderWidgetBitmap()` draws the typography and progress bar (plus the flat background fill when a static color theme is chosen).
   - In `DYNAMIC` mode, it resolves:
     - **Title & Active Progress Bar Fill**: Tertiary warm accent (`system_accent3_100` in Dark mode / `system_accent3_800` in Light mode).
     - **Hero Day Count, Label & Ending Date Footer**: Primary accent (`system_accent1_100` in Dark mode / `system_accent1_800` in Light mode).

---

## 6. Variable Typography (`Google Sans Flex`)

All widget text elements use the bundled **`Google Sans Flex`** variable font (`app/src/main/res/font/google_sans_flex.ttf`):
- **Roundness (`'ROND' 100.0`)**: Gives numerals and letterforms the signature rounded Google Pixel aesthetic.
- **Dynamic Optical Size (`'opsz' 6.0..144.0`)**: Calculated via `computeWidgetOpticalSize(baseSp, widgetMinWidthDp, widgetMinHeightDp)` using the widget's current dimensions from `AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH` / `MIN_HEIGHT`. When a user resizes the widget on their home screen, `onAppWidgetOptionsChanged` recalculates `'opsz'` so glyph stroke thickness and aperture scale optically.
- **Adaptive Title Scaling (`fitTitleTextPaint`)**: Enforces a 32-character maximum title length and iteratively steps down the font size (recomputing `'opsz'` at each step) so long titles fit cleanly inside radial and horizontal shapes without premature truncation.

---

## 7. 24-Hour Midnight Rollover & Sync Lifecycle

Countdown widgets must decrement their remaining day count and advance their progress bar immediately when a new calendar day begins at `00:00:00` local time:
1. **Midnight Alarm Scheduling (`scheduleNextMidnightUpdate`)**:
   - Computes `00:00:01.000` local time of the next day via `CountdownDateUtils.nextMidnightEpochMs()`.
   - Schedules a wakeup alarm via `AlarmManager` (`setExactAndAllowWhileIdle` when permitted, gracefully falling back to a 1-second `setWindow` or `setAndAllowWhileIdle` if exact alarm permission is unavailable).
2. **Midnight Broadcast Execution (`MidnightWidgetAlarmReceiver`)**:
   - Fires at `00:00:01`, immediately reschedules the alarm for the *following* midnight, and refreshes all active home screen widgets.
3. **System Time & Boot Recovery**:
   - `DayCountWidgetProvider` also listens for `ACTION_BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`, `ACTION_DATE_CHANGED`, `ACTION_TIME_SET`, and `ACTION_TIMEZONE_CHANGED` to reschedule the midnight alarm and refresh widgets if the device reboots or the user travels across timezones.

---

## 8. In-App Jetpack Compose UI Layer

- **Interactive Widget Studio (`WidgetPreviewShowcase`)**:
  - Displays the currently selected countdown timer at the top of the home screen over a customizable backdrop (`Aurora`, `Ocean`, `Sunset`, `Minimal`) that adapts to Light and Dark app themes.
  - Provides instant chip selectors for **Widget Shape**, **Widget Color Theme** (with circular color swatches matching each theme's exact background `surfaceHex`), and **Widget Opacity** (`0%–100%`).
- **List Cards (`MaterialWidgetCard`)**:
  - Renders each saved timer using a Compose `GenericShape` matching the home screen widget's geometry (`Squircle`, `Rounded`, `Capsule`, `Circle`, `Hexagon`, `Clover`, `Flower`).
  - Action buttons (`Add to Home Screen`, `Edit`, `Delete`) sit in a clean pill bar *outside* the widget silhouette so the preview remains uncluttered.

---

## 9. Security, Privacy & Backup Architecture

- **Minimal Permissions**: Declares only `android.permission.RECEIVE_BOOT_COMPLETED` (normal install-time permission). Zero network (`INTERNET`), location, camera, or storage permissions are requested.
- **Broadcast Receiver Isolation**:
  - The custom midnight alarm action (`ACTION_MIDNIGHT_WIDGET_UPDATE`) is handled exclusively by the non-exported `MidnightWidgetAlarmReceiver` (`android:exported="false"`).
  - The exported `DayCountWidgetProvider` ignores custom broadcast actions from external callers and only processes OS-protected system broadcasts.
- **Input Length Sanitization**:
  - Titles are capped at `32` characters (`MAX_TITLE_LENGTH`), optional subtitles at `80` characters (`MAX_SUBTITLE_LENGTH`), search queries at `100` characters (`MAX_SEARCH_QUERY_LENGTH`), and specified day counts are clamped to `1..36,500` days (`MAX_SPECIFIED_DAYS`), preventing SQLite `CursorWindow` overflow or rendering stalls.
- **Background Coroutine Exception Containment**:
  - `DayCountWidgetProvider.updateWidgetsInternal` wraps background work in `try / catch (Exception) / finally { pendingResult?.finish() }` to ensure transient I/O errors cannot crash the app process.
- **Backup & Device-Transfer Hygiene**:
  - `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` back up `countdown_timer_database` and `daycount_app_prefs.xml` while excluding `daycount_widget_prefs.xml` (which stores device-local `appWidgetId` integers).

---

## 10. Testing Strategy (`ExampleRobolectricTest`)

Unit and integration tests live in `app/src/test/java/com/example/ExampleRobolectricTest.kt` and run on the local JVM using **Robolectric** (`@Config(sdk = [34])`):
- **Countdown Math & Progress Fraction**: Verifies remaining days, elapsed days, and progress bar calculations across both `TARGET_DATE` and `DAY_COUNT` modes.
- **Timezone & DatePicker Round-Trip Safety**: Verifies that selecting dates in `UTC` preserves the exact calendar date across `America/Los_Angeles`, `America/New_York`, `UTC`, and `Asia/Tokyo`.
- **Midnight Rollover & Alarm Scheduling**: Verifies that crossing `23:59:59` $\rightarrow$ `00:00:01` decrements `daysRemaining` by `1`, advances `progressFraction`, schedules `AlarmManager` at `00:00:01`, rejects spoofed broadcasts on the exported provider, and reschedules via `MidnightWidgetAlarmReceiver`.
- **Widget Rendering & Palette Verification**: Verifies bitmap dimensions (`540×340` for horizontal shapes, `460×460` for radial shapes including `CLOVER`), `Google Sans Flex` variation strings, Material You dynamic tonal resolution (`system_accent1`, `system_accent2`, `system_accent3`), and all 15 `WidgetThemeColor` hex values.

Run the full test suite with:
```bash
gradle :app:testDebugUnitTest
```

---

## 11. Important Notes & Platform Nuances

- **Adding Multiple Home Screen Widgets**: Each pinned widget (`appWidgetId`) is mapped to a specific `timerId` in `WidgetUpdateHelper`. If a user drags a widget directly from the launcher's widget picker without pinning from inside the app, it automatically binds to the most recently created timer (`dao.getFirstTimerDirect()`).
- **Opacity Slider (`0%–100%`)**: Applies alpha transparency strictly to the widget's background fill (`surface`) while keeping the foreground typography and progress bar at full legibility so users can create floating text widgets (`0%` opacity) or translucent glass-style widgets over their wallpaper.
