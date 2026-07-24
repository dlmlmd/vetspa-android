package com.vetspa.nativeapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.vetspa.nativeapp.data.api.MyCookieJar

class VetSpaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MyCookieJar.init(this)
    }
}
