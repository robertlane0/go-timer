# IMPLEMENTATION_PLAN.md

## Overview

This implementation plan outlines the step-by-step roadmap for building **GO! Timer**, an offline-first Android application designed for *Monopoly Go!* players. The architecture strictly adheres to offline-first reactive principles, keeping business calculations isolated from the UI layer, storing timestamps as the source of truth, and scheduling notifications efficiently.

---

## Technical Stack & Target Architecture

* **Language:** Kotlin


* **UI Framework:** Jetpack Compose with Material 3


* **Architecture Pattern:** MVVM (Model-View-ViewModel)


* **Asynchronous / Stream Processing:** Kotlin Coroutines & Flow / StateFlow


* **Persistence Layer:** DataStore Preferences


* **Background Scheduling:** WorkManager and AlarmManager (exact alarms)



### Project Directory Structure

The project structure enforces strict layer separation:

```text
app/
 ├── ui/
 │    ├── dashboard/       # Main screen components (Hero Banner, Dice Tracker, Free Gift Card)
 │    ├── settings/        # Settings window and preferences UI
 │    ├── update/          # Quick Update bottom sheet and preset chips
 │    ├── widgets/         # Android App Widgets (Planned Expansion)
 │    └── components/      # Reusable UI cards, chips, and buttons
 ├── model/                # Data models and state wrappers (AppState, UserPreferences)
 ├── repository/           # Data access abstraction linking DataStore and ViewModel
 ├── datastore/            # DataStore keys and serialization utilities
 ├── notifications/        # Channel setup, notification builders, and broadcast actions
 ├── scheduler/            # AlarmManager / WorkManager handlers and BootReceiver
 ├── calculations/         # Pure functions for projections, remaining times, and formatting
 ├── viewmodel/            # Feature-specific ViewModels (Dashboard, Settings, Update)
 └── util/                 # Formatting, extensions, and constants
```

---

## Phase 1: Core Domain & Calculation Engine

### Objective
Create pure calculation functions and immutable data structures without dependencies on Android UI components.

### Tasks

1. **Define Core State Models (`model/`)**
   * `AppState`: Encapsulates season info, dice count, refill timestamps, free gift timestamps, and user preferences.
   * `UserPreferences`: Holds max dice capacity, hourly refill count, season settings, notification lead times, and "Just Played" configuration flags.

2. **Implement Calculation Engine (`calculations/`)**
   * Build `ProjectionCalculator.kt` as pure functions with zero side effects:
     * **Dice Projection Formula:**
       $$\text{Remaining Dice Needed} = \text{Max Dice} - \text{Current Dice}$$

       $$\text{Full Cycles Needed} = \left\lceil \frac{\text{Remaining Dice Needed}}{\text{Hourly Refill Rate}} \right\rceil - 1$$

       $$\text{Total Minutes to Full} = \text{Minutes to Next Refill} + (\text{Full Cycles Needed} \times 60)$$

     * **Estimated Clock Time:** `System.currentTimeMillis() + (Total Minutes to Full * 60 * 1000)`.
     * **Countdown Formatter:** Formats duration into standard strings (`DDd HHm SSs`, `HHh MMm`, or `MMm SSs`).
     * **Progress Calculation:** Calculates progress fractions `[0.0, 1.0]` for progress indicators.
     * **Free Gift Remaining:** Calculates time left until the 8-hour gift availability timestamp.

3. **Input Validation & Guard Rails (`util/ Error Handling`)**
   * Clamp dice input to `0 <= currentDice <= maxDice`.
   * Clamp refill minutes to `0 <= minutes <= 60`.
   * Fall back safely on invalid/corrupted timestamps.

4. **Unit Tests (`test/calculations/`)**
   * Test projection logic for partial refills and zero/max dice states.
   * Test progress bar fraction logic.
   * Test countdown text formatting functions.

---

## Phase 2: Persistence & Repository Layer

