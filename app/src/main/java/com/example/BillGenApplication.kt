package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class BillGenApplication : Application() {

    companion object {
        lateinit var instance: BillGenApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initFirebase()
    }

    fun initFirebase(): FirebaseApp? {
        try {
            val existingApps = FirebaseApp.getApps(this)
            if (existingApps.isNotEmpty()) {
                Log.d("BillGenApplication", "FirebaseApp already initialized: ${existingApps[0].name}")
                return FirebaseApp.getInstance()
            }

            // 1. Try standard automatic initialization from resources
            val defaultApp = FirebaseApp.initializeApp(this)
            if (defaultApp != null) {
                Log.d("BillGenApplication", "FirebaseApp initialized from resources: ${defaultApp.name}")
                return defaultApp
            }
        } catch (e: Exception) {
            Log.w("BillGenApplication", "Default FirebaseApp init failed: ${e.message}, trying programmatic fallback...")
        }

        // 2. Guaranteed programmatic fallback with exact FirebaseOptions from google-services.json
        return try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:431299158400:android:70e47043a2d6bb336fa165")
                .setApiKey("AIzaSyDYSZeb4cMTwljnyT086aee5e_cNvMd3Yg")
                .setDatabaseUrl("https://billgen-cc831-default-rtdb.firebaseio.com")
                .setProjectId("billgen-cc831")
                .setStorageBucket("billgen-cc831.firebasestorage.app")
                .setGcmSenderId("431299158400")
                .build()

            val app = FirebaseApp.initializeApp(this, options)
            Log.d("BillGenApplication", "FirebaseApp initialized with explicit options: ${app.name}")
            app
        } catch (e: Exception) {
            Log.e("BillGenApplication", "Explicit FirebaseApp init exception: ${e.message}", e)
            null
        }
    }
}
