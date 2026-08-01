package com.gotimer.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gotimer.ui.components.ActionButton
import com.gotimer.ui.components.PresetChip
import com.gotimer.viewmodel.GiftOption
import com.gotimer.viewmodel.UpdateUiState

/**
 * Quick update bottom sheet: preset chips for dice, refill minutes, and Free
 * Gift status, plus a custom dice input. SAVE applies only the categories the
 * user touched.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UpdateSheet(
    uiState: UpdateUiState,
    onSelectDice: (Int) -> Unit,
    onSelectRefillMinutes: (Int) -> Unit,
    onSelectGiftOption: (GiftOption) -> Unit,
    onCustomDiceInput: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var customDiceInput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Quick Update",
                    style = MaterialTheme.typography.headlineSmall,
                )
                TextButton(onClick = onClearSelection) {
                    Text("Clear")
                }
            }

            SectionLabel("Current Dice Count")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.dicePresets.forEach { preset ->
                    PresetChip(
                        label = if (preset == uiState.maxDice) "MAX" else preset.toString(),
                        selected = uiState.selectedDice == preset,
                        onClick = { onSelectDice(preset) },
                    )
                }
            }
            OutlinedTextField(
                value = customDiceInput,
                onValueChange = { input ->
                    customDiceInput = input
                    onCustomDiceInput(input)
                },
                label = { Text("Custom Input") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Minutes until Next Refill")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.refillPresets.forEach { minutes ->
                    PresetChip(
                        label = "${minutes}m",
                        selected = uiState.selectedRefillMinutes == minutes,
                        onClick = { onSelectRefillMinutes(minutes) },
                    )
                }
            }

            SectionLabel("Free Gift Status")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.giftOptions.forEach { option ->
                    PresetChip(
                        label = when (option) {
                            GiftOption.JUST_CLAIMED -> "Just Claimed (8h)"
                            GiftOption.FOUR_HOURS_LEFT -> "4h Left"
                            GiftOption.TWO_HOURS_LEFT -> "2h Left"
                            GiftOption.READY_NOW -> "Ready Now (0m)"
                        },
                        selected = uiState.selectedGiftOption == option,
                        onClick = { onSelectGiftOption(option) },
                    )
                }
            }

            ActionButton(
                label = "SAVE",
                enabled = uiState.saveEnabled,
                onClick = onSave,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

/**
 * Small section heading inside the update sheet.
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
