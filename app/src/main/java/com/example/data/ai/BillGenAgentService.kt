package com.example.data.ai

import com.example.data.model.InvoiceEntity
import com.example.data.model.LineItem
import com.example.data.model.ProductEntity
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AgentAction {
    data class CreateInvoice(val customerName: String, val amount: Double, val itemName: String) : AgentAction()
    data class AddUdhaar(val customerName: String, val customerPhone: String, val amount: Double, val notes: String) : AgentAction()
    data class AddProduct(val name: String, val price: Double, val stock: Int) : AgentAction()
    data class SettleDebt(val customerName: String) : AgentAction()
    data class AnswerOnly(val replyText: String) : AgentAction()
}

data class AgentResponse(
    val replyText: String,
    val executedAction: AgentAction? = null,
    val actionSummary: String? = null
)

class BillGenAgentService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun cleanFormatting(text: String): String {
        return text
            .replace("**", "") // Remove raw double asterisks
            .replace(Regex("""(?m)^\s*[\*\-]\s+"""), "• ") // Standardize bullet points
            .replace("*", "") // Remove all stray single asterisks
            .replace(Regex("""#{1,6}\s*"""), "") // Remove markdown headers
            .replace("`", "") // Remove code ticks
            .replace("###", "")
            .replace("##", "")
            .replace(Regex("""\n{3,}"""), "\n\n") // Collapse excess blank lines
            .trim()
    }

    suspend fun processQuery(
        userMessage: String,
        storeName: String,
        todaySales: Double,
        totalSales: Double,
        pendingUdhaar: Double,
        invoices: List<InvoiceEntity>,
        products: List<ProductEntity>
    ): AgentResponse = withContext(Dispatchers.IO) {
        val trimmed = userMessage.trim()

        // 1. Rule-based Fast Action & Analytics Handlers
        val lower = trimmed.lowercase()

        // Check if user is asking for today's sales
        if (lower.contains("aaj ki sale") || lower.contains("today sales") || lower.contains("todays sale") || lower.contains("aaj kitni sale") || lower.contains("today's sale")) {
            val count = invoices.filter { it.date == FormatUtils.currentDateFormatted() }.size
            val reply = "Aaj $storeName par kul ₹${todaySales.toInt()} ki sale hui hai (${count} bills bane hain). Sabhi records safe aur sync hain."
            return@withContext AgentResponse(replyText = cleanFormatting(reply))
        }

        // Check if user is asking for pending udhaar
        if (lower.contains("udhaar") && (lower.contains("kitna") || lower.contains("baki") || lower.contains("pending") || lower.contains("kiska") || lower.contains("list"))) {
            val pendingList = invoices.filter { it.pendingAmount > 0 }
            val topDebtors = pendingList.take(5).joinToString("\n") { "• ${it.customerName}: ₹${it.pendingAmount.toInt()} (Bill: #${it.invoiceNo})" }
            val reply = if (pendingList.isEmpty()) {
                "Badhai ho! Kisi bhi customer ka koi udhaar baki nahi hai. Sabhi payments clear hain."
            } else {
                "Kul pending udhaar ₹${pendingUdhaar.toInt()} hai (${pendingList.size} unpaid bills):\n$topDebtors\n\nAap Udhaar tab se direct WhatsApp reminder bhej sakte hain ya 'Clear [Name] udhaar' bolkar chukta kar sakte hain."
            }
            return@withContext AgentResponse(replyText = cleanFormatting(reply))
        }

        // Check if user wants to settle / clear an Udhaar entry
        val settleRegex = Regex("""(?:clear|settle|chukta|chuka|paid)\s+(?:udhaar\s+)?(?:for\s+)?([A-Za-z]+)""", RegexOption.IGNORE_CASE)
        val settleRegex2 = Regex("""([A-Za-z]+)\s+ka\s+udhaar\s+(?:chukta|clear|settle)""", RegexOption.IGNORE_CASE)
        val settleMatch = settleRegex.find(lower) ?: settleRegex2.find(lower)
        if (settleMatch != null) {
            val name = settleMatch.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val debtorInv = invoices.firstOrNull { it.customerName.contains(name, ignoreCase = true) && it.pendingAmount > 0 }
            if (debtorInv != null) {
                val reply = "Maine $name ka ₹${debtorInv.pendingAmount.toInt()} ka baki udhaar chukta mark kar diya hai aur receipt generate kar di hai."
                return@withContext AgentResponse(
                    replyText = cleanFormatting(reply),
                    executedAction = AgentAction.SettleDebt(name),
                    actionSummary = "Udhaar settled for $name"
                )
            }
        }

        // Check if user wants to add an Udhaar entry
        val udhaarRegex1 = Regex("""(?:add\s+udhaar|udhaar\s+likho|udhaar\s+add\s+karo|likh\s+lo)\s+(?:for\s+)?([A-Za-z\s]+?)\s+(?:rs|inr|₹)?\s*(\d+)""", RegexOption.IGNORE_CASE)
        val udhaarRegex2 = Regex("""([A-Za-z]+)\s+(?:ko|ka)\s+(?:rs|inr|₹)?\s*(\d+)\s+(?:ka\s+)?udhaar""", RegexOption.IGNORE_CASE)
        val udhaarMatch = udhaarRegex1.find(lower) ?: udhaarRegex2.find(lower)
        if (udhaarMatch != null) {
            val name = udhaarMatch.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val amount = udhaarMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                val reply = "Maine $name ke naam par ₹${amount.toInt()} ka naya Udhaar khata darj kar diya hai aur Firebase par sync kar diya hai."
                return@withContext AgentResponse(
                    replyText = cleanFormatting(reply),
                    executedAction = AgentAction.AddUdhaar(name, "", amount, "Added via BillGen AI Agent"),
                    actionSummary = "Udhaar ₹${amount.toInt()} added for $name"
                )
            }
        }

        // Check if user wants to create a Bill
        val billRegex1 = Regex("""(?:bill\s+banao|create\s+bill|make\s+invoice|invoice\s+banao)\s+(?:for\s+)?([A-Za-z\s]+?)\s+(?:rs|inr|₹)?\s*(\d+)""", RegexOption.IGNORE_CASE)
        val billRegex2 = Regex("""([A-Za-z]+)\s+ka\s+(?:rs|inr|₹)?\s*(\d+)\s+ka\s+bill""", RegexOption.IGNORE_CASE)
        val billMatch = billRegex1.find(lower) ?: billRegex2.find(lower)
        if (billMatch != null) {
            val name = billMatch.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val amount = billMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                val reply = "Maine $name ke liye ₹${amount.toInt()} ka naya invoice draft taiyar kar diya hai. Aap ise Create / Preview tab mein dekh sakte hain."
                return@withContext AgentResponse(
                    replyText = cleanFormatting(reply),
                    executedAction = AgentAction.CreateInvoice(name, amount, "Goods & Services"),
                    actionSummary = "Invoice draft created for $name (₹${amount.toInt()})"
                )
            }
        }

        // Check if user wants to add a product to catalog
        val productRegex = Regex("""(?:add\s+product|product\s+add\s+karo)\s+([A-Za-z0-9\s]+?)\s+price\s*(\d+)(?:\s+stock\s*(\d+))?""", RegexOption.IGNORE_CASE)
        val productMatch = productRegex.find(lower)
        if (productMatch != null) {
            val pName = productMatch.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val price = productMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val stock = productMatch.groupValues[3].toIntOrNull() ?: 10
            val reply = "Product '$pName' (Price: ₹${price.toInt()}, Stock: $stock) catalog mein safaltapoorvak add ho gaya hai."
            return@withContext AgentResponse(
                replyText = cleanFormatting(reply),
                executedAction = AgentAction.AddProduct(pName, price, stock),
                actionSummary = "Product $pName added"
            )
        }

        // Check if user wants to check catalog stock
        if (lower.contains("stock") || lower.contains("catalog") || lower.contains("inventory")) {
            val pList = products.take(6).joinToString("\n") { "• ${it.name}: ₹${it.price.toInt()} (Stock: ${it.stock})" }
            val reply = if (products.isEmpty()) {
                "Aapke catalog mein abhi koi product nahi hai. Aap 'Add product [Name] price [Price]' bolkar add kar sakte hain."
            } else {
                "Catalog Summary (${products.size} items):\n$pList"
            }
            return@withContext AgentResponse(replyText = cleanFormatting(reply))
        }

        // 2. Fallback to Gemini 3.5 Flash for natural answers with live store context
        val apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) { "" }

        if (apiKey.isBlank()) {
            return@withContext AgentResponse(
                replyText = cleanFormatting(
                    "Namaste! Main BillGen AI Agent hoon. Main aapke store ($storeName) ke bills, udhaar khata, catalog aur sales manage karne mein madad kar sakta hoon. Poochiye: 'Aaj ki sale kitni hai?', 'Rahul ka 500 ka bill banao', ya 'Pending udhaar kitna hai?'."
                )
            )
        }

        try {
            val systemContext = """
                You are BillGen AI Agent, the built-in smart business manager for the Android billing app BillGen AI.
                Current Store Context:
                - Store Name: $storeName
                - Today's Sales: ₹${todaySales.toInt()}
                - Total Sales: ₹${totalSales.toInt()}
                - Total Pending Udhaar (Customer Credit): ₹${pendingUdhaar.toInt()}
                - Total Invoices Generated: ${invoices.size}
                - Total Catalog Products: ${products.size}
                
                CRITICAL FORMATTING INSTRUCTIONS:
                - Do NOT use markdown asterisks (never use ** or *).
                - Provide clean, professional plain text with clear bullet points (•) and line breaks.
                - Keep answers respectful, polite, and direct for an Indian merchant/shopkeeper in natural Hindi/Hinglish (or English if addressed in English).
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemContext\n\nUser Question: $userMessage"))
                    })
                }))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 250)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val root = JSONObject(respBody)
                val text = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                if (text.isNotBlank()) {
                    return@withContext AgentResponse(replyText = cleanFormatting(text))
                }
            }
        } catch (_: Exception) {}

        return@withContext AgentResponse(
            replyText = cleanFormatting(
                "Main aapke store ke bills, udhaar, inventory aur sales sambhal sakta hoon. Jaise: 'Rahul ka 500 ka bill banao', 'Amit ka 1000 udhaar likho', ya 'Aaj kitni sale hui?'"
            )
        )
    }
}
