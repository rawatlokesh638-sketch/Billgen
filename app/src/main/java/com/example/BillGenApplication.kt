package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class BillGenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                Log.d("BillGenApplication", "FirebaseApp initialized successfully: ${app?.name}")
            } else {
                Log.d("BillGenApplication", "FirebaseApp already initialized")
            }
        } catch (e: Exception) {
            Log.e("BillGenApplication", "FirebaseApp init exception: ${e.message}", e)
        }
    }
}
