package com.example.data.firebase

import android.util.Log
import com.example.data.model.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseSyncService {
    private val database: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance("https://billgen-cc831-default-rtdb.firebaseio.com")
        } catch (e: Exception) {
            Log.e("FirebaseSyncService", "FirebaseDatabase init error: ${e.message}")
            null
        }
    }

    private fun userRef(userId: String) = database?.getReference("users")?.child(userId)

    suspend fun pushInvoiceToCloud(userId: String, invoice: InvoiceEntity) {
        val ref = userRef(userId) ?: return
        try {
            val map = HashMap<String, Any?>().apply {
                put("id", invoice.id)
                put("invoiceNo", invoice.invoiceNo)
                put("date", invoice.date)
                put("dueDate", invoice.dueDate)
                put("orderId", invoice.orderId)
                put("customerName", invoice.customerName)
                put("customerPhone", invoice.customerPhone)
                put("customerAddress", invoice.customerAddress)
                put("businessName", invoice.businessName)
                put("businessTagline", invoice.businessTagline)
                put("businessAddress", invoice.businessAddress)
                put("businessPhone", invoice.businessPhone)
                put("businessEmail", invoice.businessEmail)
                put("businessWebsite", invoice.businessWebsite)
                put("businessGstin", invoice.businessGstin)
                put("upiId", invoice.upiId)
                put("bankDetails", invoice.bankDetails)
                put("logoUri", invoice.logoUri)
                put("itemsJson", invoice.itemsJson)
                put("subtotal", invoice.subtotal)
                put("discount", invoice.discount)
                put("shipping", invoice.shipping)
                put("roundoff", invoice.roundoff)
                put("gstRate", invoice.gstRate)
                put("gstType", invoice.gstType)
                put("gstMode", invoice.gstMode)
                put("taxableAmount", invoice.taxableAmount)
                put("cgst", invoice.cgst)
                put("sgst", invoice.sgst)
                put("igst", invoice.igst)
                put("totalGst", invoice.totalGst)
                put("total", invoice.total)
                put("amountPaid", invoice.amountPaid)
                put("paymentStatus", invoice.paymentStatus)
                put("paymentMethod", invoice.paymentMethod)
                put("notes", invoice.notes)
                put("terms", invoice.terms)
                put("templateName", invoice.templateName)
                put("themeColor", invoice.themeColor)
                put("currency", invoice.currency)
                put("createdAt", invoice.createdAt)
                put("updatedAt", invoice.updatedAt)
            }
            ref.child("invoices").child(invoice.id).setValue(map).await()
            Log.d("FirebaseSync", "Invoice ${invoice.invoiceNo} successfully uploaded to Firebase")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error uploading invoice: ${e.message}")
        }
    }

    suspend fun deleteInvoiceFromCloud(userId: String, invoiceId: String) {
        val ref = userRef(userId) ?: return
        try {
            ref.child("invoices").child(invoiceId).removeValue().await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting invoice: ${e.message}")
        }
    }

    suspend fun pushProductToCloud(userId: String, product: ProductEntity) {
        val ref = userRef(userId) ?: return
        try {
            val map = HashMap<String, Any?>().apply {
                put("id", product.id)
                put("name", product.name)
                put("price", product.price)
                put("stock", product.stock)
                put("hsn", product.hsn)
                put("category", product.category)
                put("createdAt", product.createdAt)
            }
            ref.child("products").child(product.id).setValue(map).await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error uploading product: ${e.message}")
        }
    }

    suspend fun deleteProductFromCloud(userId: String, productId: String) {
        val ref = userRef(userId) ?: return
        try {
            ref.child("products").child(productId).removeValue().await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting product: ${e.message}")
        }
    }

    suspend fun pushReceiptToCloud(userId: String, receipt: ReceiptEntity) {
        val ref = userRef(userId) ?: return
        try {
            val map = HashMap<String, Any?>().apply {
                put("id", receipt.id)
                put("receiptNo", receipt.receiptNo)
                put("customerName", receipt.customerName)
                put("invoiceNo", receipt.invoiceNo)
                put("amount", receipt.amount)
                put("method", receipt.method)
                put("date", receipt.date)
                put("reference", receipt.reference)
                put("businessName", receipt.businessName)
                put("notes", receipt.notes)
                put("createdAt", receipt.createdAt)
            }
            ref.child("receipts").child(receipt.id).setValue(map).await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error uploading receipt: ${e.message}")
        }
    }

    suspend fun pushBusinessProfileToCloud(userId: String, profile: BusinessProfile) {
        val ref = userRef(userId) ?: return
        try {
            val map = HashMap<String, Any?>().apply {
                put("businessName", profile.businessName)
                put("businessTagline", profile.businessTagline)
                put("businessAddress", profile.businessAddress)
                put("businessPhone", profile.businessPhone)
                put("businessEmail", profile.businessEmail)
                put("businessWebsite", profile.businessWebsite)
                put("businessGstin", profile.businessGstin)
                put("upiId", profile.upiId)
                put("bankDetails", profile.bankDetails)
                put("defaultCurrency", profile.defaultCurrency)
                put("defaultTheme", profile.defaultTheme)
                put("invoicePrefix", profile.invoicePrefix)
                put("nextInvoiceNumber", profile.nextInvoiceNumber)
                put("defaultGstRate", profile.defaultGstRate)
                put("logoUri", profile.logoUri)
            }
            ref.child("profile").setValue(map).await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error uploading profile: ${e.message}")
        }
    }

    suspend fun fetchAllInvoicesFromCloud(userId: String): List<InvoiceEntity> {
        val ref = userRef(userId) ?: return emptyList()
        return try {
            val snapshot = ref.child("invoices").get().await()
            val list = mutableListOf<InvoiceEntity>()
            for (child in snapshot.children) {
                val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                val invNum = child.child("invoiceNo").getValue(String::class.java) ?: "INV"
                val date = child.child("date").getValue(String::class.java) ?: ""
                val dueDate = child.child("dueDate").getValue(String::class.java) ?: ""
                val orderId = child.child("orderId").getValue(String::class.java) ?: ""
                val cName = child.child("customerName").getValue(String::class.java) ?: ""
                val cPhone = child.child("customerPhone").getValue(String::class.java) ?: ""
                val cAddr = child.child("customerAddress").getValue(String::class.java) ?: ""
                val bName = child.child("businessName").getValue(String::class.java) ?: "BillGen AI"
                val bTag = child.child("businessTagline").getValue(String::class.java) ?: ""
                val bAddr = child.child("businessAddress").getValue(String::class.java) ?: ""
                val bPhone = child.child("businessPhone").getValue(String::class.java) ?: ""
                val bEmail = child.child("businessEmail").getValue(String::class.java) ?: ""
                val bWeb = child.child("businessWebsite").getValue(String::class.java) ?: ""
                val bGstin = child.child("businessGstin").getValue(String::class.java) ?: ""
                val upi = child.child("upiId").getValue(String::class.java) ?: ""
                val bank = child.child("bankDetails").getValue(String::class.java) ?: ""
                val logo = child.child("logoUri").getValue(String::class.java) ?: ""
                val itemsJson = child.child("itemsJson").getValue(String::class.java) ?: "[]"

                val subtotal = child.child("subtotal").getValue(Double::class.java) ?: 0.0
                val discount = child.child("discount").getValue(Double::class.java) ?: 0.0
                val shipping = child.child("shipping").getValue(Double::class.java) ?: 0.0
                val roundoff = child.child("roundoff").getValue(Double::class.java) ?: 0.0
                val gstRate = child.child("gstRate").getValue(Double::class.java) ?: 0.0
                val gstType = child.child("gstType").getValue(String::class.java) ?: "intra"
                val gstMode = child.child("gstMode").getValue(String::class.java) ?: "exclusive"
                val taxableAmount = child.child("taxableAmount").getValue(Double::class.java) ?: 0.0
                val cgst = child.child("cgst").getValue(Double::class.java) ?: 0.0
                val sgst = child.child("sgst").getValue(Double::class.java) ?: 0.0
                val igst = child.child("igst").getValue(Double::class.java) ?: 0.0
                val totalGst = child.child("totalGst").getValue(Double::class.java) ?: 0.0
                val total = child.child("total").getValue(Double::class.java) ?: 0.0
                val amountPaid = child.child("amountPaid").getValue(Double::class.java) ?: 0.0
                val paymentStatus = child.child("paymentStatus").getValue(String::class.java) ?: "UNPAID"
                val paymentMethod = child.child("paymentMethod").getValue(String::class.java) ?: "UPI"
                val notes = child.child("notes").getValue(String::class.java) ?: ""
                val terms = child.child("terms").getValue(String::class.java) ?: ""
                val template = child.child("templateName").getValue(String::class.java) ?: "modern"
                val theme = child.child("themeColor").getValue(String::class.java) ?: "orange"
                val curr = child.child("currency").getValue(String::class.java) ?: "INR"
                val created = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                val updated = child.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                list.add(
                    InvoiceEntity(
                        id = id,
                        invoiceNo = invNum,
                        date = date,
                        dueDate = dueDate,
                        orderId = orderId,
                        customerName = cName,
                        customerPhone = cPhone,
                        customerAddress = cAddr,
                        businessName = bName,
                        businessTagline = bTag,
                        businessAddress = bAddr,
                        businessPhone = bPhone,
                        businessEmail = bEmail,
                        businessWebsite = bWeb,
                        businessGstin = bGstin,
                        upiId = upi,
                        bankDetails = bank,
                        logoUri = logo,
                        itemsJson = itemsJson,
                        subtotal = subtotal,
                        discount = discount,
                        shipping = shipping,
                        roundoff = roundoff,
                        gstRate = gstRate,
                        gstType = gstType,
                        gstMode = gstMode,
                        taxableAmount = taxableAmount,
                        cgst = cgst,
                        sgst = sgst,
                        igst = igst,
                        totalGst = totalGst,
                        total = total,
                        amountPaid = amountPaid,
                        paymentStatus = paymentStatus,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        terms = terms,
                        templateName = template,
                        themeColor = theme,
                        currency = curr,
                        createdAt = created,
                        updatedAt = updated
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error fetching invoices from cloud: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAllProductsFromCloud(userId: String): List<ProductEntity> {
        val ref = userRef(userId) ?: return emptyList()
        return try {
            val snapshot = ref.child("products").get().await()
            val list = mutableListOf<ProductEntity>()
            for (child in snapshot.children) {
                val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                val name = child.child("name").getValue(String::class.java) ?: ""
                val price = child.child("price").getValue(Double::class.java) ?: 0.0
                val stock = child.child("stock").getValue(Int::class.java) ?: 0
                val hsn = child.child("hsn").getValue(String::class.java) ?: ""
                val cat = child.child("category").getValue(String::class.java) ?: "General"
                val created = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                list.add(
                    ProductEntity(
                        id = id,
                        name = name,
                        price = price,
                        stock = stock,
                        hsn = hsn,
                        category = cat,
                        createdAt = created
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun pushQuotationToCloud(userId: String, quotation: QuotationEntity) {
        val ref = userRef(userId) ?: return
        try {
            val map = HashMap<String, Any?>().apply {
                put("id", quotation.id)
                put("quoteNo", quotation.quoteNo)
                put("date", quotation.date)
                put("customerName", quotation.customerName)
                put("customerPhone", quotation.customerPhone)
                put("customerAddress", quotation.customerAddress)
                put("businessName", quotation.businessName)
                put("itemsJson", quotation.itemsJson)
                put("subtotal", quotation.subtotal)
                put("discount", quotation.discount)
                put("gstRate", quotation.gstRate)
                put("total", quotation.total)
                put("status", quotation.status)
                put("notes", quotation.notes)
                put("createdAt", quotation.createdAt)
            }
            ref.child("quotations").child(quotation.id).setValue(map).await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error uploading quotation: ${e.message}")
        }
    }

    suspend fun deleteQuotationFromCloud(userId: String, quotationId: String) {
        val ref = userRef(userId) ?: return
        try {
            ref.child("quotations").child(quotationId).removeValue().await()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting quotation: ${e.message}")
        }
    }

    suspend fun fetchAllQuotationsFromCloud(userId: String): List<QuotationEntity> {
        val ref = userRef(userId) ?: return emptyList()
        return try {
            val snapshot = ref.child("quotations").get().await()
            val list = mutableListOf<QuotationEntity>()
            for (child in snapshot.children) {
                val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                val quoteNo = child.child("quoteNo").getValue(String::class.java) ?: "QUO-001"
                val date = child.child("date").getValue(String::class.java) ?: ""
                val cName = child.child("customerName").getValue(String::class.java) ?: ""
                val cPhone = child.child("customerPhone").getValue(String::class.java) ?: ""
                val cAddr = child.child("customerAddress").getValue(String::class.java) ?: ""
                val bName = child.child("businessName").getValue(String::class.java) ?: ""
                val itemsJson = child.child("itemsJson").getValue(String::class.java) ?: "[]"
                val subtotal = child.child("subtotal").getValue(Double::class.java) ?: 0.0
                val discount = child.child("discount").getValue(Double::class.java) ?: 0.0
                val gstRate = child.child("gstRate").getValue(Double::class.java) ?: 0.0
                val total = child.child("total").getValue(Double::class.java) ?: 0.0
                val status = child.child("status").getValue(String::class.java) ?: "Draft"
                val notes = child.child("notes").getValue(String::class.java) ?: ""
                val created = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                list.add(
                    QuotationEntity(
                        id = id,
                        quoteNo = quoteNo,
                        date = date,
                        customerName = cName,
                        customerPhone = cPhone,
                        customerAddress = cAddr,
                        businessName = bName,
                        itemsJson = itemsJson,
                        subtotal = subtotal,
                        discount = discount,
                        gstRate = gstRate,
                        total = total,
                        status = status,
                        notes = notes,
                        createdAt = created
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchProfileFromCloud(userId: String): BusinessProfile? {
        val ref = userRef(userId) ?: return null
        return try {
            val child = ref.child("profile").get().await()
            if (!child.exists()) return null
            BusinessProfile(
                businessName = child.child("businessName").getValue(String::class.java) ?: "BillGen AI Shop",
                businessTagline = child.child("businessTagline").getValue(String::class.java) ?: "",
                businessAddress = child.child("businessAddress").getValue(String::class.java) ?: "",
                businessPhone = child.child("businessPhone").getValue(String::class.java) ?: "",
                businessEmail = child.child("businessEmail").getValue(String::class.java) ?: "",
                businessWebsite = child.child("businessWebsite").getValue(String::class.java) ?: "",
                businessGstin = child.child("businessGstin").getValue(String::class.java) ?: "",
                upiId = child.child("upiId").getValue(String::class.java) ?: "",
                bankDetails = child.child("bankDetails").getValue(String::class.java) ?: "",
                defaultCurrency = child.child("defaultCurrency").getValue(String::class.java) ?: "INR",
                defaultTheme = child.child("defaultTheme").getValue(String::class.java) ?: "orange",
                invoicePrefix = child.child("invoicePrefix").getValue(String::class.java) ?: "INV",
                nextInvoiceNumber = child.child("nextInvoiceNumber").getValue(Int::class.java) ?: 1,
                defaultGstRate = child.child("defaultGstRate").getValue(Double::class.java) ?: 18.0,
                logoUri = child.child("logoUri").getValue(String::class.java) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
