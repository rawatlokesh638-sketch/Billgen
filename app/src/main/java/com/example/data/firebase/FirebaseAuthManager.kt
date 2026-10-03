package com.example.data.firebase

import android.util.Log
import com.example.BillGenApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {

    private var lastInitError: String? = null

    private fun getAuth(): FirebaseAuth? {
        try {
            val auth = FirebaseAuth.getInstance()
            lastInitError = null
            return auth
        } catch (e: Exception) {
            Log.w("FirebaseAuthManager", "Direct FirebaseAuth.getInstance() failed: ${e.message}")
            lastInitError = e.message
        }

        // Guaranteed fallback via BillGenApplication
        return try {
            val app = try {
                BillGenApplication.instance.initFirebase()
            } catch (appErr: Exception) {
                Log.w("FirebaseAuthManager", "BillGenApplication.instance not ready: ${appErr.message}")
                null
            }

            if (app != null) {
                val auth = FirebaseAuth.getInstance(app)
                lastInitError = null
                auth
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Fallback FirebaseAuth init error: ${e.message}", e)
            lastInitError = e.message
            null
        }
    }

    val currentUser: FirebaseUser?
        get() = try { getAuth()?.currentUser } catch (_: Exception) { null }

    val isUserLoggedIn: Boolean
        get() = try { getAuth()?.currentUser != null } catch (_: Exception) { false }

    val currentUserId: String
        get() = try { getAuth()?.currentUser?.uid ?: "guest_merchant" } catch (_: Exception) { "guest_merchant" }

    val userEmail: String
        get() = try { getAuth()?.currentUser?.email ?: "" } catch (_: Exception) { "" }

    val isAnonymous: Boolean
        get() = try { getAuth()?.currentUser?.isAnonymous == true } catch (_: Exception) { false }

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(try { firebaseAuth.currentUser } catch (_: Exception) { null })
        }
        val safeAuth = getAuth()
        if (safeAuth != null) {
            try {
                safeAuth.addAuthStateListener(listener)
            } catch (e: Exception) {
                trySend(null)
            }
        } else {
            trySend(null)
        }
        awaitClose {
            try { getAuth()?.removeAuthStateListener(listener) } catch (_: Exception) {}
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val safeAuth = getAuth() ?: return Result.failure(
            Exception(lastInitError?.let { "Firebase Auth Error: $it" } ?: "Firebase Auth initialization failed. Please check internet connection.")
        )
        return try {
            val res = safeAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = res.user ?: throw Exception("Failed to create user account")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val safeAuth = getAuth() ?: return Result.failure(
            Exception(lastInitError?.let { "Firebase Auth Error: $it" } ?: "Firebase Auth initialization failed. Please check internet connection.")
        )
        return try {
            val res = safeAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = res.user ?: throw Exception("Failed to sign in")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        val safeAuth = getAuth() ?: return Result.failure(Exception("Guest mode active locally"))
        return try {
            val res = safeAuth.signInAnonymously().await()
            val user = res.user ?: throw Exception("Guest login active")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val safeAuth = getAuth() ?: return Result.failure(
            Exception(lastInitError?.let { "Firebase Auth Error: $it" } ?: "Firebase Auth service unavailable")
        )
        return try {
            safeAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try { getAuth()?.signOut() } catch (_: Exception) {}
    }
}
