package com.example.ui.monetization

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyRetentionHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("billgen_retention_prefs", Context.MODE_PRIVATE)

    var userPlanTier: String
        get() {
            val tier = prefs.getString("user_plan_tier", "FREE") ?: "FREE"
            val expiresAt = prefs.getLong("plan_expires_at", 0L)
            if (expiresAt > 0L && System.currentTimeMillis() > expiresAt) {
                return "FREE"
            }
            return tier
        }
        set(value) = prefs.edit().putString("user_plan_tier", value).apply()

    var planExpiresAt: Long
        get() = prefs.getLong("plan_expires_at", 0L)
        set(value) = prefs.edit().putLong("plan_expires_at", value).apply()

    var isProUser: Boolean
        get() = userPlanTier != "FREE"
        set(value) {
            if (value && userPlanTier == "FREE") {
                userPlanTier = "PRO"
            } else if (!value) {
                userPlanTier = "FREE"
            }
        }

    fun isProPlusOrHigher(): Boolean {
        return userPlanTier == "PRO_PLUS" || userPlanTier == "PREMIUM"
    }

    fun isPremium(): Boolean {
        return userPlanTier == "PREMIUM"
    }

    var aiScanCredits: Int
        get() = prefs.getInt("ai_scan_credits", 10)
        set(value) = prefs.edit().putInt("ai_scan_credits", value.coerceAtLeast(0)).apply()

    var billingStreak: Int
        get() = prefs.getInt("billing_streak", 1)
        private set(value) = prefs.edit().putInt("billing_streak", value).apply()

    fun recordDailyActivity(): Int {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDateStr = prefs.getString("last_active_date", "") ?: ""

        if (lastDateStr == todayStr) return billingStreak

        // Reset scan credits to 10 daily for Free users on a new day
        if (userPlanTier == "FREE") {
            aiScanCredits = 10
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            if (lastDateStr.isNotBlank()) {
                val lastDate = sdf.parse(lastDateStr)
                val todayDate = sdf.parse(todayStr)
                if (lastDate != null && todayDate != null) {
                    val diffDays = (todayDate.time - lastDate.time) / (1000 * 60 * 60 * 24)
                    if (diffDays == 1L) {
                        billingStreak += 1
                    } else if (diffDays > 1L) {
                        billingStreak = 1
                    }
                }
            } else {
                billingStreak = 1
            }
        } catch (e: Exception) {
            billingStreak = 1
        }

        prefs.edit().putString("last_active_date", todayStr).apply()
        return billingStreak
    }

    fun consumeAiScanCredit(): Boolean {
        if (isPremium()) return true
        if (isProPlusOrHigher()) return true
        if (isProUser) return true
        val current = aiScanCredits
        if (current > 0) {
            aiScanCredits = current - 1
            return true
        }
        return false
    }

    fun addRewardCredits(credits: Int = 5) {
        aiScanCredits += credits
    }

    fun activateTier(tier: String, durationDays: Int) {
        userPlanTier = tier
        val expirationMillis = System.currentTimeMillis() + (durationDays.toLong() * 24 * 60 * 60 * 1000)
        planExpiresAt = expirationMillis
    }
}
