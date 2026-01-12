package me.josh.axiom.api.models

import kotlinx.serialization.Serializable

/**
 * Authentication-related API models.
 */

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String,
    val username: String
)

@Serializable
data class SignInRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val user: UserData,
    val session: SessionData? = null
)

@Serializable
data class UserData(
    val id: String,
    val username: String,
    val totalKills: Int? = null,
    val totalDeaths: Int? = null,
    val highestScore: Int? = null
)

@Serializable
data class SessionData(
    val token: String,
    val expiresAt: Long? = null
)
