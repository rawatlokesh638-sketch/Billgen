package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessProfile
import com.example.data.model.ProductEntity
import com.example.data.model.QuotationEntity
import com.example.data.model.ReceiptEntity
import com.example.ui.BillGenViewModel
import com.example.ui.monetization.UserSubscriptionStatusCard
import com.example.ui.theme.BillGenOrange
import com.example.util.FormatUtils
import com.example.util.InvoicePrintHelper
import com.example.util.SharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreToolsScreen(
    viewModel: BillGenViewModel,
    onOpenProUpgrade: () -> Unit,
    initialSection: String = "catalog"
) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    var selectedSection by remember { mutableStateOf(initialSection) } // catalog, receipts, quotes, ai, settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Tools & Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = when (selectedSection) {
                    "catalog" -> 0
                    "receipts" -> 1
                    "quotes" -> 2
                    "plans" -> 3
                    "ai" -> 4
                    "admin" -> 5
                    else -> 6
                },
                edgePadding = 16.dp
            ) {
                Tab(selected = selectedSection == "catalog", onClick = { selectedSection = "catalog" }, text = { Text("📦 Catalog") })
                Tab(selected = selectedSection == "receipts", onClick = { selectedSection = "receipts" }, text = { Text("🧾 Receipts") })
                Tab(selected = selectedSection == "quotes", onClick = { selectedSection = "quotes" }, text = { Text("📋 Quotations") })
                Tab(selected = selectedSection == "plans", onClick = { selectedSection = "plans" }, text = { Text("👑 Plans & Buy") })
                Tab(selected = selectedSection == "ai", onClick = { selectedSection = "ai" }, text = { Text("🤖 AI Assistant") })
                if (isAdmin) {
                    Tab(selected = selectedSection == "admin", onClick = { selectedSection = "admin" }, text = { Text("🔑 Admin") })
                }
                Tab(selected = selectedSection == "settings", onClick = { selectedSection = "settings" }, text = { Text("⚙ Settings") })
            }

            when (selectedSection) {
                "catalog" -> ProductCatalogSection(viewModel)
                "receipts" -> PaymentReceiptsSection(viewModel)
                "quotes" -> QuotationsSection(viewModel)
                "plans" -> SubscriptionPlansContent(viewModel)
                "ai" -> AiAssistantSection(viewModel)
                "admin" -> if (isAdmin) AdminPanelSection(viewModel) else ProductCatalogSection(viewModel)
                "settings" -> SettingsSection(viewModel, onOpenProUpgrade)
            }
        }
    }
}

