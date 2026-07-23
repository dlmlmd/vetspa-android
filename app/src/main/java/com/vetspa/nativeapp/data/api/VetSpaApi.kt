package com.vetspa.nativeapp.data.api

import com.vetspa.nativeapp.BuildConfig
import com.vetspa.nativeapp.data.model.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface VetSpaApi {

    // Auth
    @POST("auth_api.php?action=login")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<ResponseBody>

    @GET("auth_api.php?action=me")
    suspend fun me(): Response<User>

    @POST("auth_api.php?action=logout")
    suspend fun logout(): Response<ApiResponse<Any>>

    // Staff
    @GET("staff_api.php?action=available")
    suspend fun getAvailableStaff(
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<ApiResponse<List<Staff>>>

    // Bookings
    @GET("booking_api.php?action=my")
    suspend fun getMyBookings(
        @Query("date") date: String? = null
    ): Response<ApiResponse<List<Booking>>>

    @POST("booking_api.php?action=create")
    @FormUrlEncoded
    suspend fun createBooking(
        @Field("staff_id") staffId: Int,
        @Field("bed_id") bedId: Int,
        @Field("start_time") startTime: String,
        @Field("end_time") endTime: String,
        @Field("package_id") packageId: Int? = null,
        @Field("note") note: String? = null
    ): Response<ApiResponse<Booking>>

    // Packages
    @GET("packages_api.php?action=list")
    suspend fun getPackages(): Response<ApiResponse<List<Package>>>

    @GET("packages_api.php?action=my")
    suspend fun getMyPackages(): Response<ApiResponse<List<UserPackage>>>

    // FCM
    @POST("fcm_api.php?action=register")
    suspend fun registerFcmToken(@Body body: FcmTokenRequest): Response<ApiResponse<Any>>

    // Notifications
    @GET("notification_api.php?action=poll")
    suspend fun pollNotifications(): Response<ApiResponse<NotificationResponse>>
}

data class UserPackage(
    val id: Int,
    @SerializedName("package_id") val packageId: Int,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("sessions_remaining") val sessionsRemaining: Int,
    @SerializedName("sessions") val sessions: Int,
    val status: String
)

data class NotificationResponse(
    val unread: Int,
    val notifications: List<Notification>? = null
)

data class Notification(
    val id: Int,
    val title: String,
    val message: String
)

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(logging)
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
