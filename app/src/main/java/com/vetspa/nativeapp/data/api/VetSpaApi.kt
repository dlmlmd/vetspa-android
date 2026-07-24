package com.vetspa.nativeapp.data.api

import com.vetspa.nativeapp.BuildConfig
import com.vetspa.nativeapp.data.model.*
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface VetSpaApi {

    // ==================== AUTH ====================
    @POST("android_auth_api.php?action=login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<LoginResponse>

    @GET("android_auth_api.php?action=me")
    suspend fun me(): Response<LoginResponse>

    @POST("android_auth_api.php?action=logout")
    suspend fun logout(): Response<ApiResponse<Any>>

    @POST("android_auth_api.php?action=register_fcm")
    suspend fun registerFcmToken(@Body body: FcmTokenRequest): Response<ApiResponse<Any>>

    // ==================== STAFF ====================
    @GET("android_staff_api.php?action=available")
    suspend fun getAvailableStaff(
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<StaffResponse>

    @GET("android_staff_api.php?action=list")
    suspend fun getStaffList(): Response<StaffListResponse>

    // ==================== BOOKINGS ====================
    @GET("android_booking_api.php?action=my_bookings")
    suspend fun getMyBookings(): Response<MyBookingsResponse>

    @GET("android_booking_api.php?action=available_beds")
    suspend fun getAvailableBeds(
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<AvailableBedsResponse>

    @POST("android_booking_api.php?action=create")
    suspend fun createBooking(@Body body: CreateBookingRequest): Response<CreateBookingResponse>

    @POST("android_booking_api.php?action=cancel")
    suspend fun cancelBooking(@Body body: CancelBookingRequest): Response<ApiResponse<Any>>

    @GET("android_booking_api.php?action=detail")
    suspend fun getBookingDetail(
        @Query("id") id: Int
    ): Response<BookingDetailResponse>

    // ==================== PACKAGES ====================
    @GET("android_packages_api.php?action=list")
    suspend fun getPackages(): Response<PackagesListResponse>

    @GET("android_packages_api.php?action=my")
    suspend fun getMyPackages(): Response<MyPackagesResponse>
}

// ==================== REQUEST MODELS ====================

data class CreateBookingRequest(
    @SerializedName("staff_id") val staffId: Int,
    @SerializedName("bed_id") val bedId: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("package_id") val packageId: Int,
    val note: String? = null
)

data class CancelBookingRequest(
    @SerializedName("booking_id") val bookingId: Int
)

// ==================== RESPONSE MODELS ====================

data class StaffResponse(
    val ok: Boolean,
    val staff: List<Staff>? = null,
    @SerializedName("busy_staff_ids") val busyStaffIds: List<Int>? = null,
    val error: String? = null
)

data class StaffListResponse(
    val ok: Boolean,
    val staff: List<Staff>? = null,
    val error: String? = null
)

data class MyBookingsResponse(
    val ok: Boolean,
    val bookings: List<DateGroup>? = null,
    val error: String? = null
)

data class DateGroup(
    val date: String,
    val items: List<Booking>
)

data class AvailableBedsResponse(
    val ok: Boolean,
    val beds: List<Int>? = null,
    @SerializedName("total_beds") val totalBeds: Int = 4,
    @SerializedName("booked_beds") val bookedBeds: List<Int>? = null,
    val error: String? = null
)

data class CreateBookingResponse(
    val ok: Boolean,
    val booking: Booking? = null,
    val error: String? = null
)

data class BookingDetailResponse(
    val ok: Boolean,
    val booking: Booking? = null,
    val error: String? = null
)

data class PackagesListResponse(
    val ok: Boolean,
    val packages: List<Package>? = null,
    val error: String? = null
)

data class MyPackagesResponse(
    val ok: Boolean,
    val packages: List<UserPackage>? = null,
    val error: String? = null
)

// ==================== API CLIENT ====================

object ApiClient {
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36 VetSpaNative/1.0")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "vi-VN,vi;q=0.9,en;q=0.8")
                .header("Referer", BuildConfig.WEB_APP_URL)
                .header("Sec-Fetch-Site", "same-origin")
                .build())
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(MyCookieJar())
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    val api: VetSpaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(VetSpaApi::class.java)
    }
}
