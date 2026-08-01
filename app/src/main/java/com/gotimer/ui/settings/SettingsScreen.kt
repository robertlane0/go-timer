package com.gotimer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gotimer.model.UserPreferences
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Settings window: season details, dice parameters, "Just Played" flags, and
 * notification options. Edits are staged locally and persisted only when the
 * user taps SAVE; numeric ranges are clamped by the repository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserPreferences,
    onSave: (UserPreferences) -> Unit,
    onBack: () -> Unit,
) {
    var seasonName by remember(settings) { mutableStateOf(settings.seasonName) }
    var seasonEndEpoch by remember(settings) { mutableLongStateOf(settings.seasonEndEpoch) }
    var maxDiceText by remember(settings) { mutableStateOf(settings.maxDice.toString()) }
    var refillRateText by remember(settings) { mutableStateOf(settings.hourlyRefillRate.toString()) }
    var justPlayedZeroDice by remember(settings) { mutableStateOf(settings.justPlayedZeroDice) }
    var justPlayedResetRefill by remember(settings) { mutableStateOf(settings.justPlayedResetRefill) }
    var justPlayedResetGift by remember(settings) { mutableStateOf(settings.justPlayedResetGift) }
    var notificationsEnabled by remember(settings) { mutableStateOf(settings.notificationsEnabled) }
    var leadMinutes by remember(settings) { mutableIntStateOf(settings.notificationLeadMinutes) }
    var pendingDateMillis by remember { mutableLongStateOf(NO_DATE) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                UserPreferences(
                                    seasonName = seasonName,
                                    seasonEndEpoch = seasonEndEpoch,
                                    maxDice = maxDiceText.toIntOrNull() ?: settings.maxDice,
                                    hourlyRefillRate = refillRateText.toIntOrNull() ?: settings.hourlyRefillRate,
                                    justPlayedZeroDice = justPlayedZeroDice,
                                    justPlayedResetRefill = justPlayedResetRefill,
                                    justPlayedResetGift = justPlayedResetGift,
                                    notificationsEnabled = notificationsEnabled,
                                    notificationLeadMinutes = leadMinutes,
                                ),
                            )
                        },
                    ) {
                        Text("SAVE")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("Season") {
                OutlinedTextField(
                    value = seasonName,
                    onValueChange = { seasonName = it },
                    label = { Text("Season Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Season Ends: ${formatSeasonEnd(seasonEndEpoch)}")
                }
            }

            Section("Dice") {
                OutlinedTextField(
                    value = maxDiceText,
                    onValueChange = { maxDiceText = it.filter(Char::isDigit) },
                    label = { Text("Max Dice Capacity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = refillRateText,
                    onValueChange = { refillRateText = it.filter(Char::isDigit) },
                    label = { Text("Hourly Refill Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Just Played") {
                SettingSwitchRow(
                    label = "Set Dice to 0",
                    checked = justPlayedZeroDice,
                    onCheckedChange = { justPlayedZeroDice = it },
                )
                SettingSwitchRow(
                    label = "Reset Refill Timer",
                    checked = justPlayedResetRefill,
                    onCheckedChange = { justPlayedResetRefill = it },
                )
                SettingSwitchRow(
                    label = "Reset Free Gift",
                    checked = justPlayedResetGift,
                    onCheckedChange = { justPlayedResetGift = it },
                )
            }

            Section("Notifications") {
                SettingSwitchRow(
                    label = "Push Notifications",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                )
                LeadTimeDropdown(
                    leadMinutes = leadMinutes,
                    onLeadMinutesChange = { leadMinutes = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = seasonEndEpoch.takeIf { it > 0 },
            initialDisplayedMonthMillis = seasonEndEpoch.takeIf { it > 0 },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            pendingDateMillis = dateMillis
                            showDatePicker = false
                            showTimePicker = true
                        }
                    },
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hourOf(seasonEndEpoch),
            initialMinute = minuteOf(seasonEndEpoch),
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Season End Time") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pendingDateMillis != NO_DATE) {
                            seasonEndEpoch = combineDateAndTime(
                                pendingDateMillis,
                                timePickerState.hour,
                                timePickerState.minute,
                            )
                        }
                        pendingDateMillis = NO_DATE
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

/**
 * Section container with a heading and its rows.
 */
@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

/**
 * Labeled switch row for boolean settings.
 */
@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * Dropdown of notification lead times.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeadTimeDropdown(
    leadMinutes: Int,
    onLeadMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = leadTimeLabel(leadMinutes),
            onValueChange = {},
            readOnly = true,
            label = { Text("Notification Lead Time") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LEAD_TIME_OPTIONS.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(leadTimeLabel(minutes)) },
                    onClick = {
                        onLeadMinutesChange(minutes)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun leadTimeLabel(minutes: Int): String = when (minutes) {
    0 -> "At the moment (0m)"
    5 -> "5 minutes before"
    10 -> "10 minutes before"
    15 -> "15 minutes before"
    30 -> "30 minutes before"
    60 -> "1 hour before"
    else -> "$minutes minutes before"
}

private fun formatSeasonEnd(epoch: Long): String {
    if (epoch <= 0) return "Not set"
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault())
        .format(SEASON_END_FORMATTER)
}

private fun hourOf(epoch: Long): Int =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch.coerceAtLeast(0)), ZoneId.systemDefault()).hour

private fun minuteOf(epoch: Long): Int =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch.coerceAtLeast(0)), ZoneId.systemDefault()).minute

private fun combineDateAndTime(dateMillis: Long, hour: Int, minute: Int): Long {
    // DatePickerState.selectedDateMillis is documented to be UTC midnight for
    // the selected calendar day, so the date must be read back in UTC rather
    // than the local zone. Reading it in the local zone would shift the date
    // back by a day for every time zone behind UTC (e.g. all of the Americas).
    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return ZonedDateTime.of(date, LocalTime.of(hour, minute), ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private val SEASON_END_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

private val LEAD_TIME_OPTIONS: List<Int> = listOf(0, 5, 10, 15, 30, 60)

private const val NO_DATE = 0L
