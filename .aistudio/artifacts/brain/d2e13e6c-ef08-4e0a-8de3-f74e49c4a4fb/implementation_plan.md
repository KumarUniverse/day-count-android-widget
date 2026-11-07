# DayCount — APK Size & Codebase Lightweight Optimization

This update removes unused third-party libraries from the Gradle build, cleans up unused imports across Kotlin source files, and deletes unreferenced methods and color constants so the app is as small, fast, and lightweight as possible.

### User Review & Critical Decisions

> [!IMPORTANT]
> Please review the audit findings below and click **Proceed** to apply these optimizations.

- **Comment Out Unused Heavy Libraries in `app/build.gradle.kts` (Major APK Size Reduction)**:
  - The project currently bundles **Firebase AI**, **Firebase App Check (reCAPTCHA & Debug)**, **Retrofit**, **OkHttp**, **OkHttp Logging Interceptor**, and **Moshi (+ KSP Codegen)**, none of which are used by DayCount. Commenting out these unused dependencies strips several megabytes of unused network/cloud SDK classes from the APK.
- **Remove Unused Imports (`WidgetCustomShapes.kt`)**:
  - Remove unreferenced imports: `CornerRadius`, `Rect`, `RoundRect`, and `kotlin.math.min`.
- **Prune Dead Code & Unused Constants**:
  - **DAO & Repository**: Remove unreferenced methods (`getTimerById`, `deleteTimerById`, `deleteById`, `getTimerByIdOrFirst`).
  - **Data Model**: Remove unreferenced `formattedStartDate()` helper in `CountdownTimer`.
  - **Theme Colors & Typography**: Remove 15 unused template color constants (`Purple80`, `PurpleGrey80`, `Pink80`, `Purple40`, `PurpleGrey40`, `Pink40`, etc.) in `Color.kt` and commented-out template blocks in `Type.kt`.

---

### 1. Overview & Core Concept

- **What It Does**: Audits every file and Gradle dependency in DayCount, keeping only Jetpack Compose, Material 3, Room (local SQLite), and Kotlin Coroutines.
- **Key Value**: Minimizes APK footprint, reduces memory overhead, and accelerates build and startup times without changing any app or widget behavior.

---

### 2. Technical Architecture & Data Strategy *(Technical Reference)*

- **Optimization Summary Diagram**:
```
┌──────────────────────────────────────────────────────────────────────┐
│                   Gradle Dependencies (build.gradle.kts)             │
│  [KEEP]   Jetpack Compose + Material 3 + Room + Coroutines           │
│  [REMOVE] Firebase BOM / AI / AppCheck, Retrofit, OkHttp, Moshi KSP  │
└──────────────────────────────────┬───────────────────────────────────┘
                                   │
                                   ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Kotlin Code & Import Cleanup                      │
│  • WidgetCustomShapes: Remove 4 unused geometry/math imports         │
│  • CountdownDao & Repository: Remove 4 unreferenced query helpers    │
│  • CountdownTimer: Remove unused formattedStartDate() method         │
│  • Color.kt & Type.kt: Remove 15 unused template color constants     │
└──────────────────────────────────────────────────────────────────────┘
```
