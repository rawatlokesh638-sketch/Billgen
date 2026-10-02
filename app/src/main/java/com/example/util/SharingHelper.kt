package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.InvoiceEntity
import com.example.data.model.LineItem
import java.net.URLEncoder

object SharingHelper {
    fun buildInvoiceSummaryText(
        invoice: InvoiceEntity,
        items: List<LineItem>
    ): String {
        val date = FormatUtils.formatDate(invoice.date)
        val total = FormatUtils.formatMoney(invoice.total, invoice.currency)
        val pending = FormatUtils.formatMoney(invoice.pendingAmount, invoice.currency)

        return buildString {
            append("🧾 *INVOICE: ${invoice.invoiceNo}*\n")
            append("From: *${invoice.businessName}*\n")
            append("Date: $date\n")
            append("To: ${invoice.customerName.ifBlank { "Valued Customer" }}\n\n")
            append("📦 *Items:*\n")
            items.forEachIndexed { i, item ->
                val lineTotal = FormatUtils.formatMoney(item.total, invoice.currency)
                append("${i + 1}. ${item.name} (${item.qty} × ${FormatUtils.formatMoney(item.price, invoice.currency)}) = $lineTotal\n")
            }
            append("\n💰 *Total: $total*\n")
            if (invoice.pendingAmount > 0) {
                append("⚠️ *Outstanding Due: $pending*\n")
            } else {
                append("✅ *Status: Paid in Full*\n")
            }
            if (invoice.upiId.isNotBlank()) {
                append("💳 *Pay via UPI:* ${invoice.upiId}\n")
            }
            if (invoice.notes.isNotBlank()) {
                append("\n_${invoice.notes}_\n")
            }
            append("Powered by BillGen AI")
        }
    }

    fun buildPaymentReminderText(
        customerName: String,
        invoiceNo: String,
        pendingAmount: Double,
        currency: String = "INR",
        upiId: String = "",
        businessName: String = ""
    ): String {
        val amountStr = FormatUtils.formatMoney(pendingAmount, currency)
        return buildString {
            append("Hello $customerName,\n\n")
            append("This is a gentle payment reminder from *${businessName.ifBlank { "our store" }}*.\n")
            append("Invoice: *#$invoiceNo*\n")
            append("Outstanding Balance: *$amountStr*\n\n")
            if (upiId.isNotBlank()) {
                append("You can pay easily using UPI: *$upiId*\n\n")
            }
            append("Please clear the pending balance at your earliest convenience. Thank you!")
        }
    }

    fun shareToWhatsApp(context: Context, text: String, phoneNumber: String = "") {
        try {
            val cleanPhone = phoneNumber.filter { it.isDigit() }
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = if (cleanPhone.length >= 10) {
                val fullPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                Uri.parse("https://api.whatsapp.com/send?phone=$fullPhone&text=" + URLEncoder.encode(text, "UTF-8"))
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=" + URLEncoder.encode(text, "UTF-8"))
            }
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            openShareSheet(context, text, "Share Invoice")
        }
    }

    fun openShareSheet(context: Context, text: String, title: String = "Share Invoice") {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, title)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share", Toast.LENGTH_SHORT).show()
        }
    }
}
