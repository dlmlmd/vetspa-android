package com.vetspa.nativeapp.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val ok: Boolean,
    val error: String? = null,
    val data: T? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val ok: Boolean,
    val user: User? = null,
    val error: String? = null
)

data class User(
    val id: Int,
    val username: String,
    val fullname: String?,
    val role: String,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("profile_code") val profileCode: String? = null
)

data class Booking(
    val id: Int,
    @SerializedName("staff_id") val staffId: Int,
    @SerializedName("bed_id") val bedId: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    val status: String,
    val note: String? = null,
    @SerializedName("staff_name") val staffName: String? = null,
    @SerializedName("package_name") val packageName: String? = null,
    @SerializedName("customer_name") val customerName: String? = null
)

data class Staff(
    val id: Int,
    val name: String,
    val position: String? = null,
    val phone: String? = null,
    val status: String? = null,
    val avatar: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    @SerializedName("is_frequent") val isFrequent: Boolean = false
)

data class Package(
    val id: Int,
    val name: String,
    val price: Double,
    @SerializedName("total_sessions") val totalSessions: Int,
    @SerializedName("duration_per_session") val durationPerSession: Int,
    val summary: String? = null,
    val cover: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class FcmTokenRequest(
    val token: String,
    val platform: String = "android"
)
