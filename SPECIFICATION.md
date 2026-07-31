# Application Specification: GO! Timer for Monopoly Go!

**Platform:** Android (Native Kotlin or Flutter/React Native)

**Target Audience:** *Monopoly Go!* casual and competitive players

**Primary Objective:** Provide automated, low-friction tracking and notifications for time-sensitive in-game events, resource refills, and seasonal deadlines.

---

## 1. System Overview & Core Features

**GO! Timer** is a utility application that acts as a accurate, offline-capable event tracker for *Monopoly Go!*. Instead of manually setting standard alarm clocks, players use quick-action buttons and pre-configured presets to track dice refills, store free gifts, and season deadlines.

### Key Capabilities

* **Season End Countdown:** Prominent hero banner tracking remaining days, hours, and minutes in the active album season.
* **Dice Refill & Full-Dice Projection:** Real-time tracking of hourly dice gains alongside a dynamic "Full Dice" projection timer.
* **Store Free Gift Tracker:** 8-hour countdown timer for the recurring store reward.
* **Quick-Update Modal with Chips/Presets:** Fast-entry sheet for updating game state without typing.
* **One-Tap "Just Played" Action:** Global reset button to update multiple timers simultaneously based on user settings.

---

## 2. User Interface (UI) Architecture

```
+---------------------------------------------------+
|               GO! Timer Header                    |
| [ UPDATE ]                         [ Settings ⚙️ ]|
+---------------------------------------------------+
|                                                   |
|  🏆 SEASON COUNTDOWN BANNER                      |
|  "Monopoly Origins" - Ends in: 14d 06h 22m        |
|                                                   |
+---------------------------------------------------+
|                                                   |
|  🎲 DICE REFILL TRACKER                           |
|  Current Dice: 32 / 80                            |
|  Next Refill (+10): 24m 15s                       |
|  -----------------------------------------------  |
|  ⏱️ Fully Refilled In: 4h 24m (At 08:30 PM)       |
|                                                   |
+---------------------------------------------------+
|                                                   |
|  🎁 FREE GIFT TRACKER                             |
|  Status: Claimable in 02h 15m                     |
|  [ Claimed Just Now ]                             |
|                                                   |
+---------------------------------------------------+
|                                                   |
|  ⚡ QUICK ACTION                                  |
|  [   ⚡ JUST PLAYED (0 Dice / Timers Reset)   ]   |
|                                                   |
+---------------------------------------------------+

```

---

## 3. Detailed Screen & Feature Specifications

### 3.1 App Header

* **App Title:** "GO! Timer"
* **Action Buttons:**
* `[ UPDATE ]`: Launches the **Quick Update Sheet**.
* `[ ⚙️ Settings ]`: Launches the **Settings Window**.



---

### 3.2 Main Dashboard Components

#### A. Season Countdown Banner (Top Priority UI)

* **Visual Style:** High-contrast, dynamic color accent at the top of the dashboard.
* **Data Displayed:**
* Active Season Name (e.g., *"Monopoly Origins"*).
* Large digital timer formatting: `DDd HHm SSs`.


* **Behavior:** Updates every second via local system clock comparison against configured season end time.

#### B. Dice Tracker & Projection Card

* **Primary Display:**
* Current Dice vs. Max Dice (e.g., `32 / 80`).
* Circular or linear progress bar representing percent to max capacity.
* Countdown timer to the next immediate hourly refill.


* **Secondary Projection Timer ("Dice Full" Projection):**
* Calculates exact time remaining until total dice capacity is reached ($D_{max}$).
* Displays estimated clock time for full status (e.g., *"Full at 8:30 PM"*).
* Automatically pauses/disables when `Current Dice >= Max Dice`.



#### C. Free Gift Tracker Card

* **Primary Display:**
* Status text: **READY TO CLAIM** (Highlight color) or Countdown (`05h 42m 10s`).


* **Quick Action Button:**
* `[ Claimed Just Now ]`: Immediately resets the 8-hour timer without opening the full Update modal.



#### D. "Just Played" Floating/Sticky Action Bar

* **Primary Button:** Large, easy-to-tap button at the bottom of the main screen.
* **Functionality:** Executes a configurable batch update for common post-session states (e.g., zeroing out dice, resetting refill timer to 60 minutes, resetting Free Gift timer to 8 hours).

---

## 4. Quick Update Modal ("UPDATE" Button)

Designed to eliminate manual keyboard input through context-aware chip/preset selections.

```
+---------------------------------------------------+
| Quick Update                                   [X]|
+---------------------------------------------------+
| Current Dice Count:                               |
| [ 0 ]  [ 10 ]  [ 20 ]  [ 30 ]  [ 50 ]  [ MAX ]    |
| Custom Input: [ ___ ]                             |
|                                                   |
| Minutes until Next Refill:                        |
| [ 0m ]  [ 15m ]  [ 30m ]  [ 45m ]  [ 60m ]         |
|                                                   |
| Free Gift Status:                                 |
| [ Just Claimed (8h) ]  [ 4h Left ]  [ Ready Now ] |
|                                                   |
|                     [ SAVE ]                      |
+---------------------------------------------------+

```