### Objective
Store only baseline timestamps and configurable preferences locally using Jetpack DataStore.

### Tasks

1. **Configure DataStore (`datastore/`)**
   * Keys to persist:
     * `SEASON_NAME` (String)
     * `SEASON_END_EPOCH` (Long)
     * `MAX_DICE` (Int)
     * `HOURLY_REFILL_RATE` (Int)
     * `CURRENT_DICE` (Int)
     * `NEXT_REFILL_EPOCH` (Long)
     * `FREE_GIFT_EPOCH` (Long)
     * `JUST_PLAYED_ZERO_DICE` (Boolean)
     * `JUST_PLAYED_RESET_REFILL` (Boolean)
     * `JUST_PLAYED_RESET_GIFT` (Boolean)
     * `NOTIFICATIONS_ENABLED` (Boolean)
     * `NOTIFICATION_LEAD_TIME_MINUTES` (Int)

2. **Implement `DiceRepository` (`repository/`)**
   * Expose continuous `Flow<AppState>` derived from DataStore.
   * Expose asynchronous update methods:
     * `updateDiceCount(count: Int)`
     * `resetRefillTimer(minutesToNext: Int)`
     * `claimFreeGift()` (Sets epoch to current time + 8 hours)
     * `executeJustPlayedAction()` (Applies batch reset flags)
     * `saveSettings(prefs: UserPreferences)`

3. **Repository Unit Tests**
   * Verify DataStore updates correctly emit updated state through Kotlin Flows.

---

## Phase 3: Background Services & Notifications

### Objective
Schedule precise notifications using system alarms without background polling or battery drain.

### Tasks

1. **Notification Channels (`notifications/`)**
   * Register three distinct system channels:
     * `channel_dice_alerts`: High Priority (Dice Full).
     * `channel_gift_alerts`: Medium Priority (Free Gift Ready).
     * `channel_season_alerts`: Default Priority (24h and 1h Season Reminders).

2. **Notification Scheduler (`scheduler/`)**
   * Centralized `NotificationScheduler` class using `AlarmManager` for exact notifications.
   * Calculate lead times (e.g., trigger alert 5 minutes before event completion).
   * Schedule alerts for:
     * Full Dice Reached epoch.
     * Free Gift Claimable epoch.
     * Season Ending threshold epochs.

3. **Boot Recovery Receiver (`scheduler/BootReceiver.kt`)**
   * Register `BroadcastReceiver` listening for `ACTION_BOOT_COMPLETED`.
   * On device restart, query `DiceRepository`, recalculate remaining times, and reschedule active notifications.

---

## Phase 4: ViewModel Layer

### Objective
Expose reactive UI state models via `StateFlow` and process UI actions cleanly.

### Tasks

1. **`DashboardViewModel` (`viewmodel/`)**
   * Exposes `StateFlow<DashboardUiState>`.
   * Recalculates display strings periodically (e.g., 1-second pulse while screen is visible).
   * Delegates all actual calculation logic to `ProjectionCalculator`.

2. **`SettingsViewModel` (`viewmodel/`)**
   * Reads, validates, and persists settings preferences.
   * Triggers `NotificationScheduler` rescheduling whenever capacity or rates change.

3. **`UpdateViewModel` (`viewmodel/`)**
   * Manages quick preset choices for current dice, refill minutes, and free gift options.
   * Validates custom input ranges prior to saving.

---

## Phase 5: UI Implementation (Jetpack Compose)

### Objective
Build responsive, lightweight Material 3 screens adhering to the application specification.


```

+---------------------------------------------------+
|               GO! Timer Header                    |
| [ UPDATE ]                         [ Settings ⚙️ ]|
+---------------------------------------------------+
| 🏆 SEASON COUNTDOWN BANNER                        |
+---------------------------------------------------+
| 🎲 DICE REFILL TRACKER                            |
+---------------------------------------------------+
| 🎁 FREE GIFT TRACKER                              |
+---------------------------------------------------+
| ⚡ QUICK ACTION: JUST PLAYED                       |
+---------------------------------------------------+

```

