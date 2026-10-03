package com.example.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "FirebaseAuth init error: ${e.message}")
            null
        }
    }

    val currentUser: FirebaseUser?
        get() = try { auth?.currentUser } catch (_: Exception) { null }

    val isUserLoggedIn: Boolean
        get() = try { auth?.currentUser != null } catch (_: Exception) { false }

    val currentUserId: String
        get() = try { auth?.currentUser?.uid ?: "guest_merchant" } catch (_: Exception) { "guest_merchant" }

    val userEmail: String
        get() = try { auth?.currentUser?.email ?: "" } catch (_: Exception) { "" }

    val isAnonymous: Boolean
        get() = try { auth?.currentUser?.isAnonymous == true } catch (_: Exception) { false }

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val safeAuth = auth
        if (safeAuth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(try { firebaseAuth.currentUser } catch (_: Exception) { null })
        }
        try {
            safeAuth.addAuthStateListener(listener)
        } catch (e: Exception) {
            trySend(null)
        }
        awaitClose {
            try { safeAuth.removeAuthStateListener(listener) } catch (_: Exception) {}
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val safeAuth = auth ?: return Result.failure(Exception("Authentication service unavailable"))
        return try {
            val res = safeAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = res.user ?: throw Exception("Failed to create user account")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val safeAuth = auth ?: return Result.failure(Exception("Authentication service unavailable"))
        return try {
            val res = safeAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = res.user ?: throw Exception("Failed to sign in")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        val safeAuth = auth ?: return Result.failure(Exception("Authentication service unavailable"))
        return try {
            val res = safeAuth.signInAnonymously().await()
            val user = res.user ?: throw Exception("Guest login failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val safeAuth = auth ?: return Result.failure(Exception("Authentication service unavailable"))
        return try {
            safeAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try { auth?.signOut() } catch (_: Exception) {}
    }
}
