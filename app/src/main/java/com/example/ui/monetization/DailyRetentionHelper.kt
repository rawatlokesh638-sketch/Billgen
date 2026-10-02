package com.example.ui.monetization

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyRetentionHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("billgen_retention_prefs", Context.MODE_PRIVATE)

    var isProUser: Boolean
        get() = prefs.getBoolean("is_pro_user", false)
        set(value) = prefs.edit().putBoolean("is_pro_user", value).apply()

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
}
