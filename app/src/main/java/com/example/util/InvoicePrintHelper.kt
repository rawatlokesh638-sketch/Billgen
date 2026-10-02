package com.example.util

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.InvoiceEntity
import com.example.data.model.LineItem
import com.example.data.model.ReceiptEntity

object InvoicePrintHelper {
    fun printInvoice(
        activity: Activity,
        invoice: InvoiceEntity,
        items: List<LineItem>
    ) {
        val htmlContent = generateInvoiceHtml(invoice, items)
        printHtml(activity, htmlContent, "Invoice_${invoice.invoiceNo}")
    }

    fun printReceipt(
        activity: Activity,
        receipt: ReceiptEntity
    ) {
        val htmlContent = generateReceiptHtml(receipt)
        printHtml(activity, htmlContent, "Receipt_${receipt.receiptNo}")
    }

    private fun printHtml(activity: Activity, htmlContent: String, jobName: String) {
        activity.runOnUiThread {
            val webView = WebView(activity)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("id", "print", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    }

    fun generateInvoiceHtml(invoice: InvoiceEntity, items: List<LineItem>): String {
        val currency = invoice.currency
        val dateFormatted = FormatUtils.formatDate(invoice.date)
        val dueDateFormatted = if (invoice.dueDate.isNotBlank()) FormatUtils.formatDate(invoice.dueDate) else ""
        
        val rows = items.mapIndexed { idx, it ->
            """
            <tr>
                <td style="padding:10px;border-bottom:1px solid #eee;">${idx + 1}</td>
                <td style="padding:10px;border-bottom:1px solid #eee;">
                    <strong>${escape(it.name)}</strong>
                    ${if (it.hsn.isNotBlank()) "<br><small style='color:#777'>HSN: " + escape(it.hsn) + "</small>" else ""}
                </td>
                <td style="padding:10px;border-bottom:1px solid #eee;text-align:center;">${it.qty}</td>
                <td style="padding:10px;border-bottom:1px solid #eee;text-align:right;">${FormatUtils.formatMoney(it.price, currency)}</td>
                <td style="padding:10px;border-bottom:1px solid #eee;text-align:right;"><strong>${FormatUtils.formatMoney(it.total, currency)}</strong></td>
            </tr>
            """.trimIndent()
        }.joinToString("")

        val primaryColor = when (invoice.themeColor) {
            "green" -> "#15945B"
            "blue" -> "#2563EB"
            "purple" -> "#6D5CE7"
            "rose" -> "#E14F78"
            "dark" -> "#17191D"
            "minimal" -> "#222222"
            else -> "#FF6500"
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Invoice #${escape(invoice.invoiceNo)}</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; margin: 0; padding: 25px; color: #111; }
                .invoice-container { max-width: 800px; margin: auto; border: 1px solid #eee; padding: 30px; border-radius: 12px; border-top: 6px solid $primaryColor; }
                .header { display: flex; justify-content: space-between; border-bottom: 2px solid #eee; padding-bottom: 20px; }
                .brand-title { font-size: 24px; font-weight: 900; color: #111; margin: 0; }
                .inv-title { font-size: 28px; font-weight: 900; color: $primaryColor; margin: 0; text-align: right; }
                table { width: 100%; border-collapse: collapse; margin: 20px 0; font-size: 13px; }
                th { background: #f8f9fa; text-align: left; padding: 12px 10px; font-weight: 800; border-bottom: 2px solid #ddd; }
                .summary { margin-left: auto; width: 300px; padding-top: 15px; }
                .total-row { display: flex; justify-content: space-between; font-size: 20px; font-weight: 900; color: $primaryColor; border-top: 2px solid $primaryColor; padding-top: 10px; }
            </style>
        </head>
        <body>
            <div class="invoice-container">
                <div class="header">
                    <div>
                        <h1 class="brand-title">${escape(invoice.businessName)}</h1>
                        <div>${escape(invoice.businessAddress)}<br>${escape(invoice.businessPhone)}</div>
                    </div>
                    <div>
                        <h2 class="inv-title">${if (invoice.templateName == "quotation") "QUOTATION" else "TAX INVOICE"}</h2>
                        <div><strong>#${escape(invoice.invoiceNo)}</strong><br>Date: $dateFormatted</div>
                    </div>
                </div>

                <div style="margin: 20px 0;">
                    <strong>Bill To:</strong><br>
                    ${escape(invoice.customerName)}<br>
                    ${escape(invoice.customerPhone)}
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Item</th>
                            <th style="text-align:center;">Qty</th>
                            <th style="text-align:right;">Price</th>
                            <th style="text-align:right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>

                <div class="summary">
                    <div style="display:flex;justify-content:space-between;padding:4px 0;">
                        <span>Subtotal</span>
                        <span>${FormatUtils.formatMoney(invoice.subtotal, currency)}</span>
                    </div>
                    <div class="total-row">
                        <span>Total</span>
                        <span>${FormatUtils.formatMoney(invoice.total, currency)}</span>
                    </div>
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
