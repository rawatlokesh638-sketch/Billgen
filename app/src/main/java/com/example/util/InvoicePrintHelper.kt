package com.example.util

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.model.InvoiceEntity
import com.example.data.model.LineItem
import com.example.data.model.ReceiptEntity

object InvoicePrintHelper {

    fun printInvoice(activity: Activity, invoice: InvoiceEntity, items: List<LineItem>) {
        val htmlContent = generateInvoiceHtml(invoice, items)
        val webView = WebView(activity)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                createWebPrintJob(activity, webView, "Invoice_${invoice.invoiceNo}")
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    fun printReceipt(activity: Activity, receipt: ReceiptEntity) {
        val htmlContent = generateReceiptHtml(receipt)
        val webView = WebView(activity)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                createWebPrintJob(activity, webView, "Receipt_${receipt.receiptNo}")
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun createWebPrintJob(activity: Activity, webView: WebView, jobName: String) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(activity, "Print service not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("id", "res", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, printAdapter, printAttributes)
    }

    fun generateInvoiceHtml(invoice: InvoiceEntity, items: List<LineItem>): String {
        val currency = invoice.currency.ifBlank { "INR" }
        val dateFormatted = FormatUtils.formatDate(invoice.date)
        val dueDateFormatted = if (invoice.dueDate.isNotBlank()) FormatUtils.formatDate(invoice.dueDate) else ""

        val rows = items.mapIndexed { idx, it ->
            """
            <tr>
                <td style="padding:8px;border-bottom:1px solid #eee;text-align:center;">${idx + 1}</td>
                <td style="padding:8px;border-bottom:1px solid #eee;">
                    <strong>${escape(it.name.ifBlank { "Item ${idx + 1}" })}</strong>
                </td>
                <td style="padding:8px;border-bottom:1px solid #eee;text-align:center;">${escape(it.hsn.ifBlank { "-" })}</td>
                <td style="padding:8px;border-bottom:1px solid #eee;text-align:center;">${it.qty}</td>
                <td style="padding:8px;border-bottom:1px solid #eee;text-align:right;">${FormatUtils.formatMoney(it.price, currency)}</td>
                <td style="padding:8px;border-bottom:1px solid #eee;text-align:right;"><strong>${FormatUtils.formatMoney(it.total, currency)}</strong></td>
            </tr>
            """.trimIndent()
        }.joinToString("")

        // Determine Theme & Color Scheme
        val isPro = invoice.templateName.lowercase() == "pro"
        val isProPlus = invoice.templateName.lowercase() == "pro_plus" || invoice.templateName.lowercase() == "proplus"
        val isPremium = invoice.templateName.lowercase() == "premium"

        val primaryColor = when {
            isPremium -> "#C59B27" // Luxury Gold
            isProPlus -> "#00875A" // Emerald Green
            isPro -> "#0052CC" // Royal Blue
            else -> when (invoice.themeColor) {
                "green" -> "#15945B"
                "blue" -> "#0052CC"
                "purple" -> "#6D5CE7"
                "rose" -> "#E14F78"
                "dark" -> "#17191D"
                "minimal" -> "#222222"
                else -> "#FF6500"
            }
        }

        val cardBg = if (isPremium) "#0F0F0F" else "#FFFFFF"
        val cardTextColor = if (isPremium) "#FFFFFF" else "#111827"
        val boxBg = if (isPremium) "#1A1A1A" else if (isProPlus) "#E6F4EA" else if (isPro) "#F4F8FF" else "#FCFCFC"
        val boxBorder = if (isPremium) "#333333" else if (isProPlus) "#B7E4D3" else if (isPro) "#D0E2FF" else "#EDF2F7"

        val qrCodeUrl = if (invoice.upiId.isNotBlank()) {
            "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" +
                    java.net.URLEncoder.encode("upi://pay?pa=${invoice.upiId}&pn=${invoice.businessName}&am=${invoice.pendingAmount}&cu=INR", "UTF-8")
        } else ""

        val isPaid = invoice.effectiveStatus == "PAID"
        val statusBg = if (isPaid) "#15945B" else "#D97706"

        // Strictly conditional: Filter out any refund/warranty mentions
        val filteredTerms = invoice.terms.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.contains("refund", ignoreCase = true) && !it.contains("warranty", ignoreCase = true) }

        val hasTerms = filteredTerms.isNotEmpty()
        val hasSignatureOrStamp = invoice.stampUri.isNotBlank() || invoice.signatureUri.isNotBlank() || invoice.signatoryName.isNotBlank()
        val hasPaymentDetails = invoice.upiId.isNotBlank() || invoice.bankAccountNo.isNotBlank() || invoice.bankName.isNotBlank() || invoice.bankIfsc.isNotBlank()

        // Ship To: ONLY show if user actually provided ship to address or receiver name
        val hasShipTo = invoice.shipToAddress.isNotBlank() || (invoice.shipToName.isNotBlank() && invoice.shipToName != invoice.customerName)

        val hasLogo = invoice.logoUri.isNotBlank()
        val hasBusinessPhoto = invoice.businessPhotoUri.isNotBlank()
        val hasBusinessGstin = invoice.businessGstin.isNotBlank()

        val termsHtml = if (hasTerms) {
            "<ol style='margin:5px 0 0 15px;padding:0;font-size:11px;color:" + (if (isPremium) "#CCCCCC" else "#444") + ";'>" +
                    filteredTerms.joinToString("") { "<li>" + escape(it) + "</li>" } +
                    "</ol>"
        } else ""

        val topBadgeHtml = when {
            isPremium -> """<div style="background:#C59B27;color:#000;padding:6px 16px;font-weight:900;font-size:12px;border-radius:6px;display:inline-block;margin-bottom:10px;">👑 PREMIUM • Elegant & Customizable</div>"""
            isProPlus -> """<div style="background:#00875A;color:#FFF;padding:6px 16px;font-weight:900;font-size:12px;border-radius:6px;display:inline-block;margin-bottom:10px;">⚡ PRO PLUS • Modern & Branded</div>"""
            isPro -> """<div style="background:#0052CC;color:#FFF;padding:6px 16px;font-weight:900;font-size:12px;border-radius:6px;display:inline-block;margin-bottom:10px;">⭐ PRO • Clean & Professional</div>"""
            else -> ""
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Invoice #${escape(invoice.invoiceNo)}</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; margin: 0; padding: 15px; color: $cardTextColor; background: ${if (isPremium) "#000000" else "#FFFFFF"}; }
                .invoice-card { max-width: 850px; margin: auto; border: 2px solid $primaryColor; border-radius: 12px; overflow: hidden; background: $cardBg; box-shadow: 0 4px 10px rgba(0,0,0,0.15); }
                .header-grid { display: flex; justify-content: space-between; align-items: flex-start; padding: 20px; background: ${if (isPremium) "#141414" else "#FAFAFA"}; border-bottom: 1px solid $boxBorder; }
                .brand-left { max-width: ${if (hasBusinessPhoto || (isPremium && qrCodeUrl.isNotBlank())) "60%" else "100%"}; }
                .brand-right { text-align: right; max-width: 38%; }
                .brand-title { font-size: 26px; font-weight: 900; color: $cardTextColor; margin: 0 0 4px 0; text-transform: uppercase; }
                .brand-tagline { font-size: 12px; font-weight: 600; color: $primaryColor; margin-bottom: 8px; }
                .info-line { font-size: 11px; color: ${if (isPremium) "#AAAAAA" else "#4A5568"}; margin: 2px 0; }
                .orange-banner { background: $primaryColor; color: ${if (isPremium) "#000" else "#FFF"}; padding: 12px 20px; display: flex; justify-content: space-between; align-items: center; }
                .banner-title { font-size: 28px; font-weight: 900; margin: 0; letter-spacing: 1px; }
                .banner-subtitle { font-size: 12px; opacity: 0.9; }
                .meta-table { font-size: 11px; color: ${if (isPremium) "#000" else "#FFF"}; border-collapse: collapse; }
                .meta-table td { padding: 2px 6px; }
                .status-pill { background: $statusBg; color: #fff; padding: 4px 12px; border-radius: 20px; font-weight: 800; font-size: 12px; display: inline-block; }
                .grid-two { display: flex; justify-content: space-between; padding: 15px 20px; gap: 15px; background: $cardBg; }
                .info-box { flex: 1; border: 1px solid $boxBorder; border-radius: 8px; padding: 12px; background: $boxBg; }
                .box-header { font-size: 12px; font-weight: 800; color: $primaryColor; border-bottom: 1px solid $boxBorder; padding-bottom: 6px; margin-bottom: 8px; text-transform: uppercase; display: flex; align-items: center; gap: 4px; }
                .items-table { width: 100%; border-collapse: collapse; margin: 0; font-size: 12px; }
                .items-table th { background: $primaryColor; color: ${if (isPremium) "#000" else "#FFF"}; padding: 10px 8px; font-weight: 800; font-size: 11px; text-align: left; }
                .summary-grid { display: flex; justify-content: space-between; padding: 15px 20px; gap: 15px; }
                .payment-box { flex: 1.2; border: 1px solid $boxBorder; border-radius: 8px; padding: 12px; background: $boxBg; }
                .calc-box { flex: 1; border: 1px solid $boxBorder; border-radius: 8px; padding: 12px; background: $boxBg; }
                .grand-total { background: $primaryColor; color: ${if (isPremium) "#000" else "#FFF"}; padding: 10px; border-radius: 6px; font-size: 18px; font-weight: 900; display: flex; justify-content: space-between; margin-top: 10px; }
                .footer-grid { display: flex; justify-content: space-between; padding: 15px 20px; gap: 15px; border-top: 1px solid $boxBorder; align-items: flex-end; }
                .trust-bar { background: $primaryColor; color: ${if (isPremium) "#000" else "#FFF"}; padding: 8px 20px; display: flex; justify-content: space-between; font-size: 10px; font-weight: 800; text-transform: uppercase; }
                .thankyou-text { font-size: 15px; font-weight: 900; color: ${if (isPremium) "#000" else "#FFF"}; font-style: italic; }
            </style>
        </head>
        <body>
            <div style="text-align:center;">$topBadgeHtml</div>
            <div class="invoice-card">
                <!-- Header -->
                <div class="header-grid">
                    <div class="brand-left">
                        ${if (hasLogo) """<img src="${invoice.logoUri}" style="max-height:60px;margin-bottom:6px;border-radius:6px;display:block;">""" else ""}
                        <div class="brand-title">${escape(invoice.businessName.ifBlank { "My Business" })}</div>
                        ${if (invoice.businessTagline.isNotBlank()) "<div class='brand-tagline'>" + escape(invoice.businessTagline) + "</div>" else ""}
                        ${if (invoice.businessAddress.isNotBlank()) "<div class='info-line'>📍 " + escape(invoice.businessAddress) + "</div>" else ""}
                        ${if (invoice.businessPhone.isNotBlank()) "<div class='info-line'>📞 " + escape(invoice.businessPhone) + "</div>" else ""}
                        ${if (invoice.businessEmail.isNotBlank()) "<div class='info-line'>✉️ " + escape(invoice.businessEmail) + "</div>" else ""}
                        ${if (invoice.businessWebsite.isNotBlank()) "<div class='info-line'>🌐 " + escape(invoice.businessWebsite) + "</div>" else ""}
                        ${if (invoice.socialHandle.isNotBlank()) "<div class='info-line'>📷 " + escape(invoice.socialHandle) + "</div>" else ""}
                        ${if (hasBusinessGstin) "<div class='info-line'><strong>GSTIN:</strong> " + escape(invoice.businessGstin) + "</div>" else ""}
                    </div>
                    <div class="brand-right">
                        ${if (hasBusinessPhoto) """<img src="${invoice.businessPhotoUri}" style="max-height:80px;border-radius:8px;margin-bottom:6px;border:2px solid $primaryColor;display:block;">""" else ""}
                        ${if (isPremium && qrCodeUrl.isNotBlank()) "<div style='text-align:right;margin-top:4px;'><img src='" + qrCodeUrl + "' style='width:70px;height:70px;border-radius:6px;border:1px solid " + primaryColor + ";'><br><span style='font-size:9px;color:" + primaryColor + ";font-weight:bold;'>Scan to Pay</span></div>" else ""}
                    </div>
                </div>

                <!-- Banner -->
                <div class="orange-banner">
                    <div>
                        <div class="banner-title">INVOICE</div>
                        <div class="banner-subtitle">[${escape(invoice.invoiceSubtitle.ifBlank { "TAX INVOICE" })}]</div>
                    </div>
                    <table class="meta-table">
                        <tr><td><strong>Invoice No</strong></td><td>: #${escape(invoice.invoiceNo)}</td></tr>
                        <tr><td><strong>Invoice Date</strong></td><td>: $dateFormatted</td></tr>
                        ${if (dueDateFormatted.isNotBlank()) "<tr><td><strong>Due Date</strong></td><td>: " + dueDateFormatted + "</td></tr>" else ""}
                        <tr><td><strong>Payment Mode</strong></td><td>: ${escape(invoice.paymentMethod)}</td></tr>
                    </table>
                    <div>
                        <span class="status-pill">${escape(invoice.effectiveStatus)}</span>
                    </div>
                </div>

                <!-- Customer Details & Shipping Grid -->
                ${if (hasShipTo) """
                <div class="grid-two">
                    <div class="info-box">
                        <div class="box-header">👤 BILL TO (CUSTOMER DETAILS)</div>
                        <div style="font-size:12px;line-height:1.6;">
                            <strong>${escape(invoice.customerName.ifBlank { "Customer" })}</strong><br>
                            ${if (invoice.customerPhone.isNotBlank()) "📞 " + escape(invoice.customerPhone) + "<br>" else ""}
                            ${if (invoice.customerEmail.isNotBlank()) "✉️ " + escape(invoice.customerEmail) + "<br>" else ""}
                            ${if (invoice.customerAddress.isNotBlank()) "📍 " + escape(invoice.customerAddress) + "<br>" else ""}
                            ${if (invoice.customerCompany.isNotBlank()) "🏢 " + escape(invoice.customerCompany) + "<br>" else ""}
                            ${if (invoice.customerGstin.isNotBlank()) "<strong>GSTIN:</strong> " + escape(invoice.customerGstin) else ""}
                        </div>
                    </div>

                    <div class="info-box">
                        <div class="box-header">🚚 SHIP TO / DELIVERY DETAILS</div>
                        <div style="font-size:12px;line-height:1.6;">
                            <strong>${escape(invoice.shipToName.ifBlank { invoice.customerName })}</strong><br>
                            ${if (invoice.shipToPhone.isNotBlank()) "📞 " + escape(invoice.shipToPhone) + "<br>" else if (invoice.customerPhone.isNotBlank()) "📞 " + escape(invoice.customerPhone) + "<br>" else ""}
                            ${if (invoice.shipToAddress.isNotBlank()) "📍 " + escape(invoice.shipToAddress) + "<br>" else ""}
                            Delivery Date: $dateFormatted<br>
                            Delivery Mode: Courier / Transport
                        </div>
                    </div>
                </div>
                """.trimIndent() else """
                <div style="padding: 15px 20px;">
                    <div class="info-box" style="width:100%;box-sizing:border-box;">
                        <div class="box-header">👤 BILL TO (CUSTOMER DETAILS)</div>
                        <div style="font-size:12px;line-height:1.6;">
                            <strong>${escape(invoice.customerName.ifBlank { "Customer" })}</strong><br>
                            ${if (invoice.customerPhone.isNotBlank()) "📞 " + escape(invoice.customerPhone) + "<br>" else ""}
                            ${if (invoice.customerEmail.isNotBlank()) "✉️ " + escape(invoice.customerEmail) + "<br>" else ""}
                            ${if (invoice.customerAddress.isNotBlank()) "📍 " + escape(invoice.customerAddress) + "<br>" else ""}
                            ${if (invoice.customerCompany.isNotBlank()) "🏢 " + escape(invoice.customerCompany) + "<br>" else ""}
                            ${if (invoice.customerGstin.isNotBlank()) "<strong>GSTIN:</strong> " + escape(invoice.customerGstin) else ""}
                        </div>
                    </div>
                </div>
                """.trimIndent()}

                <!-- Items Table -->
                <table class="items-table">
                    <thead>
                        <tr>
                            <th style="text-align:center;">S.No</th>
                            <th>Item / Service Description</th>
                            <th style="text-align:center;">HSN/SAC</th>
                            <th style="text-align:center;">Qty</th>
                            <th style="text-align:right;">Unit Price (${currency})</th>
                            <th style="text-align:right;">Amount (${currency})</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>

                <!-- Summary & Payment Row -->
                <div class="summary-grid">
                    ${if (hasPaymentDetails) """
                    <div class="payment-box">
                        <div class="box-header">💳 PAYMENT DETAILS</div>
                        <div style="display:flex;gap:12px;align-items:center;">
                            ${if (qrCodeUrl.isNotBlank()) "<img src='" + qrCodeUrl + "' style='width:90px;height:90px;border:1px solid " + primaryColor + ";padding:2px;border-radius:6px;'>" else ""}
                            <div style="font-size:11px;line-height:1.5;">
                                ${if (invoice.upiId.isNotBlank()) "<strong>UPI ID:</strong> " + escape(invoice.upiId) + "<br>" else ""}
                                ${if (invoice.bankAccountName.isNotBlank()) "<strong>Account Name:</strong> " + escape(invoice.bankAccountName) + "<br>" else ""}
                                ${if (invoice.bankName.isNotBlank()) "<strong>Bank Name:</strong> " + escape(invoice.bankName) + "<br>" else ""}
                                ${if (invoice.bankAccountNo.isNotBlank()) "<strong>Account No:</strong> " + escape(invoice.bankAccountNo) + "<br>" else ""}
                                ${if (invoice.bankIfsc.isNotBlank()) "<strong>IFSC Code:</strong> " + escape(invoice.bankIfsc) else ""}
                            </div>
                        </div>
                        <div style="margin-top:8px;font-size:10px;color:${primaryColor};font-weight:800;">
                            GPay • PhonePe • Paytm • BHIM • UPI
                        </div>
                    </div>
                    """.trimIndent() else ""}

                    <div class="calc-box" style="${if (!hasPaymentDetails) "flex:1;max-width:320px;margin-left:auto;" else ""}">
                        <div style="display:flex;justify-content:space-between;padding:3px 0;font-size:12px;">
                            <span>Subtotal</span>
                            <span>${FormatUtils.formatMoney(invoice.subtotal, currency)}</span>
                        </div>
                        ${if (invoice.discount > 0) "<div style='display:flex;justify-content:space-between;padding:3px 0;font-size:12px;color:#e53e3e;'><span>Discount</span><span>-" + FormatUtils.formatMoney(invoice.discount, currency) + "</span></div>" else ""}
                        ${if (invoice.cgst > 0) "<div style='display:flex;justify-content:space-between;padding:3px 0;font-size:11px;'><span>CGST</span><span>" + FormatUtils.formatMoney(invoice.cgst, currency) + "</span></div>" else ""}
                        ${if (invoice.sgst > 0) "<div style='display:flex;justify-content:space-between;padding:3px 0;font-size:11px;'><span>SGST</span><span>" + FormatUtils.formatMoney(invoice.sgst, currency) + "</span></div>" else ""}
                        ${if (invoice.igst > 0) "<div style='display:flex;justify-content:space-between;padding:3px 0;font-size:11px;'><span>IGST</span><span>" + FormatUtils.formatMoney(invoice.igst, currency) + "</span></div>" else ""}

                        <div class="grand-total">
                            <span>Grand Total</span>
                            <span>${FormatUtils.formatMoney(invoice.total, currency)}</span>
                        </div>
                    </div>
                </div>

                <!-- Footer Terms & Signature -->
                ${if (hasTerms || hasSignatureOrStamp) """
                <div class="footer-grid">
                    ${if (hasTerms) """
                    <div style="flex:1.2;">
                        <div style="font-size:12px;font-weight:800;color:$primaryColor;margin-bottom:4px;">📜 TERMS & CONDITIONS</div>
                        $termsHtml
                    </div>
                    """.trimIndent() else "<div style='flex:1.2;'></div>"}

                    ${if (hasSignatureOrStamp) """
                    <div style="flex:1;text-align:right;">
                        ${if (invoice.stampUri.isNotBlank()) "<img src='" + invoice.stampUri + "' style='max-height:50px;margin-bottom:4px;'><br>" else ""}
                        ${if (invoice.signatureUri.isNotBlank()) "<img src='" + invoice.signatureUri + "' style='max-height:45px;'><br>" else ""}
                        <div style="border-top:1px solid $primaryColor;display:inline-block;padding-top:4px;margin-top:6px;min-width:140px;">
                            <strong style="font-size:11px;">${escape(invoice.signatoryName.ifBlank { "Authorized Signatory" })}</strong><br>
                            <span style="font-size:10px;color:$primaryColor;">[${escape(invoice.signatoryDesignation.ifBlank { "Authorized Signatory" })}]</span>
                        </div>
                    </div>
                    """.trimIndent() else ""}
                </div>
                """.trimIndent() else ""}

                <!-- Trust Bar -->
                <div class="trust-bar">
                    <div>✓ TRUSTED SERVICE</div>
                    <div>🚚 ON-TIME DELIVERY</div>
                    <div>⭐ GENUINE PRODUCTS</div>
                    <div>🎧 DEDICATED SUPPORT</div>
                    <div class="thankyou-text">Thank You For Your Business! ❤</div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun generateReceiptHtml(receipt: ReceiptEntity): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Receipt #${escape(receipt.receiptNo)}</title>
        </head>
        <body style="font-family: sans-serif; padding: 30px;">
            <h2>${escape(receipt.businessName)}</h2>
            <h3>Payment Receipt #${escape(receipt.receiptNo)}</h3>
            <p>Received from: <strong>${escape(receipt.customerName)}</strong></p>
            <h1>Amount: ${FormatUtils.formatMoney(receipt.amount)}</h1>
            <p>Method: ${escape(receipt.method)}</p>
            <p>Date: ${FormatUtils.formatDate(receipt.date)}</p>
        </body>
        </html>
        """.trimIndent()
    }

    private fun escape(s: String?): String {
        return (s ?: "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
