package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import coil.compose.AsyncImage
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
    onNavigateToHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val clipboardManager = LocalClipboardManager.current
    val converters = remember { Converters() }

    val livePreviewInvoice by viewModel.activePreviewInvoice.collectAsState()
    val curInvoice = livePreviewInvoice ?: invoice

    val items = remember(curInvoice.itemsJson) { converters.toLineItemList(curInvoice.itemsJson) }

    var selectedTemplate by remember(curInvoice.templateName) { mutableStateOf(curInvoice.templateName) }
    var selectedTheme by remember(curInvoice.themeColor) { mutableStateOf(curInvoice.themeColor) }

    var showShipToDialog by remember { mutableStateOf(false) }
    var showGstinDialog by remember { mutableStateOf(false) }

    var editShipToName by remember { mutableStateOf(curInvoice.shipToName) }
    var editShipToPhone by remember { mutableStateOf(curInvoice.shipToPhone) }
    var editShipToAddress by remember { mutableStateOf(curInvoice.shipToAddress) }
    var editGstinInput by remember { mutableStateOf(curInvoice.businessGstin) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updatePreviewInvoiceLogo(it.toString())
            Toast.makeText(context, "Logo updated on Invoice!", Toast.LENGTH_SHORT).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updatePreviewInvoicePhoto(it.toString())
            Toast.makeText(context, "Business Photo updated on Invoice!", Toast.LENGTH_SHORT).show()
        }
    }

    val primaryColor = when {
        selectedTemplate.lowercase() == "premium" -> Color(0xFFC59B27)
        selectedTemplate.lowercase() == "pro_plus" || selectedTemplate.lowercase() == "proplus" -> Color(0xFF00875A)
        selectedTemplate.lowercase() == "pro" -> Color(0xFF0052CC)
        else -> when (selectedTheme) {
            "green" -> Color(0xFF15945B)
            "blue" -> Color(0xFF2563EB)
            "purple" -> Color(0xFF6D5CE7)
            "rose" -> Color(0xFFE14F78)
            "dark" -> Color(0xFF17191D)
            "minimal" -> Color(0xFF222222)
            else -> BillGenOrange
        }
    }

    val paperBackground = if (selectedTemplate == "dark" || selectedTemplate == "premium") Color(0xFF17191D) else Color.White
    val paperTextColor = if (selectedTemplate == "dark" || selectedTemplate == "premium") Color.White else Color(0xFF111827)
    val paperMutedColor = if (selectedTemplate == "dark" || selectedTemplate == "premium") Color(0xFFA0A5B0) else Color(0xFF6B7280)

    val templatesList = listOf(
        "pro" to "⭐ Pro Clean",
        "pro_plus" to "⚡ Pro Plus Modern",
        "premium" to "👑 Premium Gold",
        "modern" to "Modern",
        "gst" to "GST Tax",
        "retail" to "Retail",
        "dark" to "Dark Mode"
    )

    // Ship To is ONLY visible if delivery address or receiver name is present
    val hasShipTo = curInvoice.shipToAddress.isNotBlank() || (curInvoice.shipToName.isNotBlank() && curInvoice.shipToName != curInvoice.customerName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice #${curInvoice.invoiceNo}", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditInvoice(curInvoice) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = BillGenOrange)
                    }
                    IconButton(onClick = {
                        val summary = SharingHelper.buildInvoiceSummaryText(curInvoice, items)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Save Button
                    Button(
                        onClick = {
                            val updated = curInvoice.copy(templateName = selectedTemplate, themeColor = selectedTheme)
                            viewModel.saveInvoice(updated)
                            Toast.makeText(context, "Invoice #${updated.invoiceNo} Saved & Synced!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_save_invoice"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Print Button
                    Button(
                        onClick = {
                            if (activity != null) {
                                InvoicePrintHelper.printInvoice(
                                    activity,
                                    curInvoice.copy(templateName = selectedTemplate, themeColor = selectedTheme),
                                    items
                                )
                            } else {
                                Toast.makeText(context, "Printer Error: No printer linked or connected.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_print_pdf"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Share Button
                    Button(
                        onClick = {
                            val text = SharingHelper.buildInvoiceSummaryText(curInvoice, items)
                            SharingHelper.shareToWhatsApp(context, text, curInvoice.customerPhone)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_share_whatsapp"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // History Button
                    Button(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_history_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "History", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("History", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            // Template Selector
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
                                .clickable {
                                    selectedTemplate = key
                                    val updated = curInvoice.copy(templateName = key, themeColor = selectedTheme)
                                    viewModel.saveInvoice(updated)
                                }
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

            // Quick Actions to directly insert Ship To, Logo, Store Photo, GSTIN if not present
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "⚡ Quick Customization for this Bill:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = hasShipTo,
                                onClick = {
                                    editShipToName = curInvoice.shipToName
                                    editShipToPhone = curInvoice.shipToPhone
                                    editShipToAddress = curInvoice.shipToAddress
                                    showShipToDialog = true
                                },
                                label = { Text(if (hasShipTo) "🚚 Ship To Added ✓" else "+ Add Ship To", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = curInvoice.logoUri.isNotBlank(),
                                onClick = {
                                    logoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                label = { Text(if (curInvoice.logoUri.isNotBlank()) "🖼️ Logo Added ✓" else "+ Add Logo", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = curInvoice.businessPhotoUri.isNotBlank(),
                                onClick = {
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                label = { Text(if (curInvoice.businessPhotoUri.isNotBlank()) "🏪 Photo Added ✓" else "+ Store Photo", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = curInvoice.businessGstin.isNotBlank(),
                                onClick = {
                                    editGstinInput = curInvoice.businessGstin
                                    showGstinDialog = true
                                },
                                label = { Text(if (curInvoice.businessGstin.isNotBlank()) "GSTIN: ${curInvoice.businessGstin}" else "+ Add GSTIN", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Colors and Due Reminder
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
                            .clickable {
                                selectedTheme = name
                                val updated = curInvoice.copy(templateName = selectedTemplate, themeColor = name)
                                viewModel.saveInvoice(updated)
                            }
                            .then(
                                if (selectedTheme == name) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (curInvoice.pendingAmount > 0) {
                    TextButton(
                        onClick = {
                            val reminder = SharingHelper.buildPaymentReminderText(
                                customerName = curInvoice.customerName,
                                invoiceNo = curInvoice.invoiceNo,
                                pendingAmount = curInvoice.pendingAmount,
                                currency = curInvoice.currency,
                                upiId = curInvoice.upiId,
                                businessName = curInvoice.businessName
                            )
                            SharingHelper.shareToWhatsApp(context, reminder, curInvoice.customerPhone)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD94732))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Due Reminder", fontSize = 11.sp, color = Color(0xFFD94732), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Paper Preview
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
                            if (curInvoice.logoUri.isNotBlank()) {
                                AsyncImage(
                                    model = curInvoice.logoUri,
                                    contentDescription = "Business Logo",
                                    modifier = Modifier
                                        .height(50.dp)
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                            Text(
                                text = curInvoice.businessName.ifBlank { "My Business" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = paperTextColor
                            )
                            if (curInvoice.businessTagline.isNotBlank()) {
                                Text(
                                    text = curInvoice.businessTagline,
                                    fontSize = 10.sp,
                                    color = paperMutedColor
                                )
                            }
                            if (curInvoice.businessAddress.isNotBlank()) {
                                Text(
                                    text = curInvoice.businessAddress,
                                    fontSize = 10.sp,
                                    color = paperMutedColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            if (curInvoice.businessGstin.isNotBlank()) {
                                Text(
                                    text = "GSTIN: ${curInvoice.businessGstin}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = paperMutedColor
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (curInvoice.businessPhotoUri.isNotBlank()) {
                                AsyncImage(
                                    model = curInvoice.businessPhotoUri,
                                    contentDescription = "Store Photo",
                                    modifier = Modifier
                                        .height(55.dp)
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, primaryColor, RoundedCornerShape(6.dp))
                                )
                            }
                            Text(
                                text = if (selectedTemplate == "quotation") "QUOTATION" else "INVOICE",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryColor
                            )
                            Text(
                                text = "#${curInvoice.invoiceNo}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = paperTextColor
                            )
                            Text(
                                text = "Date: ${FormatUtils.formatDate(curInvoice.date)}",
                                fontSize = 10.sp,
                                color = paperMutedColor
                            )
                            if (curInvoice.dueDate.isNotBlank()) {
                                Text(
                                    text = "Due: ${FormatUtils.formatDate(curInvoice.dueDate)}",
                                    fontSize = 10.sp,
                                    color = paperMutedColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                    // Customer and Optional Ship To
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BILL TO", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = paperMutedColor)
                            Text(
                                text = curInvoice.customerName.ifBlank { "Customer" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = paperTextColor
                            )
                            if (curInvoice.customerPhone.isNotBlank()) {
                                Text("📞 ${curInvoice.customerPhone}", fontSize = 10.sp, color = paperMutedColor)
                            }
                            if (curInvoice.customerAddress.isNotBlank()) {
                                Text("📍 ${curInvoice.customerAddress}", fontSize = 10.sp, color = paperMutedColor)
                            }
                            if (curInvoice.customerGstin.isNotBlank()) {
                                Text("GSTIN: ${curInvoice.customerGstin}", fontSize = 10.sp, color = paperMutedColor)
                            }
                        }

                        // Ship To Box: ONLY RENDERED IF DATA WAS ENTERED!
                        if (hasShipTo) {
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("SHIP TO", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = paperMutedColor)
                                Text(
                                    text = curInvoice.shipToName.ifBlank { curInvoice.customerName },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = paperTextColor
                                )
                                if (curInvoice.shipToPhone.isNotBlank()) {
                                    Text("📞 ${curInvoice.shipToPhone}", fontSize = 10.sp, color = paperMutedColor)
                                }
                                if (curInvoice.shipToAddress.isNotBlank()) {
                                    Text("📍 ${curInvoice.shipToAddress}", fontSize = 10.sp, color = paperMutedColor)
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = paperMutedColor)
                            Surface(
                                color = if (curInvoice.effectiveStatus == "PAID") Color(0xFFE9F8F0) else Color(0xFFFFF0E9),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = curInvoice.effectiveStatus,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (curInvoice.effectiveStatus == "PAID") Color(0xFF15945B) else Color(0xFFC04C1D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text("Method: ${curInvoice.paymentMethod}", fontSize = 10.sp, color = paperMutedColor, modifier = Modifier.padding(top = 2.dp))
                            if (curInvoice.pendingAmount > 0) {
                                Text(
                                    text = "Due: " + FormatUtils.formatMoney(curInvoice.pendingAmount, curInvoice.currency),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC04C1D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = if (selectedTemplate == "dark" || selectedTemplate == "premium") Color(0xFF22262E) else Color(0xFFF9FAFB),
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
                            Text(FormatUtils.formatMoney(item.price, curInvoice.currency), fontSize = 11.sp, color = paperTextColor, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                            Text(FormatUtils.formatMoney(item.total, curInvoice.currency), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = paperTextColor, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
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
                        CalculationRow("Subtotal", FormatUtils.formatMoney(curInvoice.subtotal, curInvoice.currency), paperTextColor)
                        if (curInvoice.discount > 0) {
                            CalculationRow("Discount", "- " + FormatUtils.formatMoney(curInvoice.discount, curInvoice.currency), Color(0xFFC04C1D))
                        }
                        if (curInvoice.gstRate > 0 && curInvoice.gstType != "none") {
                            CalculationRow("Taxable", FormatUtils.formatMoney(curInvoice.taxableAmount, curInvoice.currency), paperMutedColor)
                            if (curInvoice.gstType == "intra") {
                                CalculationRow("CGST (${curInvoice.gstRate / 2}%)", FormatUtils.formatMoney(curInvoice.cgst, curInvoice.currency), paperMutedColor)
                                CalculationRow("SGST (${curInvoice.gstRate / 2}%)", FormatUtils.formatMoney(curInvoice.sgst, curInvoice.currency), paperMutedColor)
                            } else {
                                CalculationRow("IGST (${curInvoice.gstRate}%)", FormatUtils.formatMoney(curInvoice.igst, curInvoice.currency), paperMutedColor)
                            }
                        }
                        if (curInvoice.shipping > 0) {
                            CalculationRow("Shipping", FormatUtils.formatMoney(curInvoice.shipping, curInvoice.currency), paperTextColor)
                        }
                        if (curInvoice.roundoff != 0.0) {
                            CalculationRow("Roundoff", FormatUtils.formatMoney(curInvoice.roundoff, curInvoice.currency), paperTextColor)
                        }

                        HorizontalDivider(modifier = Modifier.width(220.dp).padding(vertical = 6.dp), color = primaryColor)

                        Row(
                            modifier = Modifier.width(220.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Black, color = primaryColor)
                            Text(FormatUtils.formatMoney(curInvoice.total, curInvoice.currency), fontSize = 18.sp, fontWeight = FontWeight.Black, color = primaryColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (curInvoice.notes.isNotBlank()) {
                                Text(curInvoice.notes, fontSize = 10.sp, color = paperMutedColor)
                            }
                            if (curInvoice.upiId.isNotBlank()) {
                                Text("UPI: ${curInvoice.upiId}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                            if (curInvoice.bankDetails.isNotBlank()) {
                                Text(curInvoice.bankDetails, fontSize = 9.sp, color = paperMutedColor)
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

    // Ship To Dialog
    if (showShipToDialog) {
        AlertDialog(
            onDismissRequest = { showShipToDialog = false },
            title = { Text("Delivery / Ship To Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delivery address dalne par invoice par Ship To box dikhega, khali chhodne par chhip jayega.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = editShipToName,
                        onValueChange = { editShipToName = it },
                        label = { Text("Receiver Name") },
                        placeholder = { Text(curInvoice.customerName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editShipToPhone,
                        onValueChange = { editShipToPhone = it },
                        label = { Text("Receiver Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editShipToAddress,
                        onValueChange = { editShipToAddress = it },
                        label = { Text("Delivery Address") },
                        placeholder = { Text("City, State, Pincode") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePreviewInvoiceShipTo(editShipToName, editShipToPhone, editShipToAddress)
                        showShipToDialog = false
                        Toast.makeText(context, "Shipping details updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    Text("Save Shipping")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Option to clear shipping details
                    editShipToAddress = ""
                    editShipToName = ""
                    editShipToPhone = ""
                    viewModel.updatePreviewInvoiceShipTo("", "", "")
                    showShipToDialog = false
                    Toast.makeText(context, "Shipping cleared (Box hidden)", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Clear (Hide Box)", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // GSTIN Dialog
    if (showGstinDialog) {
        AlertDialog(
            onDismissRequest = { showGstinDialog = false },
            title = { Text("Store GSTIN Number", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GSTIN dalne par invoice par dikhega, khali chhodne par chhip jayega.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = editGstinInput,
                        onValueChange = { editGstinInput = it.uppercase() },
                        label = { Text("GSTIN") },
                        placeholder = { Text("e.g. 07AAAAA0000A1Z5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePreviewInvoiceGstin(editGstinInput.trim())
                        showGstinDialog = false
                        Toast.makeText(context, "GSTIN updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    Text("Save GSTIN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editGstinInput = ""
                    viewModel.updatePreviewInvoiceGstin("")
                    showGstinDialog = false
                    Toast.makeText(context, "GSTIN removed", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        )
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
