package com.vetspa.nativeapp.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vetspa.nativeapp.data.api.ApiClient
import com.vetspa.nativeapp.data.model.FcmTokenRequest
import com.vetspa.nativeapp.util.NotifHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token: $token")
        com.vetspa.nativeapp.data.api.MyCookieJar.setFcmToken(token)
        registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Thông báo từ VetSpa"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        NotifHelper.showNotification(
            applicationContext,
            title,
            body,
            System.currentTimeMillis().toInt()
        )
    }

    private fun registerToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.api.registerFcmToken(FcmTokenRequest(token))
                Log.d(TAG, "Token registered: ${resp.code()}")
            } catch (e: Exception) {
                Log.e(TAG, "Register failed: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "FcmService"
    }
}
