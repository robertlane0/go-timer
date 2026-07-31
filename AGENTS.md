# AGENTS.md

## Project Overview

**Project Name:** GO! Timer

GO! Timer is an Android application that provides offline-capable timers, projections, and notifications for Monopoly GO! players. The application tracks:

- Season countdown
- Dice refill progress
- Projected full dice time
- Free Gift availability
- Quick update actions
- Background notifications

The implementation must follow the product specification and prioritize reliability, low battery usage, and a fast UI. :contentReference[oaicite:0]{index=0}

---

# Core Principles

1. **Offline First**
   - All timers work without internet.
   - Never require a backend.
   - Store all state locally.

2. **Time is the Source of Truth**
   - Never decrement counters every second in storage.
   - Store timestamps.
   - Calculate remaining time from `System.currentTimeMillis()`.

3. **Reactive UI**
   - UI reflects current state automatically.
   - No manual refreshes.

4. **Battery Efficiency**
   - Avoid unnecessary wakeups.
   - Use WorkManager/AlarmManager only for notifications.
   - UI timers update only while visible.

5. **Simple Architecture**
   - Keep business logic independent of UI.
   - UI should not perform calculations.

---

# Recommended Tech Stack

## Preferred

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Kotlin Coroutines
- Flow / StateFlow
- DataStore Preferences
- WorkManager
- AlarmManager (exact alarms when appropriate)

Alternative cross-platform implementations should preserve the same architecture.

---

# Project Structure

```

app/
ui/
dashboard/
settings/
update/
widgets/

```
components/

model/

repository/

datastore/

notifications/

scheduler/

calculations/

viewmodel/

util/
```

````

---

# Architecture

UI

↓

ViewModel

↓

Repository

↓

DataStore

↓

Notification Scheduler

Business calculations belong inside the calculation layer, not inside composables.

---

# State Model

Maintain application state similar to:

```kotlin
AppState
    seasonName
    seasonEndEpoch

    currentDice
    maxDice
    refillRatePerHour
    minutesToNextRefill

    freeGiftAvailableEpoch

    settings
````

Only timestamps should be persisted whenever possible.

---

# Major Components

## Dashboard

Responsible for displaying:

* Season countdown
* Dice tracker
* Full projection
* Gift timer
* Just Played action

Must never perform calculations itself.

---

## Settings

Responsible for:

* Reading DataStore
* Writing DataStore
* Validation

No timer logic.

---

## Update Sheet

Responsible only for editing state.

No calculations except basic validation.

---

## Scheduler

Responsible for:

* Notification scheduling
* Boot recovery
* Notification updates
* Cancel/reschedule

No UI.

---

## Calculation Engine

Single source of truth for:

* Countdown formatting
* Dice projections
* Remaining minutes
* Next refill
* Progress percentage

Everything should be pure functions.

---

# Calculation Rules

Never duplicate formulas.

Create dedicated utility methods.

Examples:

```
calculateRemainingDice()

calculateMinutesToFull()

calculateProjectionTime()

calculateGiftRemaining()

calculateSeasonRemaining()

calculateProgress()
```

The projection logic must implement the specification's formulas exactly. 

---

# Persistence

Persist:

* Dice count
* Max dice
* Refill rate
* Next refill timestamp
* Gift timestamp
* Season timestamp
* Settings

Avoid persisting derived values.

Derived values should always be recalculated.

---

# Notifications

Notification scheduling should be centralized.

Supported notification categories:

* Dice Full
* Free Gift Ready
* Season Reminder

Notification channels should match the specification. 

---

# ViewModels

One ViewModel per feature.

Example:

DashboardViewModel

SettingsViewModel

UpdateViewModel

No ViewModel should know implementation details of another.

Shared repositories are acceptable.

---

# UI Guidelines

Compose screens should remain lightweight.

Prefer:

```
DashboardScreen

    ->
DashboardContent

    ->
Reusable Cards

    ->
Small Components
```

Avoid composables larger than approximately 250 lines.

---

# Reusable Components

Examples:

* CountdownCard
* ProgressCard
* HeroBanner
* ActionButton
* SettingRow
* TimeChip
* DiceChip
* ProgressIndicator

Components should remain stateless whenever possible.

---

# Error Handling

Never crash on invalid user input.

Clamp values.

Examples:

Current Dice

```
0 <= dice <= maxDice
```

Minutes

```
0 <= minutes <= 60
```

Reject impossible timestamps.

---

# Testing

Implement:

## Unit Tests

* Projection calculations
* Countdown formatting
* Progress calculations
* Notification scheduling
* Validation

## UI Tests

* Dashboard rendering
* Update modal
* Settings
* Quick actions

---

# Performance Goals

Dashboard should render smoothly.

Avoid recomputing expensive calculations every recomposition.

Use:

* remember
* derivedStateOf
* StateFlow

appropriately.

---

# Coding Style

* Prefer immutable models.
* Prefer data classes.
* Keep functions short.
* Avoid nested conditionals.
* Avoid magic numbers.
* Use descriptive names.
* One responsibility per class.

---

# Naming

Classes

```
DiceRepository

NotificationScheduler

ProjectionCalculator
```

Methods

```
calculateDiceProjection()

saveSettings()

scheduleGiftNotification()
```

Variables

```
currentDice

remainingMinutes

seasonEndEpoch
```

Avoid abbreviations.

---

# Documentation

Every public class should contain KDoc.

Complex calculations should explain:

* inputs
* outputs
* assumptions

---

# Future Expansion

Design the architecture so these planned features can be added without significant refactoring:

* Timeline View
* Home Screen Widgets
* Notification Quick Actions
* Swipe Gestures

These enhancements are explicitly identified as in scope for future iterations. 

---

# Out of Scope

Do not implement unless explicitly requested:

* Cloud Sync
* Statistics
* Themes
* Session History
* Automatic Season Updates
* Achievements
* iOS Features

These are listed as future ideas rather than current requirements. 

---

# Agent Expectations

When contributing code:

1. Preserve MVVM boundaries.
2. Do not place business logic inside composables.
3. Reuse calculation utilities.
4. Write unit tests for new logic.
5. Keep UI responsive.
6. Favor readability over cleverness.
7. Keep features modular.
8. Minimize battery usage.
9. Do not introduce unnecessary dependencies.
10. Follow Material 3 design conventions unless directed otherwise.