### Tasks

1. **Reusable Components (`ui/components/`)**
   * Build small, stateless UI elements:
     * `CountdownCard`: Container card for digital counters.
     * `ProgressCard`: Displays current vs max values with circular or linear progress indicators.
     * `PresetChip`: Tappable Material 3 selection chip.
     * `ActionButton`: Main call-to-action button.

2. **Main Dashboard Screen (`ui/dashboard/`)**
   * **App Header:** Title with direct navigation to "UPDATE" sheet and "Settings" screen.
   * **Season Countdown Hero Banner:** High-contrast banner displaying active season title and dynamic digital countdown (`DDd HHm SSs`).
   * **Dice Tracker Card:**
     * Displays `Current / Max` count.
     * Linear/Circular progress bar.
     * Countdown to next hourly increment (+10 dice).
     * Secondary projection line: *"Full at HH:MM AM/PM (in Xh Ym)"*.
   * **Free Gift Card:**
     * Status display (`READY TO CLAIM` or countdown timer).
     * Instant action button: `[ Claimed Just Now ]`.
   * **Sticky Action Bar:** Prominent `[ ⚡ JUST PLAYED ]` button at the bottom.

3. **Quick Update Sheet (`ui/update/`)**
   * Bottom sheet interface with preset selection chips:
     * **Current Dice Presets:** `0`, multiples of refill rate, and `MAX`.
     * **Next Refill Steps:** `0m`, `15m`, `30m`, `45m`, `60m`.
     * **Free Gift Options:** `Just Claimed (8h)`, `4h Left`, `2h Left`, `Ready Now`.
     * Custom text input fallback.

4. **Settings Screen (`ui/settings/`)**
   * Form inputs for season details, dice parameters, "Just Played" preferences, and notification options.

---

## Phase 6: In-Scope Enhancements

### Objective
Integrate planned core features identified in the specification.

### Tasks

1. **Swipe Gestures (`ui/dashboard/`)**
   * Implement swipe-to-action gestures using Compose `SwipeToDismissBox`:
     * Swipe right on Free Gift Card $\rightarrow$ Mark as claimed.
     * Swipe left on Dice Tracker $\rightarrow$ Execute "Just Played" batch update.

2. **Timeline View (`ui/dashboard/`)**
   * Add a chronological list section showing upcoming events in order:
     1. Next Dice Refill
     2. Free Gift Available
     3. Dice Full Projection
     4. Season End

3. **Notification Quick Actions (`notifications/`)**
   * Add inline action buttons on notifications:
     * **"Claimed"**: Resets corresponding timer from the shade.
     * **"Snooze"**: Delays notification by 15 minutes.

4. **Home Screen Widgets (`ui/widgets/`)**
   * Create Glance AppWidget showing current dice, next refill, gift countdown, and full projection time.

---

## Phase 7: Verification & Quality Assurance

### Testing Strategy

1. **Unit Testing (`app/src/test/`)**
   * Validate calculation pure functions against test inputs.
   * Verify time arithmetic and formatting edge cases.
   * Validate data validation rules and clamping.

2. **UI & Integration Testing (`app/src/androidTest/`)**
   * Verify Quick Update modal applies chip choices correctly.
   * Test "Just Played" action updates state according to active settings.
   * Test Settings persistence across app reloads.

3. **Battery & Performance Audits**
   * Verify screen composables pause countdown updates when stopped or backgrounded.
   * Confirm no background services perform active polling.

---

## Explicitly Out of Scope

The following features will **NOT** be included in this implementation cycle:
* Cloud Synchronization & User Accounts
* Historical Analytics & Statistics
* Dynamic Theme Engines
* Automated In-Game Season Scraping
* iOS Cross-Platform Features / Dynamic Island

```
