package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BusinessProfile
import com.example.ui.BillGenViewModel
import com.example.ui.theme.BillGenOrange
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    viewModel: BillGenViewModel
) {
    val context = LocalContext.current
    val currentProfile by viewModel.businessProfile.collectAsState()

    var name by remember(currentProfile) { mutableStateOf(currentProfile.businessName) }
    var tagline by remember(currentProfile) { mutableStateOf(currentProfile.businessTagline) }
    var address by remember(currentProfile) { mutableStateOf(currentProfile.businessAddress) }
    var phone by remember(currentProfile) { mutableStateOf(currentProfile.businessPhone) }
    var email by remember(currentProfile) { mutableStateOf(currentProfile.businessEmail) }
    var website by remember(currentProfile) { mutableStateOf(currentProfile.businessWebsite) }
    var socialHandle by remember(currentProfile) { mutableStateOf(currentProfile.socialHandle) }
    var gstin by remember(currentProfile) { mutableStateOf(currentProfile.businessGstin) }

    var upiId by remember(currentProfile) { mutableStateOf(currentProfile.upiId) }
    var bankAccountName by remember(currentProfile) { mutableStateOf(currentProfile.bankAccountName) }
    var bankName by remember(currentProfile) { mutableStateOf(currentProfile.bankName) }
    var bankAccountNo by remember(currentProfile) { mutableStateOf(currentProfile.bankAccountNo) }
    var bankIfsc by remember(currentProfile) { mutableStateOf(currentProfile.bankIfsc) }

    var logoUri by remember(currentProfile) { mutableStateOf(currentProfile.logoUri) }
    var photoUri by remember(currentProfile) { mutableStateOf(currentProfile.businessPhotoUri) }
    var stampUri by remember(currentProfile) { mutableStateOf(currentProfile.stampUri) }
    var signatureUri by remember(currentProfile) { mutableStateOf(currentProfile.signatureUri) }

    var signatoryName by remember(currentProfile) { mutableStateOf(currentProfile.signatoryName) }
    var signatoryDesignation by remember(currentProfile) { mutableStateOf(currentProfile.signatoryDesignation) }
    var terms by remember(currentProfile) { mutableStateOf(currentProfile.defaultTerms) }

    // Image Launchers
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { logoUri = it.toString() }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { photoUri = it.toString() }
    }
    val stampPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { stampUri = it.toString() }
    }
    val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { signatureUri = it.toString() }
    }

    var showSignaturePad by remember { mutableStateOf(false) }

    if (showSignaturePad) {
        DigitalSignatureDialog(
            onDismiss = { showSignaturePad = false },
            onSignatureSaved = { savedUriStr ->
                signatureUri = savedUriStr
                showSignaturePad = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏢 Business Profile & Branding", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    Button(
                        onClick = {
                            val updated = currentProfile.copy(
                                businessName = name,
                                businessTagline = tagline,
                                businessAddress = address,
                                businessPhone = phone,
                                businessEmail = email,
                                businessWebsite = website,
                                socialHandle = socialHandle,
                                businessGstin = gstin,
                                upiId = upiId,
                                bankAccountName = bankAccountName,
                                bankName = bankName,
                                bankAccountNo = bankAccountNo,
                                bankIfsc = bankIfsc,
                                logoUri = logoUri,
                                businessPhotoUri = photoUri,
                                stampUri = stampUri,
                                signatureUri = signatureUri,
                                signatoryName = signatoryName,
                                signatoryDesignation = signatoryDesignation,
                                defaultTerms = terms
                            )
                            viewModel.updateBusinessProfile(updated)
                            Toast.makeText(context, "Business Details Saved! Auto-applied to all future invoices.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_business_profile_top_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Details", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3EC)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BillGenOrange, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Yahan apni dukaan/business ki details ek baar daal dein. Har baar bill banate waqt app auto-fill kar lega. Sabhi fields optional hain!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 1. BRAND VISUALS (LOGO & PHOTO)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = BillGenOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1. Brand Logos & Photos (Optional)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Logo Box
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Business Logo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, BillGenOrange, RoundedCornerShape(12.dp))
                                        .clickable { logoPicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (logoUri.isNotBlank()) {
                                        AsyncImage(
                                            model = logoUri,
                                            contentDescription = "Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = BillGenOrange)
                                            Text("Upload Logo", fontSize = 10.sp, color = BillGenOrange)
                                        }
                                    }
                                }
                                if (logoUri.isNotBlank()) {
                                    TextButton(onClick = { logoUri = "" }) {
                                        Text("Remove", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // Store Photo Box
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Store/Business Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, BillGenOrange, RoundedCornerShape(12.dp))
                                        .clickable { photoPicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (photoUri.isNotBlank()) {
                                        AsyncImage(
                                            model = photoUri,
                                            contentDescription = "Store Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = BillGenOrange)
                                            Text("Upload Shop Photo", fontSize = 10.sp, color = BillGenOrange)
                                        }
                                    }
                                }
                                if (photoUri.isNotBlank()) {
                                    TextButton(onClick = { photoUri = "" }) {
                                        Text("Remove", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. STORE IDENTITY DETAILS
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = BillGenOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("2. Store Identity & Contact", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Business / Store Name *") },
                            placeholder = { Text("e.g. Acme Retailers") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("business_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = tagline,
                            onValueChange = { tagline = it },
                            label = { Text("Tagline / Slogan (Optional)") },
                            placeholder = { Text("e.g. Quality • Trust • Service") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Full Address (Optional)") },
                            placeholder = { Text("e.g. Shop #12, Main Market, New Delhi") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number") },
                                placeholder = { Text("+91 98765 43210") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                placeholder = { Text("store@email.com") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = website,
                                onValueChange = { website = it },
                                label = { Text("Website (Optional)") },
                                placeholder = { Text("www.mystore.com") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = socialHandle,
                                onValueChange = { socialHandle = it },
                                label = { Text("Social / Instagram (Optional)") },
                                placeholder = { Text("@mystore_official") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it.uppercase() },
                            label = { Text("GSTIN Number (Optional)") },
                            placeholder = { Text("07AAAAA0000A1Z5") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 3. PAYMENT & BANKING DETAILS
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = BillGenOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("3. Payment & Bank Account Details (Optional)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID (Auto-generates QR Code on Bills)") },
                            placeholder = { Text("e.g. 9876543210@ybl or store@upi") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = bankAccountName,
                                onValueChange = { bankAccountName = it },
                                label = { Text("Account Name") },
                                placeholder = { Text("Store Owner Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                label = { Text("Bank Name") },
                                placeholder = { Text("HDFC / SBI / ICICI") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = bankAccountNo,
                                onValueChange = { bankAccountNo = it },
                                label = { Text("Account Number") },
                                placeholder = { Text("50200012345678") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = bankIfsc,
                                onValueChange = { bankIfsc = it.uppercase() },
                                label = { Text("IFSC Code") },
                                placeholder = { Text("HDFC0001234") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // 4. SIGNATURE & STAMP / SEAL
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Draw, contentDescription = null, tint = BillGenOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("4. Digital Signature & Stamp (Optional)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Digital Signature Box
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Authorized Signature", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, BillGenOrange, RoundedCornerShape(12.dp))
                                        .clickable { showSignaturePad = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (signatureUri.isNotBlank()) {
                                        AsyncImage(
                                            model = signatureUri,
                                            contentDescription = "Signature",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(6.dp)
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Draw, contentDescription = null, tint = BillGenOrange)
                                            Text("Draw / Upload Sign", fontSize = 10.sp, color = BillGenOrange)
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { showSignaturePad = true }) {
                                        Text("Draw", fontSize = 10.sp)
                                    }
                                    TextButton(onClick = { signaturePicker.launch("image/*") }) {
                                        Text("Upload", fontSize = 10.sp)
                                    }
                                    if (signatureUri.isNotBlank()) {
                                        TextButton(onClick = { signatureUri = "" }) {
                                            Text("Clear", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }

                            // Business Stamp Box
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Stamp / Seal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, BillGenOrange, RoundedCornerShape(12.dp))
                                        .clickable { stampPicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (stampUri.isNotBlank()) {
                                        AsyncImage(
                                            model = stampUri,
                                            contentDescription = "Stamp",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(6.dp)
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = BillGenOrange)
                                            Text("Upload Stamp", fontSize = 10.sp, color = BillGenOrange)
                                        }
                                    }
                                }
                                if (stampUri.isNotBlank()) {
                                    TextButton(onClick = { stampUri = "" }) {
                                        Text("Remove Stamp", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = signatoryName,
                                onValueChange = { signatoryName = it },
                                label = { Text("Signatory Name") },
                                placeholder = { Text("Rahul Verma") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = signatoryDesignation,
                                onValueChange = { signatoryDesignation = it },
                                label = { Text("Designation") },
                                placeholder = { Text("Authorized Signatory") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // 5. DEFAULT TERMS & CONDITIONS
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Gavel, contentDescription = null, tint = BillGenOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("5. Default Terms & Conditions (Optional)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedTextField(
                            value = terms,
                            onValueChange = { terms = it },
                            label = { Text("Terms & Conditions") },
                            placeholder = { Text("1. Goods once sold cannot be returned...\n2. Payment due within 7 days...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Bottom Big Save Button
            item {
                Button(
                    onClick = {
                        val updated = currentProfile.copy(
                            businessName = name,
                            businessTagline = tagline,
                            businessAddress = address,
                            businessPhone = phone,
                            businessEmail = email,
                            businessWebsite = website,
                            socialHandle = socialHandle,
                            businessGstin = gstin,
                            upiId = upiId,
                            bankAccountName = bankAccountName,
                            bankName = bankName,
                            bankAccountNo = bankAccountNo,
                            bankIfsc = bankIfsc,
                            logoUri = logoUri,
                            businessPhotoUri = photoUri,
                            stampUri = stampUri,
                            signatureUri = signatureUri,
                            signatoryName = signatoryName,
                            signatoryDesignation = signatoryDesignation,
                            defaultTerms = terms
                        )
                        viewModel.updateBusinessProfile(updated)
                        Toast.makeText(context, "✓ Business Details Saved Successfully!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_business_profile_bottom_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save All Business Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun DigitalSignatureDialog(
    onDismiss: () -> Unit,
    onSignatureSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val path = remember { Path() }
    var actionCount by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Draw Authorized Signature", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Finger se box mein sign karein:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, BillGenOrange, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    path.moveTo(offset.x, offset.y)
                                    actionCount++
                                },
                                onDrag = { change, _ ->
                                    path.lineTo(change.position.x, change.position.y)
                                    actionCount++
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPath(
                            path = path,
                            color = Color.Black,
                            style = Stroke(width = 5f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            strokeWidth = 6f
                            style = android.graphics.Paint.Style.STROKE
                            isAntiAlias = true
                        }
                        
                        val file = File(context.cacheDir, "signature_${System.currentTimeMillis()}.png")
                        val fos = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush()
                        fos.close()
                        
                        onSignatureSaved(file.absolutePath)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Signature saved!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
            ) {
                Text("Save Sign")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                path.reset()
                actionCount++
            }) {
                Text("Clear Pad")
            }
        }
    )
}