@Composable
fun ProductCatalogSection(viewModel: BillGenViewModel) {
    val products by viewModel.allProducts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newStock by remember { mutableStateOf("") }
    var newHsn by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Product Catalog & Stock (${products.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Product", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("₹${p.price} • HSN: ${p.hsn.ifBlank { "N/A" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (p.isOutOfStock) Color(0xFFFFE4E4) else if (p.isLowStock) Color(0xFFFFF8DB) else Color(0xFFE9F8F0),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (p.isOutOfStock) "Out of Stock" else "Stock: ${p.stock}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (p.isOutOfStock) Color(0xFFD94732) else if (p.isLowStock) Color(0xFFD97706) else Color(0xFF15945B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.restockProduct(p.id, 10) }) {
                                Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Restock", tint = BillGenOrange)
                            }
                            IconButton(onClick = { viewModel.deleteProduct(p) }) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFD94732))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Product to Catalog") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Product Name *") }, singleLine = true)
                    OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, label = { Text("Unit Price (₹) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = newStock, onValueChange = { newStock = it }, label = { Text("Starting Stock Count") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = newHsn, onValueChange = { newHsn = it }, label = { Text("HSN/SAC Code (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pr = newPrice.toDoubleOrNull() ?: 0.0
                        val st = newStock.toIntOrNull() ?: 0
                        if (newName.isNotBlank() && pr > 0) {
                            viewModel.addOrUpdateProduct(ProductEntity(name = newName, price = pr, stock = st, hsn = newHsn))
                            newName = ""; newPrice = ""; newStock = ""; newHsn = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) { Text("Save Product") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PaymentReceiptsSection(viewModel: BillGenViewModel) {
    val receipts by viewModel.allReceipts.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Verified Payment Receipts (${receipts.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(10.dp))

        if (receipts.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No receipts issued yet. Record payments from the Udhaar screen to generate receipts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(receipts) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(r.receiptNo, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF15945B))
                                    Text("From: ${r.customerName} • ${FormatUtils.formatDate(r.date)}", fontSize = 12.sp)
                                    Text("Method: ${r.method} ${if (r.invoiceNo.isNotBlank()) "• Inv: #" + r.invoiceNo else ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("₹${r.amount}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF15945B))
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = {
                                        activity?.let { InvoicePrintHelper.printReceipt(it, r) }
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Print", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val text = "Payment Receipt #${r.receiptNo}\nFrom: ${r.businessName}\nCustomer: ${r.customerName}\nAmount: ₹${r.amount}\nMethod: ${r.method}\nStatus: Received with thanks."
                                        SharingHelper.shareToWhatsApp(context, text)
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuotationsSection(viewModel: BillGenViewModel) {
    val quotations by viewModel.allQuotations.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Quotations & Estimates (${quotations.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(10.dp))

        if (quotations.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No quotations yet. In the Invoice Editor, select 'Quotation' style to create estimates.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quotations) { q ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(q.quoteNo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2563EB))
                                    Text("For: ${q.customerName} • ${FormatUtils.formatDate(q.date)}", fontSize = 12.sp)
                                }
                                Text("₹${q.total}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF2563EB))
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = {
                                        viewModel.convertQuotationToInvoice(q)
                                        Toast.makeText(context, "Quotation converted to tax invoice!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Convert to Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.deleteQuotation(q) }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFD94732))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiAssistantSection(viewModel: BillGenViewModel) {
    var promptInput by remember { mutableStateOf("") }
    var checkInput by remember { mutableStateOf("Quantity = 3\nPrice = ₹599\nEntered total = ₹1497") }
    val nlpResult by viewModel.aiNlpResult.collectAsState()
    val checkResult by viewModel.aiCheckResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🗣 Natural Language Order Builder", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Type or paste WhatsApp customer messages in Hindi or English.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("e.g. Amit ne 3 shirt 599 ki aur 2 jeans 999 ki li, discount 100") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.runAiAssistantOrder(promptInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Build Invoice from Message", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (nlpResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                            Text(nlpResult!!, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("⚠️ AI Math & Discrepancy Checker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Verify calculations and check for math discrepancies.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = checkInput,
                        onValueChange = { checkInput = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.runDiscrepancyCheck(checkInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Check Calculation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (checkResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = Color(0xFFFFF0E9), shape = RoundedCornerShape(8.dp)) {
                            Text(checkResult!!, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC04C1D), modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    viewModel: BillGenViewModel,
    onOpenProUpgrade: () -> Unit
) {
    val profile by viewModel.businessProfile.collectAsState()
    val isPro = viewModel.retentionHelper.isProUser
    val context = LocalContext.current

    var name by remember(profile) { mutableStateOf(profile.businessName) }
    var tagline by remember(profile) { mutableStateOf(profile.businessTagline) }
    var address by remember(profile) { mutableStateOf(profile.businessAddress) }
    var phone by remember(profile) { mutableStateOf(profile.businessPhone) }
    var email by remember(profile) { mutableStateOf(profile.businessEmail) }
    var gstin by remember(profile) { mutableStateOf(profile.businessGstin) }
    var upi by remember(profile) { mutableStateOf(profile.upiId) }
    var bank by remember(profile) { mutableStateOf(profile.bankDetails) }
    var prefix by remember(profile) { mutableStateOf(profile.invoicePrefix) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            UserSubscriptionStatusCard(
                viewModel = viewModel,
                onOpenUpgrade = onOpenProUpgrade
            )
        }

        item {
            val userEmail = viewModel.userEmail
            val isLoggedIn = viewModel.isUserLoggedIn
            val isSyncing by viewModel.isCloudSyncing.collectAsState()
            val syncStatus by viewModel.cloudSyncStatus.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFF15945B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Firebase Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (isLoggedIn && userEmail.isNotBlank()) userEmail else "Guest Mode (Local Only)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFE9F8F0),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "billgen-cc831",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15945B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = syncStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.syncDataFromCloud()
                                Toast.makeText(context, "Syncing from Firebase...", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B))
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download Cloud", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.uploadAllLocalDataToCloud()
                                Toast.makeText(context, "Backing up to Firebase...", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup Local", fontSize = 11.sp)
                        }
                    }

                    if (isLoggedIn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.signOutUser()
                                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Log Out / Switch Account", fontSize = 11.sp, color = Color(0xFFD94732))
                        }
                    }
                }
            }
        }

        item {
            Text("Business Profile & Invoicing Defaults", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        item {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Shop / Brand Name") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Business Tagline") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Business Address") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = gstin, onValueChange = { gstin = it }, label = { Text("GSTIN") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = upi, onValueChange = { upi = it }, label = { Text("UPI ID") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            OutlinedTextField(value = bank, onValueChange = { bank = it }, label = { Text("Bank Details (Bank, A/C, IFSC)") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = prefix, onValueChange = { prefix = it }, label = { Text("Invoice Prefix (e.g. INV)") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            Button(
                onClick = {
                    viewModel.updateBusinessProfile(
                        profile.copy(
                            businessName = name,
                            businessTagline = tagline,
                            businessAddress = address,
                            businessPhone = phone,
                            businessEmail = email,
                            businessGstin = gstin,
                            upiId = upi,
                            bankDetails = bank,
                            invoicePrefix = prefix
                        )
                    )
                    Toast.makeText(context, "Business profile saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
            ) {
                Text("Save Business Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminPanelSection(viewModel: BillGenViewModel) {
    val requests by viewModel.allAdminSubscriptionRequests.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshAdminSubscriptionRequests()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Firebase Admin Configuration Instructions", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "To manually elevate yourself or any user account to lifetime Pro / Pro Plus in the Firebase Console, follow these quick steps:\n\n" +
                                "1. Open your Firebase Console web browser page.\n" +
                                "2. Go to Realtime Database -> Realtime Database nodes.\n" +
                                "3. Locate or create the node:\n" +
                                "   users / {userId} / subscriptions / {subscriptionId}\n" +
                                "4. Inside this node, set / write these key-value fields:\n" +
                                "   • \"status\": \"APPROVED\"\n" +
                                "   • \"planType\": \"PRO\" (or \"PRO_PLUS\" or \"PREMIUM\")\n" +
                                "   • \"expiresAt\": 4102444800000 (Lifetime expiry)\n" +
                                "5. You can also view or edit pending payments submitted by users directly under 'admin_subscriptions' node.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pending Subscription Approvals (${requests.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                IconButton(onClick = { 
                    viewModel.refreshAdminSubscriptionRequests()
                    Toast.makeText(context, "Refreshed list!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = BillGenOrange)
                }
            }
        }

        if (requests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pending or registered subscription requests found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(requests) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("User: ${req.userEmail}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("ID: ${req.userId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                color = when (req.status) {
                                    "APPROVED" -> Color(0xFFE9F8F0)
                                    "REJECTED" -> Color(0xFFFFECEB)
                                    else -> Color(0xFFFFF3CD)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = req.status,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (req.status) {
                                        "APPROVED" -> Color(0xFF15945B)
                                        "REJECTED" -> Color(0xFFD94732)
                                        else -> Color(0xFF856404)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Plan: ${req.planType} (${req.billingCycle})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("UTR / Ref: ${req.utrNumber}", fontSize = 11.sp)
                        Text("UPI Sender Phone: ${req.paymentPhone}", fontSize = 11.sp)
                        Text("Amount Paid: ₹${req.amount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15945B))

                        if (req.adminNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    "Note: ${req.adminNote}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        if (req.status == "PENDING") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.adminApproveSubscriptionRequest(req, "Verified and Approved")
                                        Toast.makeText(context, "Request Approved!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B)),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve Pro", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.adminRejectSubscriptionRequest(req, "Incorrect UTR / Reference No")
                                        Toast.makeText(context, "Request Rejected", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD94732)),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFD94732))
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject", fontSize = 11.sp, color = Color(0xFFD94732))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
