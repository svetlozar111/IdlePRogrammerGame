package com.example.idleprogrammergame.game_logic

/**
 * Enum representing different ad reward types.
 * Currently has one option: +2x coins for 5 minutes.
 * Can be expanded with more options in the future.
 */
enum class AdRewardType(
    val multiplier: Double,
    val durationMinutes: Int,
    val displayText: String
) {
    DOUBLE_COINS_5_MIN(
        multiplier = 2.0,
        durationMinutes = 5,
        displayText = "+2x Coins"
    )
}
