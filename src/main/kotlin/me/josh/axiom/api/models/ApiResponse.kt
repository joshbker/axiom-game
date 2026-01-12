package me.josh.axiom.api.models

import kotlinx.serialization.Serializable

/**
 * Generic wrapper for API responses.
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)
