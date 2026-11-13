# DayCount — Day Countdown Widgets for Android

**DayCount** is a modern Material Design 3 Android countdown application and home screen widget suite. It lets you track upcoming events, milestones, and multi-day streaks with customizable geometric widget shapes, Material You dynamic colors, and curated color palettes designed to blend seamlessly with Google Pixel home screens.

---

## Key Features

- **Two Flexible Countdown Modes**:
  - **Until Specific Date**: Count down to any calendar date (vacations, launches, holidays) with an optional starting date to customize your progress bar percentage.
  - **Specified Days**: Track fixed-length challenges and streaks (`1` to `36,500` days) with a tap-to-edit numeric keypad and quick presets (`7`, `14`, `21`, `30`, `60`, `90`, `100`, `365` days).
- **7 Custom Android Widget Shapes**:
  - **Horizontal Shapes**: `Squircle`, `Rounded`, `Capsule`
  - **Radial Shapes**: `Circle`, `Hexagon`, `Clover` (4-petal Google Clock style), `Flower` (12-petal badge)
- **15 Color Themes + True Material You Dynamic Coloring**:
  - **Device Dynamic (MUI)**: Matches first-party Google Pixel widgets (`system_accent2_50` in Light mode / `system_accent2_800` in Dark mode) with multi-accent Material You typography (`system_accent3` warm accent for title & active progress bar; `system_accent1` primary accent for day count & footer).
  - **14 Curated Static Palettes**: *Snow White*, *Charcoal Black*, *Ocean Azure*, *Baby Blue*, *Cinnamon Brown*, *Tangerine Orange*, *Golden Yellow*, *Emerald Green*, *Mint Green*, *Cosmic Violet*, *Cotton Candy Pink*, *Rose Red*, *Mellow Maroon*, and *Midnight Blue*.
- **Variable Typography (`Google Sans Flex`)**:
  - Renders widget numerals and titles using bundled `Google Sans Flex` with Roundness `100` (`'ROND' 100`) and dynamic Optical Sizing (`'opsz'`) that adapts when widgets are resized on the home screen.
- **Customizable Footer & Opacity (`0%–100%`)**:
  - Toggle the bottom countdown label on or off, switch between **Ending Date** (`Fri, Dec 18, 2026`) and **Fraction of Days Completed** (`12 / 30 days completed`), and adjust background opacity from `0%` (transparent floating text) to `100%` (solid fill).
- **Automatic 24-Hour Midnight Rollover**:
  - Home screen widgets automatically refresh at `00:00:01` local time every night via `AlarmManager` and respond to timezone, clock, and device boot events.

---

## Documentation

For an in-depth technical walkthrough of the architecture, package structure, widget rendering pipeline, timezone-safe calendar math, and security model, see:

- **[Codebase & Architecture Guide (`docs/ARCHITECTURE.md`)](docs/ARCHITECTURE.md)**

---

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Home Screen Widgets**: Android `AppWidgetProvider`, layered `RemoteViews`, and custom 2D `Canvas` path rendering
- **Local Persistence**: Room SQLite Database (`androidx.room`) & `SharedPreferences`
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Testing**: JUnit 4 & Robolectric (`@Config(sdk = [34])`)

---

## Building & Running Tests

### Compile Debug APK
```bash
gradle assembleDebug
```

### Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

---

## License & Copyright

Copyright (c) 2026 Akash Kumar.

Licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
