package me.josh.axiom.api.models

import kotlinx.serialization.Serializable

/**
 * Leaderboard and score-related API models.
 */

@Serializable
data class LeaderboardEntry(
    val _id: String? = null,
    val playerId: String,
    val playerName: String,
    val score: Int,
    val kills: Int,
    val survivalTime: Double
)

@Serializable
data class ScoreSubmission(
    val kills: Int,
    val survivalTime: Float,
    val score: Int
)
