package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvoiceEntity
import com.example.data.model.LineItem
import com.example.data.model.ProductEntity
import com.example.ui.BillGenViewModel
import com.example.ui.theme.BillGenOrange
import com.example.util.FormatUtils
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.delay
import android.graphics.Bitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    viewModel: BillGenViewModel,
    initialMode: String = "ai",
    onInvoiceGenerated: (InvoiceEntity) -> Unit
) {
    val context = LocalContext.current
    var builderMode by remember { mutableStateOf(initialMode) }
    var pastedText by remember { mutableStateOf("") }
    var showCatalogDialog by remember { mutableStateOf<Int?>(null) }

    var aiSubTab by remember { mutableStateOf("single") } // single, multi, camera
    var autoScanActive by remember { mutableStateOf(false) }

    val isMultiExtracting by viewModel.isMultiExtracting.collectAsState()
    val multiProgressMessage by viewModel.multiProgressMessage.collectAsState()
    val multiGeneratedInvoices by viewModel.multiGeneratedInvoices.collectAsState()

    val isCameraScanning by viewModel.isCameraScanning.collectAsState()
    val cameraScanProgress by viewModel.cameraScanProgress.collectAsState()
    val cameraScanInvoices by viewModel.cameraScanInvoices.collectAsState()

    val customerName by viewModel.editorCustomerName.collectAsState()
    val customerPhone by viewModel.editorCustomerPhone.collectAsState()
    val customerAddress by viewModel.editorCustomerAddress.collectAsState()
    val shipToName by viewModel.editorShipToName.collectAsState()
    val shipToPhone by viewModel.editorShipToPhone.collectAsState()
    val shipToAddress by viewModel.editorShipToAddress.collectAsState()
    val editorLogoUri by viewModel.editorLogoUri.collectAsState()
    val editorPhotoUri by viewModel.editorPhotoUri.collectAsState()
    val editorGstin by viewModel.editorGstin.collectAsState()
    val invoiceNo by viewModel.editorInvoiceNo.collectAsState()
    val date by viewModel.editorDate.collectAsState()
    val dueDate by viewModel.editorDueDate.collectAsState()
    val items by viewModel.editorItems.collectAsState()
    val discount by viewModel.editorDiscount.collectAsState()
    val shipping by viewModel.editorShipping.collectAsState()
    val roundoff by viewModel.editorRoundoff.collectAsState()
    val gstRate by viewModel.editorGstRate.collectAsState()
    val gstType by viewModel.editorGstType.collectAsState()
    val gstMode by viewModel.editorGstMode.collectAsState()
    val paymentStatus by viewModel.editorPaymentStatus.collectAsState()
    val paymentMethod by viewModel.editorPaymentMethod.collectAsState()
    val amountPaid by viewModel.editorAmountPaid.collectAsState()
    val notes by viewModel.editorNotes.collectAsState()
    val currency by viewModel.editorCurrency.collectAsState()

    val isExtracting by viewModel.isExtracting.collectAsState()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsState()
    val selectedBitmap by viewModel.selectedImageBitmap.collectAsState()
    val products by viewModel.allProducts.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    viewModel.setSelectedBitmap(bmp)
                }
            } catch (e: Exception) {}
        }
    }

    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.processMultiScreenshots(context, uris)
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.editorLogoUri.value = it.toString() }
    }

    val storePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.editorPhotoUri.value = it.toString() }
    }

    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraPhotoUri != null) {
            try {
                context.contentResolver.openInputStream(cameraPhotoUri!!)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    viewModel.processCameraScan(bmp, context) { generated ->
                        if (generated != null) {
                            Toast.makeText(context, "Real-time Scan Success! Saved Bill #${generated.invoiceNo}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Scanning failed: No readable invoice text or items found.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load captured photo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val createTempImageUri = remember {
        { ctx: Context ->
            try {
                val tempFile = java.io.File.createTempFile("billgen_capture_", ".jpg", ctx.cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                androidx.core.content.FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.fileprovider",
                    tempFile
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    val calculation = remember(items, discount, shipping, roundoff, gstRate, gstType, gstMode, amountPaid, dueDate) {
        FormatUtils.calculateInvoice(
            items = items,
            discount = discount,
            shipping = shipping,
            roundoff = roundoff,
            gstRate = gstRate,
            gstType = gstType,
            gstMode = gstMode,
            amountPaid = amountPaid,
            dueDate = dueDate
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.editingInvoiceId != null) "Edit Invoice" else "Create Invoice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.initNewInvoice() },
                        modifier = Modifier.testTag("reset_invoice_button")
                    ) {
                        Text("Reset", color = BillGenOrange, fontWeight = FontWeight.Bold)
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
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Grand Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = FormatUtils.formatMoney(calculation.total, currency),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = BillGenOrange
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveCurrentInvoice { saved ->
                                onInvoiceGenerated(saved)
                            }
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("save_and_preview_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate & Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    ModeTabButton(
                        title = "✨ Screenshot / AI",
                        isSelected = builderMode == "ai",
                        modifier = Modifier.weight(1f),
                        onClick = { builderMode = "ai" }
                    )
                    ModeTabButton(
                        title = "🧾 Manual Invoice",
                        isSelected = builderMode == "manual",
                        modifier = Modifier.weight(1f),
                        onClick = { builderMode = "manual" }
                    )
                }
            }

            if (builderMode == "ai") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Sub-Tabs Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    Triple("single", "✨ Single Scan", Icons.Default.AutoAwesome),
                                    Triple("multi", "📸 Multi Scan", Icons.Default.PhotoLibrary),
                                    Triple("camera", "🎥 Live Camera", Icons.Default.Videocam)
                                ).forEach { (tabId, label, icon) ->
                                    val isSelected = aiSubTab == tabId
                                    Surface(
                                        color = if (isSelected) BillGenOrange else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { aiSubTab = tabId }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Credit limits and Plan Tier Badge
                            val currentTier = viewModel.retentionHelper.userPlanTier
                            val credits = viewModel.retentionHelper.aiScanCredits
                            Surface(
                                color = if (currentTier != "FREE") Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (currentTier != "FREE") {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Stars, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Unlimited Pro AI Scans Activated", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF92400E))
                                        }
                                        Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(4.dp)) {
                                            Text("PRO", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Daily AI scan credits: 10 daily", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Remaining: $credits", fontSize = 11.sp, fontWeight = FontWeight.Black, color = BillGenOrange)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sub-Tab Content Rendering
                            when (aiSubTab) {
                                "single" -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Single Screenshot → Invoice", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        TextButton(onClick = {
                                            pastedText = "Amit Sharma\nPhone: 9876543210\nRewari, Haryana\n2 T-shirt 599\n1 Jeans 999\n1 Cap 299\nDiscount 100\nPaid by UPI"
                                        }) {
                                            Text("Load Demo", fontSize = 11.sp, color = BillGenOrange)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (selectedBitmap != null) 140.dp else 100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedBitmap != null) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                                Image(
                                                    bitmap = selectedBitmap!!.asImageBitmap(),
                                                    contentDescription = "Selected Image",
                                                    modifier = Modifier.height(85.dp).clip(RoundedCornerShape(6.dp))
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Screenshot loaded • Tap to change", fontSize = 10.sp, color = BillGenOrange, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BillGenOrange, modifier = Modifier.size(28.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Tap to choose single screenshot or bill photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("WhatsApp screenshot, paper bills, hand bills", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = pastedText,
                                        onValueChange = { pastedText = it },
                                        label = { Text("Or paste order message / text...") },
                                        placeholder = { Text("e.g. Rahul, 2 T-shirt 599, 1 Jeans 999, discount 100") },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { viewModel.extractInvoiceWithAi(pastedText) },
                                        enabled = !isExtracting && (selectedBitmap != null || pastedText.isNotBlank()),
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                                    ) {
                                        if (isExtracting) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Extracting with Gemini AI...", fontSize = 12.sp)
                                        } else {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Extract with Gemini AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (aiStatusMessage.isNotBlank()) {
                                        Text(text = aiStatusMessage, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }

                                "multi" -> {
                                    Text("Multi-Screenshot Invoice Creator", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Ek baar me multiple screenshots attach karke un sabke invoices background me bna lijiye.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (isMultiExtracting) {
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF3B82F6))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("Processing Batch Invoices...", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                                                    Text(multiProgressMessage, fontSize = 11.sp, color = Color(0xFF1E3A8A))
                                                }
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                multiPhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Select Multiple Screenshots", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    if (multiGeneratedInvoices.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Generated Batch Invoices (${multiGeneratedInvoices.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15945B))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            items(multiGeneratedInvoices) { inv ->
                                                Surface(
                                                    color = Color(0xFFECFDF5),
                                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.clickable {
                                                        viewModel.setPreviewInvoice(inv)
                                                        onInvoiceGenerated(inv)
                                                    }
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text("No: ${inv.invoiceNo}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF065F46))
                                                        Text("To: ${inv.customerName}", fontSize = 10.sp, color = Color(0xFF065F46))
                                                        Text("Total: ₹${inv.total.toInt()}", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color(0xFF065F46))
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Tap to View 👁", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "camera" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Real Mobile Camera Scanner 📸", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Surface(
                                            color = Color(0xFFE0F2FE),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "HD Quality",
                                                color = Color(0xFF0369A1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                val uri = createTempImageUri(context)
                                                if (uri != null) {
                                                    cameraPhotoUri = uri
                                                    takePictureLauncher.launch(uri)
                                                } else {
                                                    Toast.makeText(context, "Error: Camera permissions or file context missing.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Camera",
                                                tint = BillGenOrange,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Tap to Launch Real Camera",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Point at any page, paper bill, or receipts",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            val uri = createTempImageUri(context)
                                            if (uri != null) {
                                                cameraPhotoUri = uri
                                                takePictureLauncher.launch(uri)
                                            } else {
                                                Toast.makeText(context, "Error: Camera file context missing.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isCameraScanning,
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                                    ) {
                                        if (isCameraScanning) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Scanning captured page...", fontSize = 12.sp)
                                        } else {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Launch Real Camera Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (isCameraScanning || cameraScanProgress.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = Color(0xFFF0FDF4),
                                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF10B981))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Extracting page with Gemini AI...",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF047857)
                                                    )
                                                    Text(cameraScanProgress, fontSize = 10.sp, color = Color(0xFF047857))
                                                }
                                            }
                                        }
                                    }

                                    if (cameraScanInvoices.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Camera Invoices Scanned this Session (${cameraScanInvoices.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            items(cameraScanInvoices) { inv ->
                                                Surface(
                                                    color = Color(0xFFF0FDF4),
                                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.clickable {
                                                        viewModel.setPreviewInvoice(inv)
                                                        onInvoiceGenerated(inv)
                                                    }
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text("No: ${inv.invoiceNo}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF065F46))
                                                        Text("To: ${inv.customerName}", fontSize = 10.sp, color = Color(0xFF065F46))
                                                        Text("Total: ₹${inv.total.toInt()}", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color(0xFF065F46))
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("View 👁", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Customer Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Customer Information", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { viewModel.editorCustomerName.value = it },
                            label = { Text("Customer Name *") },
                            modifier = Modifier.fillMaxWidth().testTag("input_customer_name"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { viewModel.editorCustomerPhone.value = it },
                                label = { Text("Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f).testTag("input_customer_phone"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = invoiceNo,
                                onValueChange = { viewModel.editorInvoiceNo.value = it },
                                label = { Text("Invoice #") },
                                modifier = Modifier.weight(1f).testTag("input_invoice_number"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customerAddress,
                            onValueChange = { viewModel.editorCustomerAddress.value = it },
                            label = { Text("Customer Address (City / State)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Ship To / Delivery Details (Optional) Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = BillGenOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ship To / Delivery Details (Optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            if (shipToAddress.isNotBlank() || shipToName.isNotBlank()) {
                                TextButton(onClick = {
                                    viewModel.editorShipToName.value = ""
                                    viewModel.editorShipToPhone.value = ""
                                    viewModel.editorShipToAddress.value = ""
                                }) {
                                    Text("Clear (Hide Box)", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Text(
                            text = "💡 Note: डिलीवरी एड्रेस न होने पर यह बॉक्स इनवॉइस से पूरी तरह छिप जाएगा।",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = shipToName,
                                onValueChange = { viewModel.editorShipToName.value = it },
                                label = { Text("Receiver Name") },
                                placeholder = { Text("Same as Customer") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = shipToPhone,
                                onValueChange = { viewModel.editorShipToPhone.value = it },
                                label = { Text("Receiver Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = shipToAddress,
                            onValueChange = { viewModel.editorShipToAddress.value = it },
                            label = { Text("Delivery Address") },
                            placeholder = { Text("City, State, Pincode (Khali chhodne par Ship To box nahi dikhega)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Store Branding & GST (For this Bill)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = BillGenOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Store Branding & GST (Optional for Bill)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "💡 Profile me dali details auto aayi hain. Yahan se is bill ke liye alag photo/logo/GST bhi daal sakte hain.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Logo Button
                            OutlinedButton(
                                onClick = {
                                    logoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (editorLogoUri.isNotBlank()) "Logo Added ✓" else "+ Add Logo",
                                    fontSize = 11.sp,
                                    color = if (editorLogoUri.isNotBlank()) Color(0xFF15945B) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Store Photo Button
                            OutlinedButton(
                                onClick = {
                                    storePhotoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (editorPhotoUri.isNotBlank()) "Photo Added ✓" else "+ Store Photo",
                                    fontSize = 11.sp,
                                    color = if (editorPhotoUri.isNotBlank()) Color(0xFF15945B) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (editorLogoUri.isNotBlank() || editorPhotoUri.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (editorLogoUri.isNotBlank()) {
                                    TextButton(onClick = { viewModel.editorLogoUri.value = "" }) {
                                        Text("Remove Logo", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (editorPhotoUri.isNotBlank()) {
                                    TextButton(onClick = { viewModel.editorPhotoUri.value = "" }) {
                                        Text("Remove Photo", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = editorGstin,
                            onValueChange = { viewModel.editorGstin.value = it.uppercase() },
                            label = { Text("GSTIN Number (Optional)") },
                            placeholder = { Text("e.g. 07AAAAA0000A1Z5") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Line Items Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Products & Services (${items.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = { viewModel.addItemToEditor() },
                        modifier = Modifier.height(34.dp).testTag("add_item_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BillGenOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Item", fontSize = 11.sp, color = BillGenOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }

            itemsIndexed(items) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BillGenOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { showCatalogDialog = index }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pick from Catalog", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            if (items.size > 1) {
                                IconButton(
                                    onClick = { viewModel.removeItemFromEditor(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFD94732), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { viewModel.updateItemInEditor(index, item.copy(name = it)) },
                            label = { Text("Product / Service Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = if (item.qty == 0.0) "" else item.qty.toString(),
                                onValueChange = {
                                    val q = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateItemInEditor(index, item.copy(qty = q))
                                },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = if (item.price == 0.0) "" else item.price.toString(),
                                onValueChange = {
                                    val p = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateItemInEditor(index, item.copy(price = p))
                                },
                                label = { Text("Price (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = item.hsn,
                                onValueChange = { viewModel.updateItemInEditor(index, item.copy(hsn = it)) },
                                label = { Text("HSN/SAC") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Line Total: " + FormatUtils.formatMoney(item.total, currency),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Discounts, GST & Payment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = if (discount == 0.0) "" else discount.toString(),
                                onValueChange = { viewModel.editorDiscount.value = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Discount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = if (shipping == 0.0) "" else shipping.toString(),
                                onValueChange = { viewModel.editorShipping.value = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Shipping (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var expandedGst by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = "${gstRate.toInt()}% GST",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("GST Rate") },
                                    trailingIcon = {
                                        IconButton(onClick = { expandedGst = true }) {
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(expanded = expandedGst, onDismissRequest = { expandedGst = false }) {
                                    listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                                        DropdownMenuItem(
                                            text = { Text("${rate.toInt()}% GST") },
                                            onClick = {
                                                viewModel.editorGstRate.value = rate
                                                expandedGst = false
                                            }
                                        )
                                    }
                                }
                            }

                            var expandedGstType by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = when (gstType) {
                                        "intra" -> "Intra (CGST+SGST)"
                                        "inter" -> "Inter (IGST)"
                                        else -> "No GST / Exempt"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("GST Type") },
                                    trailingIcon = {
                                        IconButton(onClick = { expandedGstType = true }) {
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(expanded = expandedGstType, onDismissRequest = { expandedGstType = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Intra (CGST+SGST)") },
                                        onClick = {
                                            viewModel.editorGstType.value = "intra"
                                            expandedGstType = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Inter (IGST)") },
                                        onClick = {
                                            viewModel.editorGstType.value = "inter"
                                            expandedGstType = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("No GST / Exempt") },
                                        onClick = {
                                            viewModel.editorGstType.value = "none"
                                            expandedGstType = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var expandedStatus by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = paymentStatus,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Status") },
                                    trailingIcon = {
                                        IconButton(onClick = { expandedStatus = true }) {
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                    listOf("PAID", "UNPAID", "PARTIALLY PAID", "OVERDUE").forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st) },
                                            onClick = {
                                                viewModel.editorPaymentStatus.value = st
                                                if (st == "PAID" && amountPaid == 0.0) {
                                                    viewModel.editorAmountPaid.value = calculation.total
                                                }
                                                expandedStatus = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = if (amountPaid == 0.0) "" else amountPaid.toString(),
                                onValueChange = { viewModel.editorAmountPaid.value = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Amount Paid (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { viewModel.editorNotes.value = it },
                            label = { Text("Invoice Notes / Remarks") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }

    if (showCatalogDialog != null) {
        val targetIndex = showCatalogDialog!!
        AlertDialog(
            onDismissRequest = { showCatalogDialog = null },
            title = { Text("Select from Product Catalog") },
            text = {
                if (products.isEmpty()) {
                    Text("No saved catalog products. Add products in the Catalog tab to reuse them.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(products) { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCatalogProductForItem(targetIndex, prod)
                                        showCatalogDialog = null
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("HSN: ${prod.hsn.ifBlank { "N/A" }} • Stock: ${prod.stock}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("₹${prod.price}", fontWeight = FontWeight.ExtraBold, color = BillGenOrange)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCatalogDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModeTabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) BillGenOrange else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
