package com.gotimer.viewmodel

import com.gotimer.model.UserPreferences

/**
 * Free Gift choices offered as quick chips in the update sheet.
 *
 * @property hoursUntilClaimable Offset from now when the gift becomes
 * claimable; zero means immediately.
 */
enum class GiftOption(val hoursUntilClaimable: Int) {
    JUST_CLAIMED(8),
    FOUR_HOURS_LEFT(4),
    TWO_HOURS_LEFT(2),
    READY_NOW(0),
}

/**
 * Immutable state for the quick update sheet.
 *
 * Selections are applied on SAVE only; a null selection leaves that
 * category untouched. No selection at all disables the save action.
 *
 * @property maxDice Current capacity, used to clamp custom dice input.
 * @property dicePresets Generated chips: zero, multiples of the hourly
 * refill rate, and MAX.
 * @property selectedDice Chosen dice chip, or null when untouched.
 * @property refillPresets Fixed `0m..60m` step chips.
 * @property selectedRefillMinutes Chosen refill chip, or null when untouched.
 * @property giftOptions The four fixed gift chips.
 * @property selectedGiftOption Chosen gift chip, or null when untouched.
 */
data class UpdateUiState(
    val maxDice: Int = UserPreferences.DEFAULT_MAX_DICE,
    val dicePresets: List<Int> = emptyList(),
    val selectedDice: Int? = null,
    val refillPresets: List<Int> = REFILL_PRESETS,
    val selectedRefillMinutes: Int? = null,
    val giftOptions: List<GiftOption> = GiftOption.entries,
    val selectedGiftOption: GiftOption? = null,
) {

    /** True once at least one category has a selection. */
    val saveEnabled: Boolean
        get() = selectedDice != null ||
            selectedRefillMinutes != null ||
            selectedGiftOption != null

    companion object {
        /** Fixed refill countdown steps offered by the update sheet. */
        val REFILL_PRESETS: List<Int> = listOf(0, 15, 30, 45, 60)
    }
}
