package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class BillGenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val app = FirebaseApp.initializeApp(this)
            Log.d("BillGenApplication", "FirebaseApp initialized successfully: ${app?.name}")
        } catch (e: Exception) {
            Log.e("BillGenApplication", "FirebaseApp init exception: ${e.message}")
        }
    }
}
