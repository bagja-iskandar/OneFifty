# 🏃 TEMPO 150 — Workout Countdown Recorder

**TEMPO 150** is a minimal, frictionless mobile application designed to help users log their physical activities and achieve the recommended **150 minutes of weekly workout** (AHA / WHO guideline).

Built with **Kotlin Multiplatform (KMP)** and **Jetpack / Compose Multiplatform**, TEMPO 150 focuses on speed, simplicity, and zero-distraction workout logging.

---

## 🌟 Key Features

* **150-Minute Target Direct Countdown (`H:MM:SS`)**:
  * Displays remaining weekly goal time directly in the main circle (`2:30:00` ➔ `2:29:59`).
  * Green Checkmark (`✓`) celebration state when the 150-minute weekly goal is reached.
* **Activity-First Selection**:
  * 8 square rounded activity tiles (`Running 🏃`, `Walking 🚶`, `Cycling 🚴`, `Swimming 🏊`, `Gym 🏋️`, `Yoga 🧘`, `Cardio ❤️`, `Other ⚡`).
  * Tap to select/deselect activities before starting workouts.
* **Ambient Heartbeat Aura (`drawBehind`)**:
  * Organic 4.8s heartbeat pulse aura that glows in **Emerald Green (`#34D399`)** during active workouts and **Primary Orange (`#FF6B4A`)** when ready.
  * Rendered via `drawBehind` so drawing glows never affect screen layout or cause scrolling.
* **Single Weekly Stacked Horizontal Bar (150m Full Track)**:
  * Full track represents 100% of the 150-minute goal (9000s).
  * Colored portion represents actual workout progress with embedded activity emoji icons.
* **Consistent Global Activity Color Palette**:
  * Each activity type has a fixed, consistent theme color across Workout Share, Daily Timeline stacked bars, and active legends.
* **Real-Time System Calendar (`DateUtils`)**:
  * Live Monday–Sunday week calculations (`28 Sep – 4 Oct, 2025`).
  * Automatic Monday week rollover reset to 0 minutes for a fresh start each week.
* **100% Local-Only Storage (`LocalWorkoutRepository`)**:
  * Lightweight `SharedPreferences` storage (< 5 KB disk, < 15 MB RAM) with zero cloud overhead.
  * Stored Data Inspector & Clear All Local Data reset features in Settings.
* **Android 1x1 Home Screen Live Widget (`OneFiftyWidgetProvider`)**:
  * Live progress percentage (`80%`) and minute total (`120/150m`) widget on your phone's home screen.
* **Horizontal Swipe Page Navigation (`HorizontalPager`)**:
  * Smooth horizontal swipe gestures between **Session**, **Weekly**, and **Settings** screens.

---

## 🚀 Tech Stack

* **Language**: Kotlin 2.0+
* **Framework**: Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP)
* **Architecture**: Clean Architecture / Repository Pattern (`WorkoutRepository`)
* **State Management**: Reactive `StateFlow` & Jetpack Compose State
* **UI & Design Tokens**: Jetpack Compose Material 3 (Custom Dark Theme)
* **Persistence**: Android `SharedPreferences` + JSON Serialization
* **Widget**: Android AppWidget Framework (`AppWidgetProvider`)

---

## 🛠️ Project Structure

```
OneFifty/
├── shared/                             <-- KMP Shared Module (Multiplatform)
│   └── src/commonMain/kotlin/com/example/onefifty/
│       ├── ui/
│       │   ├── theme/              (Color.kt, Type.kt, Theme.kt - Design Tokens)
│       │   ├── model/              (ActivityType, TimerState, WorkoutSessionItem)
│       │   ├── components/         (TempoTopBar, TimerCircleView, ActivitySelector, WeeklyBreakdownCard)
│       │   └── screens/            (SessionScreen, WeeklyScreen, SettingsScreen)
│       └── util/                   (DateUtils.kt - Real-time Calendar Engine)
│
└── app/                                <-- Android Application Module
    ├── src/main/java/com/example/onefifty/
    │   ├── MainActivity.kt         (Entry point with HorizontalPager & Timer State)
    │   ├── repository/             (LocalWorkoutRepository.kt - Local Storage Engine)
    │   └── widget/                 (OneFiftyWidgetProvider.kt - 1x1 Live Home Widget)
    └── src/main/res/               (Vector drawables, App icons, Layouts)
```

---

## 📄 License

Copyright © 2026 Bagja Iskandar. All rights reserved.
