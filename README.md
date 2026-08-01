# GO! Timer

**GO! Timer** is an offline-first Android app for *Monopoly GO!* players that tracks time-sensitive in-game events: the active season countdown, dice refill progress and full-pool projection, the store Free Gift cycle, and scheduled background notifications. Everything runs on-device — no account, no backend, no internet required.

The app is built for reliability, low battery usage, and a fast reactive UI. Timers are derived from stored timestamps (never decremented counters), so the state stays correct across app restarts and device reboots.

> **Disclaimer:** This is an unofficial, third-party utility and is not affiliated with, endorsed by, or connected to Scopely or *Monopoly GO!*.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Repository Structure](#repository-structure)
- [Prerequisites](#prerequisites)
- [Building](#building)
- [Running on a Device](#running-on-a-device)
- [Testing](#testing)
- [Configuration](#configuration)
- [Usage](#usage)
- [Notifications & Scheduling](#notifications--scheduling)
- [Calculation Rules](#calculation-rules)
- [Troubleshooting](#troubleshooting)
- [License](#license)
- [Related Documentation](#related-documentation)

## Features

- **Season countdown hero banner** — high-contrast banner showing the active season name and a live `DDd HHh MMm` countdown to the configured season end.
- **Dice refill tracker** — current vs. max dice with a progress bar, countdown to the next hourly refill, and an automatic "Full in Xh Ym · At HH:MM" projection that disables itself once the pool is full.
- **Free Gift tracker** — an 8-hour countdown that flips to **READY TO CLAIM**, with a one-tap "Claimed Just Now" action.
- **Quick Update sheet** — preset chips for dice (`0`, multiples of your hourly refill rate, `MAX`), next refill (`0m`–`60m`), and Free Gift status, plus a custom dice input. No typing required for common states.
- **"Just Played" batch action** — a sticky button that applies a configurable reset (zero dice, refill timer back to 60m, restart gift timer) with a single tap, also triggered by swiping left on the dice card.
- **Background notifications** — exact alarms for dice-full, Free Gift ready, and 24h/1h season reminders, with shade quick actions (Claimed / Just Played / Snooze). Alarms are rebuilt after device reboot.
- **Home screen widget** — a Glance app widget showing dice count, full-pool projection, next refill, and gift countdown at a glance.
- **Timeline view** — an "Upcoming" card listing the next refill, Free Gift, full-pool, and season-end events in chronological order.

## Architecture

The project follows a strict MVVM layering with business calculations isolated from the UI. State flows one way:

```
UI (Compose)  →  ViewModel  →  DiceRepository  →  DataStore (Preferences)
                                      │
                                      └──> NotificationScheduler ──> AlarmManager
```

Key design principles (from [AGENTS.md](AGENTS.md)):

- **Offline first** — all state is stored locally in DataStore; no network calls anywhere.
- **Time is the source of truth** — only baseline timestamps and raw values are persisted. The stored dice count is a baseline anchored at the start of the current refill cycle; elapsed refill cycles are accrued **on read** via `ProjectionCalculator.calculateEffectiveDice()`, so nothing is decremented over time.
- **Reactive UI** — screens observe `StateFlow`s and recompose automatically. The dashboard's one-second ticker runs only while the screen is subscribed (`SharingStarted.WhileSubscribed`), so no work happens in the background.
- **Pure calculation engine** — `ProjectionCalculator` and `CountdownFormatter` are side-effect-free; time is always passed in explicitly, making them deterministic and unit-testable.
- **Battery efficiency** — `AlarmManager` is used only for notification alarms; there is no polling service.

### Notable components

| Component | Responsibility |
| --- | --- |
| `DiceRepository` | Single data-access entry point: exposes `Flow<AppState>` and applies every mutation as one atomic DataStore edit. |
| `PreferencesMapper` | Maps raw DataStore `Preferences` into `AppState`/`UserPreferences`, applying defaults, clamping, and fallbacks for corrupt timestamps. |
| `SchedulePlanner` | Pure logic deciding which alarms to arm for a given state (fully unit-testable, no Android dependencies). |
| `NotificationScheduler` | Arms/cancels `AlarmManager` alarms via `PendingIntent`s to `NotificationReceiver`; falls back to a 10-minute inexact window where exact alarms aren't permitted (API 31–32). |
| `BootReceiver` | Rebuilds alarms and refreshes the widget after `ACTION_BOOT_COMPLETED`. |
| ViewModels | One per feature (`Dashboard`, `Settings`, `Update`); none know another's internals, and all depend on the `NotificationRescheduler` interface for testability. |

The app is single-activity (`MainActivity`); navigation between dashboard and settings is simple state-based switching, and the Quick Update is a modal bottom sheet.

## Repository Structure

```
├── app/
│   ├── build.gradle.kts            # App module: AGP config, dependencies
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/gotimer/
│       │   │   ├── ui/             # Compose UI
│       │   │   │   ├── MainActivity.kt
│       │   │   │   ├── dashboard/  # Dashboard screen + cards (hero, dice, gift, timeline)
│       │   │   │   ├── settings/   # Settings window
│       │   │   │   ├── update/     # Quick Update bottom sheet
│       │   │   │   ├── widgets/    # Glance home-screen widget
│       │   │   │   ├── components/ # Stateless reusable UI (ActionButton, PresetChip, ...)
│       │   │   │   └── theme/      # Material 3 theme
│       │   │   ├── model/          # AppState, UserPreferences (immutable data classes)
│       │   │   ├── repository/     # DiceRepository
│       │   │   ├── datastore/      # DataStore delegate, keys, preferences mapper
│       │   │   ├── calculations/   # ProjectionCalculator, CountdownFormatter
│       │   │   ├── scheduler/      # AlarmManager scheduling + boot recovery
│       │   │   ├── notifications/  # Channels + notification factory
│       │   │   ├── viewmodel/      # Feature ViewModels + UI state models
│       │   │   └── util/           # InputValidator, TimeConstants
│       │   └── res/                # Resources, widget metadata
│       ├── test/                   # JVM unit tests (JUnit4 + coroutines-test)
│       └── androidTest/            # Instrumented Compose UI tests
├── build.gradle.kts                # Root build: plugin versions
├── settings.gradle.kts             # Repos, rootProject name "GoTimer", :app module
├── gradle.properties               # JVM args, AndroidX flags
├── gradle/                         # Gradle wrapper
├── AGENTS.md                       # Contributor/agent conventions
├── IMPLEMENTATION_PLAN.md          # Build roadmap by phase
├── SPECIFICATION.md                # Product specification & formulas
└── LICENSE                         # MIT
```

## Prerequisites

- **JDK 17** (the project targets JVM 17).
- **Android SDK** with API 36 (`compileSdk`). Set `ANDROID_HOME` (or a `local.properties` with `sdk.dir=...`), or open the project in Android Studio and let it configure the SDK.
- Android Studio (recommended) or the Android command-line tools.
- For instrumented UI tests: an emulator or physical device.

All other dependencies (Gradle wrapper, Android Gradle Plugin 8.13.2, Kotlin 2.2.21, Compose, etc.) are declared in the build scripts and downloaded automatically. There is no version catalog; the Compose BOM `2026.06.01` is used for Compose artifacts.

## Building

The Gradle wrapper is included, so no global Gradle install is needed:

```bash
# Build a debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Run Android Lint
./gradlew lintDebug
```

A debug build is signed with the debug keystore and installable directly. A release build can be produced with:

```bash
./gradlew assembleRelease
```

> **Note:** no signing configuration is committed to this repository. Release builds are unsigned by default; add your own `signingConfigs`/`release` configuration in `app/build.gradle.kts` (or sign via Android Studio → *Build → Generate Signed App Bundle / APK*) before distribution.

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Running on a Device

With a device connected over ADB:

```bash
./gradlew installDebug
adb shell am start -n com.gotimer/.ui.MainActivity
```

Or simply open the project in Android Studio, select a device/emulator, and press **Run**. The first launch requests the notification permission on Android 13+.

**Minimum API:** 26 (Android 8.0). **Target/compile API:** 36.

## Testing

**Unit tests** (JVM, no device needed) — cover the calculation engine, countdown formatting, scheduling plan, input validation, the repository/DataStore flow, and all three ViewModels:

```bash
./gradlew testDebugUnitTest
```

Test reports are written to `app/build/reports/tests/testDebugUnitTest/`.

**Instrumented UI tests** (Compose, requires an emulator/device):

```bash
./gradlew connectedDebugAndroidTest
```

These run real end-to-end flows through `MainActivity`, the ViewModels, and DataStore — e.g. saving a dice chip in the Quick Update sheet and seeing it reflected on the dashboard, and the "Just Played" batch action.

There is currently no CI configuration committed to the repository.

## Configuration

All persistent settings live in the **Settings** screen (gear icon in the header) and are stored in the `go_timer_preferences` DataStore file. Edits are staged locally and saved atomically with the top-right **SAVE** button.

| Setting | Default | Notes |
| --- | --- | --- |
| Season Name | `Current Season` | Shown on the hero banner. |
| Season End Date & Time | Now + 30 days | Picked with date + time pickers. |
| Max Dice Capacity | `80` | Cap on free dice accrual; clamped to `>= 0`. |
| Hourly Refill Count | `10` | Dice generated per hour; clamped to `>= 0`. |
| Just Played: Set Dice to 0 | on | Batch action zeroes current dice. |
| Just Played: Reset Refill Timer | on | Batch action resets the refill to 60 minutes. |
| Just Played: Reset Free Gift | off | Batch action restarts the 8-hour gift cycle. |
| Push Notifications | on | Enables the alarm-scheduled notifications. |
| Notification Lead Time | `5 minutes before` | Options: `0m`, `5m`, `10m`, `15m`, `30m`, `1h`. |

There are no environment variables or secrets in this project.

## Usage

### Quick Update

Tap **UPDATE** in the header to open the Quick Update sheet. Select any combination of categories — SAVE applies only the ones you touched:

- **Current Dice Count:** `0`, multiples of your hourly refill rate, and `MAX`, or type a custom value in the field (clamped into `0..maxDice`).
- **Minutes until Next Refill:** `0m`, `15m`, `30m`, `45m`, `60m`.
- **Free Gift Status:** `Just Claimed (8h)`, `4h Left`, `2h Left`, `Ready Now (0m)`.

### "Just Played"

Tap **⚡ JUST PLAYED** (sticky bottom bar) or swipe **left** on the dice card. It applies the configured batch update — by default zeroing dice and resetting the refill timer to 60 minutes. Swipe **right** on the Free Gift card to mark it claimed.

### Home screen widget

Long-press the home screen → *Widgets* → **GO! Timer** to add a 4×2 widget showing `GO! 32/80`, the full-pool status, and the refill/gift countdowns. It refreshes on a 30-minute cycle, on device boot, and whenever the app is opened.

## Notifications & Scheduling

Notifications are scheduled with one-shot `AlarmManager` alarms (`RTC_WAKEUP`), exact where permitted and falling back to a 10-minute window otherwise (API 31–32 special access). The full plan is computed from persisted state by `SchedulePlanner` and re-armed whenever timers or settings change.

| Notification | Channel | Priority | Trigger |
| --- | --- | --- | --- |
| Dice are full | `channel_dice_alerts` | High | Projected full-pool epoch minus lead time; skipped when already full. |
| Free Gift ready | `channel_gift_alerts` | Default | Gift claimable epoch minus lead time; skipped when already claimable. |
| Season ending soon | `channel_season_alerts` | Default | Exactly 24h and 1h before season end; only future thresholds are armed. |

Quick actions are available directly from the notification shade:

- **Just Played** (dice full notification) — resets the refill timers.
- **Claimed** (gift notification) — restarts the 8-hour gift cycle.
- **Snooze** — defers the notification by 15 minutes without disturbing the rest of the plan.

The `POST_NOTIFICATIONS` runtime permission is requested on first launch on Android 13+. After a device reboot, `BootReceiver` recalculates remaining times and re-arms all alarms, and refreshes the widget. All timestamps are epoch milliseconds.

## Calculation Rules

The dice projection follows the specification's formulas exactly (`ProjectionCalculator`):

```
Remaining Dice Needed  = Max Dice − Current Dice
Full Cycles Needed     = ceil(Remaining Dice Needed / Hourly Refill Rate) − 1
Total Minutes to Full  = Minutes to Next Refill + (Full Cycles Needed × 60)
Projected Full Epoch   = now + (Total Minutes to Full × 60 × 1000)
```

Example (per the spec): max 80, current 15, rate 10/hr, next refill in 20 min → `80 − 15 = 65` dice needed, the next refill gives 10, then 6 full cycles → `20 + 360 = 380` minutes (6h 20m).

Countdown strings are produced by `CountdownFormatter` in two styles — compact (`14d 06h 22m`) and with seconds (`05h 42m 10s`) — and clock times format as 12-hour (`8:30 PM`). The stored dice baseline accrues completed refill cycles on read, so the count rolls forward automatically without any background timer.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `SDK location not found` | Set `ANDROID_HOME` in your environment or create a `local.properties` with `sdk.dir=/path/to/Android/Sdk`. |
| Notifications never fire | Grant the notification permission in system settings; on Android 12–13, confirm alarm access (the app falls back to inexact windows if exact alarms are denied). |
| Widget shows stale values | Widgets update on the 30-minute provider cycle, on boot, and when the app is opened — open the app once to refresh immediately. |
| Unit tests can't resolve dependencies | Run without `--offline` once to populate the Gradle cache, then try again. |
| Release APK won't install | Release builds are unsigned; add a signing config or use Android Studio's signed-build flow. | 

## License

[MIT](LICENSE) © 2026 Robert Lane.

## Related Documentation

- [SPECIFICATION.md](SPECIFICATION.md) — the product specification and full formula definitions.
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) — the phased build roadmap.
- [AGENTS.md](AGENTS.md) — architecture, coding-style, and contribution conventions.
