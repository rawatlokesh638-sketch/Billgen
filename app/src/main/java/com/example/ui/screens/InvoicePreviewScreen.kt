package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Converters
import com.example.data.model.InvoiceEntity
import com.example.ui.BillGenViewModel
import com.example.ui.monetization.AdsterraBannerAd
import com.example.ui.monetization.AdsterraNativeAd
import com.example.ui.monetization.EarnKaroNativeAdCard
import com.example.ui.theme.BillGenOrange
import com.example.util.FormatUtils
import com.example.util.InvoicePrintHelper
import com.example.util.SharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewScreen(
    invoice: InvoiceEntity,
    viewModel: BillGenViewModel,
    onEditInvoice: (InvoiceEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val clipboardManager = LocalClipboardManager.current
    val converters = remember { Converters() }
    val items = remember(invoice.itemsJson) { converters.toLineItemList(invoice.itemsJson) }

    var selectedTemplate by remember { mutableStateOf(invoice.templateName) }
    var selectedTheme by remember { mutableStateOf(invoice.themeColor) }

    val primaryColor = when (selectedTheme) {
        "green" -> Color(0xFF15945B)
        "blue" -> Color(0xFF2563EB)
        "purple" -> Color(0xFF6D5CE7)
        "rose" -> Color(0xFFE14F78)
        "dark" -> Color(0xFF17191D)
        "minimal" -> Color(0xFF222222)
        else -> BillGenOrange
    }

    val paperBackground = if (selectedTemplate == "dark") Color(0xFF17191D) else Color.White
    val paperTextColor = if (selectedTemplate == "dark") Color.White else Color(0xFF111827)
    val paperMutedColor = if (selectedTemplate == "dark") Color(0xFFA0A5B0) else Color(0xFF6B7280)

    val templatesList = listOf(
        "modern" to "Modern",
        "classic" to "Classic",
        "minimal" to "Minimal",
        "gst" to "GST Tax",
        "retail" to "Retail",
        "freelancer" to "Freelancer",
        "service" to "Service",
        "restaurant" to "Restaurant",
        "dark" to "Dark Mode",
        "colorful" to "Colorful",
        "premium" to "Premium Gold",
        "ecommerce" to "E-Commerce",
        "small-business" to "Shopkeeper",
        "quotation" to "Quotation",
        "proforma" to "Proforma"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice #${invoice.invoiceNo}", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditInvoice(invoice) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = BillGenOrange)
                    }
                    IconButton(onClick = {
                        val summary = SharingHelper.buildInvoiceSummaryText(invoice, items)
                        clipboardManager.setText(AnnotatedString(summary))
                        Toast.makeText(context, "Invoice summary copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            activity?.let {
                                InvoicePrintHelper.printInvoice(
                                    it,
                                    invoice.copy(templateName = selectedTemplate, themeColor = selectedTheme),
                                    items
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_print_pdf"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print / PDF", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val text = SharingHelper.buildInvoiceSummaryText(invoice, items)
                            SharingHelper.shareToWhatsApp(context, text, invoice.customerPhone)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("action_share_whatsapp"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Invoice Template Design (${templatesList.size} Styles)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(templatesList) { (key, label) ->
                        val isSelected = selectedTemplate == key
                        Surface(
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = if (!isSelected) CardDefaults.outlinedCardBorder() else null,
                            modifier = Modifier
                                .clickable { selectedTemplate = key }
                                .testTag("template_pill_$key")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val themes = listOf(
                    "orange" to BillGenOrange,
                    "green" to Color(0xFF15945B),
                    "blue" to Color(0xFF2563EB),
                    "purple" to Color(0xFF6D5CE7),
                    "rose" to Color(0xFFE14F78),
                    "dark" to Color(0xFF17191D)
                )
                themes.forEach { (name, color) ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedTheme = name }
                            .then(
                                if (selectedTheme == name) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (invoice.pendingAmount > 0) {
                    TextButton(
                        onClick = {
                            val reminder = SharingHelper.buildPaymentReminderText(
                                customerName = invoice.customerName,
                                invoiceNo = invoice.invoiceNo,
                                pendingAmount = invoice.pendingAmount,
                                currency = invoice.currency,
                                upiId = invoice.upiId,
                                businessName = invoice.businessName
                            )
                            SharingHelper.shareToWhatsApp(context, reminder, invoice.customerPhone)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD94732))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Due Reminder", fontSize = 11.sp, color = Color(0xFFD94732), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_paper_canvas"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = paperBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(primaryColor)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = invoice.businessName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = paperTextColor
                            )
                            if (invoice.businessTagline.isNotBlank()) {
                                Text(
                                    text = invoice.businessTagline,
                                    fontSize = 10.sp,
                                    color = paperMutedColor
                                )
                            }
                            Text(
                                text = invoice.businessAddress,
                                fontSize = 10.sp,
                                color = paperMutedColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (invoice.businessGstin.isNotBlank()) {
                                Text(
                                    text = "GSTIN: ${invoice.businessGstin}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = paperMutedColor
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (selectedTemplate == "quotation") "QUOTATION" else "INVOICE",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryColor
                            )
                            Text(
                                text = "#${invoice.invoiceNo}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = paperTextColor
                            )
                            Text(
                                text = "Date: ${FormatUtils.formatDate(invoice.date)}",
                                fontSize = 10.sp,
                                color = paperMutedColor
                            )
                            if (invoice.dueDate.isNotBlank()) {
                                Text(
                                    text = "Due: ${FormatUtils.formatDate(invoice.dueDate)}",
                                    fontSize = 10.sp,
                                    color = paperMutedColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("BILL TO", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = paperMutedColor)
                            Text(
                                text = invoice.customerName.ifBlank { "Valued Customer" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = paperTextColor
                            )
                            if (invoice.customerPhone.isNotBlank()) {
                                Text(invoice.customerPhone, fontSize = 10.sp, color = paperMutedColor)
                            }
                            if (invoice.customerAddress.isNotBlank()) {
                                Text(invoice.customerAddress, fontSize = 10.sp, color = paperMutedColor)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = paperMutedColor)
                            Surface(
                                color = if (invoice.effectiveStatus == "PAID") Color(0xFFE9F8F0) else Color(0xFFFFF0E9),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = invoice.effectiveStatus,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (invoice.effectiveStatus == "PAID") Color(0xFF15945B) else Color(0xFFC04C1D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text("Method: ${invoice.paymentMethod}", fontSize = 10.sp, color = paperMutedColor, modifier = Modifier.padding(top = 2.dp))
                            if (invoice.pendingAmount > 0) {
                                Text(
                                    text = "Due: " + FormatUtils.formatMoney(invoice.pendingAmount, invoice.currency),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC04C1D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = if (selectedTemplate == "dark") Color(0xFF22262E) else Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = paperMutedColor, modifier = Modifier.width(20.dp))
                            Text("Item", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = paperMutedColor, modifier = Modifier.weight(2f))
                            Text("Qty", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = paperMutedColor, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            Text("Rate", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = paperMutedColor, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                            Text("Amount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = paperMutedColor, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                        }
                    }

                    items.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${idx + 1}", fontSize = 11.sp, color = paperMutedColor, modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(2f)) {
                                Text(item.name.ifBlank { "Item" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = paperTextColor)
                                if (item.hsn.isNotBlank()) {
                                    Text("HSN: ${item.hsn}", fontSize = 9.sp, color = paperMutedColor)
                                }
                            }
                            Text("${item.qty}", fontSize = 11.sp, color = paperTextColor, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            Text(FormatUtils.formatMoney(item.price, invoice.currency), fontSize = 11.sp, color = paperTextColor, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                            Text(FormatUtils.formatMoney(item.total, invoice.currency), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = paperTextColor, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        CalculationRow("Subtotal", FormatUtils.formatMoney(invoice.subtotal, invoice.currency), paperTextColor)
                        if (invoice.discount > 0) {
                            CalculationRow("Discount", "- " + FormatUtils.formatMoney(invoice.discount, invoice.currency), Color(0xFFC04C1D))
                        }
                        if (invoice.gstRate > 0 && invoice.gstType != "none") {
                            CalculationRow("Taxable", FormatUtils.formatMoney(invoice.taxableAmount, invoice.currency), paperMutedColor)
                            if (invoice.gstType == "intra") {
                                CalculationRow("CGST (${invoice.gstRate / 2}%)", FormatUtils.formatMoney(invoice.cgst, invoice.currency), paperMutedColor)
                                CalculationRow("SGST (${invoice.gstRate / 2}%)", FormatUtils.formatMoney(invoice.sgst, invoice.currency), paperMutedColor)
                            } else {
                                CalculationRow("IGST (${invoice.gstRate}%)", FormatUtils.formatMoney(invoice.igst, invoice.currency), paperMutedColor)
                            }
                        }
                        if (invoice.shipping > 0) {
                            CalculationRow("Shipping", FormatUtils.formatMoney(invoice.shipping, invoice.currency), paperTextColor)
                        }
                        if (invoice.roundoff != 0.0) {
                            CalculationRow("Roundoff", FormatUtils.formatMoney(invoice.roundoff, invoice.currency), paperTextColor)
                        }

                        HorizontalDivider(modifier = Modifier.width(220.dp).padding(vertical = 6.dp), color = primaryColor)

                        Row(
                            modifier = Modifier.width(220.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Black, color = primaryColor)
                            Text(FormatUtils.formatMoney(invoice.total, invoice.currency), fontSize = 18.sp, fontWeight = FontWeight.Black, color = primaryColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(invoice.notes, fontSize = 10.sp, color = paperMutedColor)
                            if (invoice.upiId.isNotBlank()) {
                                Text("UPI: ${invoice.upiId}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                            if (invoice.bankDetails.isNotBlank()) {
                                Text(invoice.bankDetails, fontSize = 9.sp, color = paperMutedColor)
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✓", fontSize = 24.sp, color = primaryColor, fontFamily = FontFamily.Cursive)
                            Text("Authorized Signatory", fontSize = 9.sp, color = paperMutedColor)
                        }
                    }
                }
            }

            // High CPM Adsterra Native & Banner Ads
            AdsterraNativeAd(isProUser = viewModel.retentionHelper.isProUser)
            AdsterraBannerAd(isProUser = viewModel.retentionHelper.isProUser)
            EarnKaroNativeAdCard(isProUser = viewModel.retentionHelper.isProUser)
        }
    }
}

@Composable
private fun CalculationRow(title: String, amount: String, color: Color) {
    Row(
        modifier = Modifier
            .width(220.dp)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 11.sp, color = color)
        Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
