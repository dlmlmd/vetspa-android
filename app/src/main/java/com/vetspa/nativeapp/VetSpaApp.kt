package com.vetspa.nativeapp

import android.app.Application
import com.google.firebase.FirebaseApp

class VetSpaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
