package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.model.LineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiInvoiceService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val extractionPrompt = """
        You are an intelligent billing and invoice extraction engine.
        Inspect the provided screenshot, invoice photo, bill image, or pasted order text.
        Extract and return ONLY a valid JSON object matching this structure:
        {
          "customerName": "Customer Name or empty",
          "customerPhone": "Customer Phone or empty",
          "customerAddress": "Customer Address or empty",
          "items": [
            {
              "name": "Product Name",
              "hsn": "HSN/SAC code or empty",
              "qty": 1.0,
              "price": 599.0
            }
          ],
          "discount": 0.0,
          "shipping": 0.0,
          "gstRate": 0.0,
          "paymentMethod": "UPI",
          "paymentStatus": "PAID or UNPAID or PARTIALLY PAID",
          "amountPaid": 0.0,
          "notes": ""
        }
        Extract real visible quantities and prices. Do not invent missing values. Output JSON only.
    """.trimIndent()

    suspend fun extractInvoice(
        bitmap: Bitmap? = null,
        pastedText: String = "",
        customApiKey: String? = null
    ): Result<ParsedInvoiceData> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() } ?: try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val localResult = LocalInvoiceParser.parseNaturalOrder(pastedText)
            return@withContext Result.success(localResult)
        }

        try {
            val partsArray = JSONArray()
            val fullText = buildString {
                append(extractionPrompt)
                if (pastedText.isNotBlank()) {
                    append("\n\nPasted Order Text:\n")
                    append(pastedText)
                }
            }
            partsArray.put(JSONObject().put("text", fullText))

            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                val inlineData = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Image)
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
            val generationConfig = JSONObject()
                .put("responseMimeType", "application/json")
                .put("temperature", 0.1)

            val requestJson = JSONObject()
                .put("contents", contentsArray)
                .put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val fallback = LocalInvoiceParser.parseNaturalOrder(pastedText)
                return@withContext Result.success(fallback)
            }

            val responseJson = JSONObject(responseBody)
            val candidateText = responseJson
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")

            if (candidateText.isNullOrBlank()) {
                val fallback = LocalInvoiceParser.parseNaturalOrder(pastedText)
                return@withContext Result.success(fallback)
            }

            val cleanedJson = candidateText.replace("```json", "").replace("```", "").trim()
            val parsedJson = JSONObject(cleanedJson)
            val customerName = parsedJson.optString("customerName", "")
            val customerPhone = parsedJson.optString("customerPhone", "")
            val customerAddress = parsedJson.optString("customerAddress", "")
            val discount = parsedJson.optDouble("discount", 0.0)
            val shipping = parsedJson.optDouble("shipping", 0.0)
            val gstRate = parsedJson.optDouble("gstRate", 0.0)
            val paymentMethod = parsedJson.optString("paymentMethod", "UPI")
            val paymentStatus = parsedJson.optString("paymentStatus", "UNPAID")
            val amountPaid = parsedJson.optDouble("amountPaid", 0.0)
            val notes = parsedJson.optString("notes", "")

            val itemsList = mutableListOf<LineItem>()
            val itemsJsonArray = parsedJson.optJSONArray("items")
            if (itemsJsonArray != null) {
                for (i in 0 until itemsJsonArray.length()) {
                    val itemObj = itemsJsonArray.optJSONObject(i) ?: continue
                    val name = itemObj.optString("name", "Item")
                    val hsn = itemObj.optString("hsn", "")
                    val qty = itemObj.optDouble("qty", 1.0)
                    val price = itemObj.optDouble("price", 0.0)
                    itemsList.add(LineItem(name = name, hsn = hsn, qty = qty, price = price))
                }
            }

            val finalItems = if (itemsList.isNotEmpty()) itemsList else listOf(LineItem(name = "Item 1", qty = 1.0, price = 0.0))

            Result.success(
                ParsedInvoiceData(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerAddress = customerAddress,
                    items = finalItems,
                    discount = discount,
                    shipping = shipping,
                    gstRate = gstRate,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    amountPaid = amountPaid,
                    notes = notes
                )
            )
        } catch (e: Exception) {
            val fallback = LocalInvoiceParser.parseNaturalOrder(pastedText)
            Result.success(fallback)
        }
    }
}
