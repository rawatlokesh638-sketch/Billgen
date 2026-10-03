package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class BillGenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("BillGenApplication", "FirebaseApp initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("BillGenApplication", "FirebaseApp init handled gracefully: ${e.message}")
        }
    }
}