### Input Presets Logic

1. **Current Dice Count:**
* Dynamic preset chips: `0`, multiples of the configured *Hourly Refill Rate*, and `MAX`.


2. **Next Refill Countdown:**
* Step chips: `0m`, `15m`, `30m`, `45m`, `60m`.


3. **Free Gift Status:**
* Quick chips: `Just Claimed (8h)`, `4h Remaining`, `2h Remaining`, `Ready Now (0m)`.



---

## 5. Settings Window

Stores persistent preferences using Android `SharedPreferences` or `DataStore`.

| Setting Name | Input Type | Default Value | Description |
| --- | --- | --- | --- |
| **Season Name** | Text Field | *"Current Season"* | Name displayed on the hero banner. |
| **Season End Date & Time** | Date/Time Picker | Current Date + 30 Days | Target timestamp for season countdown. |
| **Max Dice Capacity** | Numeric Input | `80` | Cap on free dice accrual. |
| **Hourly Refill Count** | Numeric Input | `10` | Number of dice generated per hour. |
| **Just Played: Set Dice to 0** | Checkbox | `Enabled (TRUE)` | If true, pressing "Just Played" sets current dice to 0. |
| **Just Played: Reset Refill Timer** | Checkbox | `Enabled (TRUE)` | If true, resets current hourly timer to 60m. |
| **Just Played: Reset Free Gift** | Checkbox | `Disabled (FALSE)` | If true, resets Free Gift countdown to 8h. |
| **Push Notifications** | Toggle | `Enabled (TRUE)` | Enables Android system notifications. |
| **Notification Lead Time** | Dropdown | `5 minutes before` | Sends alerts slightly before event completion. |

---

## 6. Logic & Calculation Rules

### 6.1 Projected Time to Max Dice Formula

$$\text{Remaining Dice Needed} = \text{Max Dice} - \text{Current Dice}$$

$$\text{Full Cycles Needed} = \left\lceil \frac{\text{Remaining Dice Needed}}{\text{Hourly Refill Rate}} \right\rceil - 1$$

$$\text{Total Minutes to Full} = \text{Minutes to Next Refill} + (\text{Full Cycles Needed} \times 60)$$

> **Example:**
> * **Max Dice:** 80 | **Current Dice:** 15 | **Refill Rate:** 10/hr | **Next Refill:** 20 mins
> * Remaining Dice Needed = $80 - 15 = 65$ dice.
> * Next refill gives 10 dice in 20 mins $\rightarrow$ 55 dice needed after.
> * 6 full cycles needed after first refill ($6 \times 60 = 360$ mins).
> * **Total Minutes:** $20 + 360 = 380 \text{ mins}$ (**6 hours and 20 minutes**).
> 
> 

---

## 7. Android System Integration & Notifications

To ensure timers run reliably in the background without draining the battery, the app utilizes standard Android scheduling APIs:

* **AlarmManager / WorkManager:** Schedules exact background alarms for:
1. *Dice Capacity Reached:* Triggers when current dice equal max capacity.
2. *Free Gift Available:* Triggers when 8-hour gift timer expires.


* **Notification Channels:**
* `channel_dice_alerts`: High-priority notifications for full dice capacity.
* `channel_gift_alerts`: Medium-priority notifications for Free Gift claims.
* `channel_season_alerts`: Reminder alerts 24 hours and 1 hour before season ends.


* **Persistence:** All timestamps are saved in epoch milliseconds (`System.currentTimeMillis()`). If the device reboots, an `ON_BOOT_COMPLETED` BroadcastReceiver recalculates remaining times and restores all system notifications.


---

## 8. Planned Enhancements (In Scope)

### Timeline View
A dashboard section listing upcoming events in chronological order:
- Next Dice Refill
- Free Gift Available
- Dice Full Projection
- Season End

### Home Screen Widgets
Small and medium Android widgets showing dice count, next refill, gift timer, and full-dice projection.

### Notification Quick Actions
Notifications include:
- **Claimed** (reset timer)
- **Snooze**

### Swipe Gestures
- Swipe right on the Free Gift card to mark it claimed.
- Swipe left on the Dice Tracker to execute the configurable **Just Played** action.

---

## 9. Future Ideas (Out of Scope)

- Custom Event Timers
- Session History
- Smart Notification Suggestions
- Statistics Dashboard
- Automatic Season Updates
- iOS Live Activities / Dynamic Island
- Themes
- Cloud Backup / Sync
- Presets
- Achievement System
- Timer Accuracy Indicator
- Power User Mode

