package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.CalculationDiscrepancy
import com.example.data.ai.GeminiInvoiceService
import com.example.data.ai.LocalInvoiceParser
import com.example.data.ai.ParsedInvoiceData
import com.example.data.local.AppDatabase
import com.example.data.local.Converters
import com.example.data.model.BusinessProfile
import com.example.data.model.InvoiceEntity
import com.example.data.model.LineItem
import com.example.data.model.ProductEntity
import com.example.data.model.QuotationEntity
import com.example.data.model.ReceiptEntity
import com.example.ui.monetization.DailyRetentionHelper
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillGenViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val invoiceDao = db.invoiceDao()
    private val productDao = db.productDao()
    private val receiptDao = db.receiptDao()
    private val quotationDao = db.quotationDao()
    private val geminiService = GeminiInvoiceService()
    val retentionHelper = DailyRetentionHelper(application)
    private val converters = Converters()

    private val prefs = application.getSharedPreferences("billgen_business_profile", Context.MODE_PRIVATE)

    val allInvoices: StateFlow<List<InvoiceEntity>> = invoiceDao.getAllInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = productDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReceipts: StateFlow<List<ReceiptEntity>> = receiptDao.getAllReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuotations: StateFlow<List<QuotationEntity>> = quotationDao.getAllQuotations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _businessProfile = MutableStateFlow(loadBusinessProfile())
    val businessProfile: StateFlow<BusinessProfile> = _businessProfile.asStateFlow()

    var editingInvoiceId: String? = null
    val editorCustomerName = MutableStateFlow("")
    val editorCustomerPhone = MutableStateFlow("")
    val editorCustomerAddress = MutableStateFlow("")
    val editorInvoiceNo = MutableStateFlow("")
    val editorDate = MutableStateFlow(FormatUtils.currentDateFormatted())
    val editorDueDate = MutableStateFlow("")
    val editorOrderId = MutableStateFlow("")
    val editorItems = MutableStateFlow<List<LineItem>>(listOf(LineItem(name = "", qty = 1.0, price = 0.0)))
    val editorDiscount = MutableStateFlow(0.0)
    val editorShipping = MutableStateFlow(0.0)
    val editorRoundoff = MutableStateFlow(0.0)
    val editorGstRate = MutableStateFlow(0.0)
    val editorGstType = MutableStateFlow("intra")
    val editorGstMode = MutableStateFlow("exclusive")
    val editorPaymentStatus = MutableStateFlow("UNPAID")
    val editorPaymentMethod = MutableStateFlow("UPI")
    val editorAmountPaid = MutableStateFlow(0.0)
    val editorNotes = MutableStateFlow("Thank you for your business!")
    val editorTerms = MutableStateFlow("Goods once sold cannot be returned.")
    val editorTemplate = MutableStateFlow("modern")
    val editorTheme = MutableStateFlow("orange")
    val editorCurrency = MutableStateFlow("INR")

    private val _activePreviewInvoice = MutableStateFlow<InvoiceEntity?>(null)
    val activePreviewInvoice: StateFlow<InvoiceEntity?> = _activePreviewInvoice.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow("")
    val aiStatusMessage: StateFlow<String> = _aiStatusMessage.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    val aiNlpResult = MutableStateFlow<String?>(null)
    val aiMessyResult = MutableStateFlow<String?>(null)
    val aiCheckResult = MutableStateFlow<String?>(null)

    init {
        retentionHelper.recordDailyActivity()
        initNewInvoice()
        seedSampleDataIfEmpty()
    }

    fun initNewInvoice() {
        editingInvoiceId = null
        val prof = _businessProfile.value
        editorInvoiceNo.value = FormatUtils.generateNextInvoiceNumber(prof.invoicePrefix, prof.nextInvoiceNumber)
        editorCustomerName.value = ""
        editorCustomerPhone.value = ""
        editorCustomerAddress.value = ""
        editorDate.value = FormatUtils.currentDateFormatted()
        editorDueDate.value = ""
        editorOrderId.value = ""
        editorItems.value = listOf(LineItem(name = "", qty = 1.0, price = 0.0))
        editorDiscount.value = 0.0
        editorShipping.value = 0.0
        editorRoundoff.value = 0.0
        editorGstRate.value = prof.defaultGstRate
        editorGstType.value = "intra"
        editorGstMode.value = "exclusive"
        editorPaymentStatus.value = "UNPAID"
        editorPaymentMethod.value = "UPI"
        editorAmountPaid.value = 0.0
        editorNotes.value = "Thank you for your business!"
        editorTerms.value = "Goods once sold cannot be returned unless defective."
        editorTemplate.value = "modern"
        editorTheme.value = "orange"
        editorCurrency.value = prof.defaultCurrency
        _selectedImageBitmap.value = null
        _aiStatusMessage.value = "Ready to create invoice"
    }

    fun loadInvoiceForEditing(invoice: InvoiceEntity) {
        editingInvoiceId = invoice.id
        editorInvoiceNo.value = invoice.invoiceNo
        editorCustomerName.value = invoice.customerName
        editorCustomerPhone.value = invoice.customerPhone
        editorCustomerAddress.value = invoice.customerAddress
        editorDate.value = invoice.date
        editorDueDate.value = invoice.dueDate
        editorOrderId.value = invoice.orderId
        editorItems.value = converters.toLineItemList(invoice.itemsJson).ifEmpty {
            listOf(LineItem(name = "Item", qty = 1.0, price = invoice.total))
        }
        editorDiscount.value = invoice.discount
        editorShipping.value = invoice.shipping
        editorRoundoff.value = invoice.roundoff
        editorGstRate.value = invoice.gstRate
        editorGstType.value = invoice.gstType
        editorGstMode.value = invoice.gstMode
        editorPaymentStatus.value = invoice.paymentStatus
        editorPaymentMethod.value = invoice.paymentMethod
        editorAmountPaid.value = invoice.amountPaid
        editorNotes.value = invoice.notes
        editorTerms.value = invoice.terms
        editorTemplate.value = invoice.templateName
        editorTheme.value = invoice.themeColor
        editorCurrency.value = invoice.currency
        _activePreviewInvoice.value = invoice
    }

    fun setPreviewInvoice(invoice: InvoiceEntity) {
        _activePreviewInvoice.value = invoice
    }

    fun addItemToEditor() {
        val current = editorItems.value.toMutableList()
        current.add(LineItem(name = "", qty = 1.0, price = 0.0))
        editorItems.value = current
    }

    fun updateItemInEditor(index: Int, item: LineItem) {
        val current = editorItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = item
            editorItems.value = current
        }
    }

    fun removeItemFromEditor(index: Int) {
        val current = editorItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            if (current.isEmpty()) {
                current.add(LineItem(name = "", qty = 1.0, price = 0.0))
            }
            editorItems.value = current
        }
    }

    fun selectCatalogProductForItem(index: Int, product: ProductEntity) {
        val current = editorItems.value.toMutableList()
        if (index in current.indices) {
            val old = current[index]
            current[index] = old.copy(
                catalogId = product.id,
                name = product.name,
                price = product.price,
                hsn = product.hsn
            )
            editorItems.value = current
        }
    }

    fun setSelectedBitmap(bitmap: Bitmap?) {
        _selectedImageBitmap.value = bitmap
    }

    fun extractInvoiceWithAi(pastedText: String = "", customApiKey: String? = null, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isExtracting.value = true
            _aiStatusMessage.value = "Analyzing order with Gemini AI..."

            val hasCredit = retentionHelper.consumeAiScanCredit()
            if (!hasCredit) {
                _aiStatusMessage.value = "No AI scan credits left. Watch a quick ad or upgrade to Pro."
                _isExtracting.value = false
                return@launch
            }

            val result = geminiService.extractInvoice(
                bitmap = _selectedImageBitmap.value,
                pastedText = pastedText,
                customApiKey = customApiKey
            )

            result.onSuccess { data ->
                applyParsedDataToEditor(data)
                _aiStatusMessage.value = "Extraction complete! Review your invoice."
                onComplete()
            }.onFailure { error ->
                _aiStatusMessage.value = "Extraction error: ${error.message}"
            }

            _isExtracting.value = false
        }
    }

    private fun applyParsedDataToEditor(data: ParsedInvoiceData) {
        if (data.customerName.isNotBlank()) editorCustomerName.value = data.customerName
        if (data.customerPhone.isNotBlank()) editorCustomerPhone.value = data.customerPhone
        if (data.customerAddress.isNotBlank()) editorCustomerAddress.value = data.customerAddress
        if (data.items.isNotEmpty()) editorItems.value = data.items
        editorDiscount.value = data.discount
        editorShipping.value = data.shipping
        if (data.gstRate > 0) editorGstRate.value = data.gstRate
        if (data.paymentMethod.isNotBlank()) editorPaymentMethod.value = data.paymentMethod
        if (data.paymentStatus.isNotBlank()) editorPaymentStatus.value = data.paymentStatus
        if (data.amountPaid > 0) editorAmountPaid.value = data.amountPaid
        if (data.notes.isNotBlank()) editorNotes.value = data.notes
    }

    fun saveCurrentInvoice(onSaved: (InvoiceEntity) -> Unit) {
        viewModelScope.launch {
            val prof = _businessProfile.value
            val calc = FormatUtils.calculateInvoice(
                items = editorItems.value,
                discount = editorDiscount.value,
                shipping = editorShipping.value,
                roundoff = editorRoundoff.value,
                gstRate = editorGstRate.value,
                gstType = editorGstType.value,
                gstMode = editorGstMode.value,
                amountPaid = editorAmountPaid.value,
                dueDate = editorDueDate.value
            )

            val invoice = InvoiceEntity(
                id = editingInvoiceId ?: "inv_${System.currentTimeMillis()}",
                invoiceNo = editorInvoiceNo.value.ifBlank {
                    FormatUtils.generateNextInvoiceNumber(prof.invoicePrefix, prof.nextInvoiceNumber)
                },
                date = editorDate.value.ifBlank { FormatUtils.currentDateFormatted() },
                dueDate = editorDueDate.value,
                orderId = editorOrderId.value,
                customerName = editorCustomerName.value.ifBlank { "Customer" },
                customerPhone = editorCustomerPhone.value,
                customerAddress = editorCustomerAddress.value,
                businessName = prof.businessName,
                businessTagline = prof.businessTagline,
                businessAddress = prof.businessAddress,
                businessPhone = prof.businessPhone,
                businessEmail = prof.businessEmail,
                businessWebsite = prof.businessWebsite,
                businessGstin = prof.businessGstin,
                upiId = prof.upiId,
                bankDetails = prof.bankDetails,
                logoUri = prof.logoUri,
                itemsJson = converters.fromLineItemList(editorItems.value),
                subtotal = calc.subtotal,
                discount = calc.discount,
                shipping = calc.shipping,
                roundoff = calc.roundoff,
                gstRate = calc.gstRate,
                gstType = calc.gstType,
                gstMode = calc.gstMode,
                taxableAmount = calc.taxableAmount,
                cgst = calc.cgst,
                sgst = calc.sgst,
                igst = calc.igst,
                totalGst = calc.totalGst,
                total = calc.total,
                amountPaid = calc.amountPaid,
                paymentStatus = calc.status,
                paymentMethod = editorPaymentMethod.value,
                notes = editorNotes.value,
                terms = editorTerms.value,
                templateName = editorTemplate.value,
                themeColor = editorTheme.value,
                currency = editorCurrency.value,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            invoiceDao.insertInvoice(invoice)

            editorItems.value.forEach { item ->
                item.catalogId?.let { catId ->
                    productDao.deductStock(catId, item.qty.toInt().coerceAtLeast(1))
                }
            }

            if (editingInvoiceId == null) {
                incrementNextInvoiceNumber()
            }

            retentionHelper.recordDailyActivity()
            _activePreviewInvoice.value = invoice
            onSaved(invoice)
        }
    }

    fun duplicateInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            val prof = _businessProfile.value
            val newNum = FormatUtils.generateNextInvoiceNumber(prof.invoicePrefix, prof.nextInvoiceNumber)
            val duplicated = invoice.copy(
                id = "inv_${System.currentTimeMillis()}",
                invoiceNo = newNum,
                date = FormatUtils.currentDateFormatted(),
                amountPaid = 0.0,
                paymentStatus = "UNPAID",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            invoiceDao.insertInvoice(duplicated)
            incrementNextInvoiceNumber()
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            invoiceDao.deleteInvoiceById(invoice.id)
            if (_activePreviewInvoice.value?.id == invoice.id) {
                _activePreviewInvoice.value = null
            }
        }
    }

    fun recordPaymentForInvoice(invoice: InvoiceEntity, amount: Double, method: String, reference: String) {
        viewModelScope.launch {
            val newPaid = (invoice.amountPaid + amount).coerceAtMost(invoice.total)
            val newStatus = if (newPaid >= invoice.total) "PAID" else "PARTIALLY PAID"
            val updated = invoice.copy(
                amountPaid = newPaid,
                paymentStatus = newStatus,
                paymentMethod = method,
                updatedAt = System.currentTimeMillis()
            )
            invoiceDao.updateInvoice(updated)

            val receipt = ReceiptEntity(
                receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(4)}",
                customerName = invoice.customerName,
                invoiceNo = invoice.invoiceNo,
                amount = amount,
                method = method,
                date = FormatUtils.currentDateFormatted(),
                reference = reference,
                businessName = invoice.businessName
            )
            receiptDao.insertReceipt(receipt)
            _activePreviewInvoice.value = updated
        }
    }

    fun addOrUpdateProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.insertProduct(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.deleteProductById(product.id)
        }
    }

    fun restockProduct(id: String, additionalQty: Int) {
        viewModelScope.launch {
            productDao.restockProduct(id, additionalQty)
        }
    }

    fun createQuotation(quotation: QuotationEntity) {
        viewModelScope.launch {
            quotationDao.insertQuotation(quotation)
        }
    }

    fun convertQuotationToInvoice(quotation: QuotationEntity) {
        viewModelScope.launch {
            val prof = _businessProfile.value
            val invNum = FormatUtils.generateNextInvoiceNumber(prof.invoicePrefix, prof.nextInvoiceNumber)
            val invoice = InvoiceEntity(
                invoiceNo = invNum,
                date = FormatUtils.currentDateFormatted(),
                customerName = quotation.customerName,
                customerPhone = quotation.customerPhone,
                customerAddress = quotation.customerAddress,
                businessName = quotation.businessName.ifBlank { prof.businessName },
                itemsJson = quotation.itemsJson,
                subtotal = quotation.subtotal,
                discount = quotation.discount,
                gstRate = quotation.gstRate,
                total = quotation.total,
                templateName = "modern",
                themeColor = "orange",
                currency = prof.defaultCurrency
            )
            invoiceDao.insertInvoice(invoice)
            quotationDao.updateStatus(quotation.id, "Converted")
            incrementNextInvoiceNumber()
            _activePreviewInvoice.value = invoice
        }
    }

    fun deleteQuotation(quotation: QuotationEntity) {
        viewModelScope.launch {
            quotationDao.deleteQuotationById(quotation.id)
        }
    }

    fun updateBusinessProfile(newProfile: BusinessProfile) {
        _businessProfile.value = newProfile
        prefs.edit()
            .putString("name", newProfile.businessName)
            .putString("tagline", newProfile.businessTagline)
            .putString("address", newProfile.businessAddress)
            .putString("phone", newProfile.businessPhone)
            .putString("email", newProfile.businessEmail)
            .putString("website", newProfile.businessWebsite)
            .putString("gstin", newProfile.businessGstin)
            .putString("upi", newProfile.upiId)
            .putString("bank", newProfile.bankDetails)
            .putString("prefix", newProfile.invoicePrefix)
            .putInt("next_num", newProfile.nextInvoiceNumber)
            .putFloat("default_gst", newProfile.defaultGstRate.toFloat())
            .putString("currency", newProfile.defaultCurrency)
            .apply()
    }

    private fun incrementNextInvoiceNumber() {
        val prof = _businessProfile.value
        val updated = prof.copy(nextInvoiceNumber = prof.nextInvoiceNumber + 1)
        updateBusinessProfile(updated)
    }

    private fun loadBusinessProfile(): BusinessProfile {
        return BusinessProfile(
            businessName = prefs.getString("name", "BillGen AI Shop") ?: "BillGen AI Shop",
            businessTagline = prefs.getString("tagline", "Quality • Trust • Service") ?: "Quality • Trust • Service",
            businessAddress = prefs.getString("address", "Connaught Place, New Delhi, India") ?: "Connaught Place, New Delhi, India",
            businessPhone = prefs.getString("phone", "+91 98765 43210") ?: "+91 98765 43210",
            businessEmail = prefs.getString("email", "billing@billgen.ai") ?: "billing@billgen.ai",
            businessWebsite = prefs.getString("website", "www.billgen.ai") ?: "www.billgen.ai",
            businessGstin = prefs.getString("gstin", "07AAAAA0000A1Z5") ?: "07AAAAA0000A1Z5",
            upiId = prefs.getString("upi", "billgen@upi") ?: "billgen@upi",
            bankDetails = prefs.getString("bank", "HDFC Bank • A/C 50200012345678 • IFSC HDFC0001234") ?: "HDFC Bank • A/C 50200012345678 • IFSC HDFC0001234",
            defaultGstRate = prefs.getFloat("default_gst", 18f).toDouble(),
            defaultCurrency = prefs.getString("currency", "INR") ?: "INR",
            invoicePrefix = prefs.getString("prefix", "INV") ?: "INV",
            nextInvoiceNumber = prefs.getInt("next_num", 1)
        )
    }

    fun runAiAssistantOrder(promptText: String) {
        val parsed = LocalInvoiceParser.parseNaturalOrder(promptText)
        aiNlpResult.value = "Extracted Order:\nCustomer: ${parsed.customerName}\nItems: ${parsed.items.size} item(s)\nTotal: ₹${parsed.items.sumOf { it.total }}"
        applyParsedDataToEditor(parsed)
    }

    fun runMessyOcrCleaner(messyText: String) {
        val parsed = LocalInvoiceParser.parseNaturalOrder(messyText)
        aiMessyResult.value = "Cleaned Items:\n" + parsed.items.joinToString("\n") { "• ${it.name}: ${it.qty} × ₹${it.price} = ₹${it.total}" }
        applyParsedDataToEditor(parsed)
    }

    fun runDiscrepancyCheck(text: String): CalculationDiscrepancy? {
        val result = LocalInvoiceParser.checkCalculationDiscrepancy(text)
        if (result != null) {
            aiCheckResult.value = if (result.isMatch) {
                "✓ Calculation matches! (Expected: ₹${result.expected}, Actual: ₹${result.actual})"
            } else {
                "⚠️ Mismatch! Expected ₹${result.expected}, entered ₹${result.actual} (Diff: ₹${result.diff})"
            }
        } else {
            aiCheckResult.value = "⚠️ Could not parse Quantity, Price and Total from text."
        }
        return result
    }

    private fun seedSampleDataIfEmpty() {
        viewModelScope.launch {
            val p1 = ProductEntity(name = "Cotton T-Shirt", price = 599.0, stock = 24, hsn = "6109")
            val p2 = ProductEntity(name = "Slim Fit Jeans", price = 999.0, stock = 12, hsn = "6203")
            val p3 = ProductEntity(name = "Baseball Cap", price = 299.0, stock = 4, hsn = "6505")
            val p4 = ProductEntity(name = "Winter Hoodie", price = 1299.0, stock = 8, hsn = "6102")

            productDao.insertProduct(p1)
            productDao.insertProduct(p2)
            productDao.insertProduct(p3)
            productDao.insertProduct(p4)
        }
    }
}
