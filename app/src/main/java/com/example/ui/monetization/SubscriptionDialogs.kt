package com.example.ui.monetization

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SubscriptionRequest
import com.example.ui.BillGenViewModel
import com.example.ui.theme.BillGenOrange
import com.example.util.FormatUtils

@Composable
fun ProUpgradeDialog(
    viewModel: BillGenViewModel,
    onDismiss: () -> Unit
) {
    var isYearly by remember { mutableStateOf(false) }
    var selectedPlanKey by remember { mutableStateOf("PRO_PLUS") } // PRO, PRO_PLUS, PREMIUM
    var showPaymentDialog by remember { mutableStateOf(false) }

    val proAmount = if (isYearly) 399.0 else 49.0
    val proPlusAmount = if (isYearly) 799.0 else 99.0
    val premiumAmount = if (isYearly) 1499.0 else 199.0

    val currentAmount = when (selectedPlanKey) {
        "PRO" -> proAmount
        "PREMIUM" -> premiumAmount
        else -> proPlusAmount
    }

    if (showPaymentDialog) {
        PhonePePaymentDialog(
            planKey = selectedPlanKey,
            billingCycle = if (isYearly) "YEARLY" else "MONTHLY",
            amount = currentAmount,
            viewModel = viewModel,
            onDismiss = { showPaymentDialog = false },
            onSubmitted = {
                showPaymentDialog = false
                onDismiss()
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = BillGenOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "⭐ SELECT STORE SUBSCRIPTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = BillGenOrange,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Upgrade BillGen AI Store",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly / Yearly Toggle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = if (!isYearly) BillGenOrange else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isYearly = false }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "Monthly Plan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (!isYearly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = if (isYearly) BillGenOrange else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isYearly = true }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Yearly Plan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isYearly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(color = Color(0xFF15945B), shape = RoundedCornerShape(4.dp)) {
                                        Text("Save 30%+", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Plans Selection List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. Pro Plan
                    PlanCard(
                        title = "Pro Plan",
                        priceText = if (isYearly) "₹399 / year" else "₹49 / month",
                        badge = "BASIC",
                        badgeColor = Color(0xFF2563EB),
                        isSelected = selectedPlanKey == "PRO",
                        features = listOf(
                            "Unlimited Bills & Receipts",
                            "Custom Logo & Store Branding",
                            "Remove BillGen Watermark",
                            "WhatsApp 1-Click Sharing",
                            "Basic Udhaar Khata Ledger"
                        ),
                        onClick = { selectedPlanKey = "PRO" }
                    )

                    // 2. Pro Plus Plan
                    PlanCard(
                        title = "Pro Plus Plan",
                        priceText = if (isYearly) "₹799 / year" else "₹99 / month",
                        badge = "POPULAR",
                        badgeColor = BillGenOrange,
                        isSelected = selectedPlanKey == "PRO_PLUS",
                        features = listOf(
                            "Everything in Pro Plan",
                            "100 AI Photo & Voice Scans / Month",
                            "GST Tax Calculations (CGST/SGST)",
                            "Inventory Stock Auto-deduction",
                            "Firebase Realtime Cloud Sync",
                            "Quotations & Estimates Generator"
                        ),
                        onClick = { selectedPlanKey = "PRO_PLUS" }
                    )

                    // 3. Premium Plan
                    PlanCard(
                        title = "Premium Plan",
                        priceText = if (isYearly) "₹1,499 / year" else "₹199 / month",
                        badge = "VIP FULL ACCESS",
                        badgeColor = Color(0xFF6D5CE7),
                        isSelected = selectedPlanKey == "PREMIUM",
                        features = listOf(
                            "Everything in Pro Plus Plan",
                            "Unlimited AI Voice & Photo Scans",
                            "58mm/80mm Bluetooth Thermal Printer",
                            "Multi-User Staff & Counter Access",
                            "GSTR-1 & GSTR-3B Excel Reports",
                            "24/7 VIP Priority Phone Support"
                        ),
                        onClick = { selectedPlanKey = "PREMIUM" }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { showPaymentDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pay_via_phonepe_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlanKey == "PREMIUM") Color(0xFF6D5CE7) else BillGenOrange
                    )
                ) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pay ₹${currentAmount.toInt()} via PhonePe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Continue with Free Version", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    priceText: String,
    badge: String,
    badgeColor: Color,
    isSelected: Boolean,
    features: List<String>,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) badgeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) badgeColor else MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = priceText,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                features.forEach { ft ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF15945B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = ft, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun PhonePePaymentDialog(
    planKey: String,
    billingCycle: String,
    amount: Double,
    viewModel: BillGenViewModel,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    var utrInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val phonePeNumber = "9050884894"
    val upiId = "9050884894@ybl"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PhonePe UPI Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF5F259F).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Payable Amount: ₹${amount.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFF5F259F)
                        )
                        Text(
                            text = "Plan: $planKey ($billingCycle)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Steps
                Text(
                    text = "Step 1: Pay ₹${amount.toInt()} on PhonePe",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PhonePe / Paytm / GPay Number", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(phonePeNumber, fontWeight = FontWeight.Black, fontSize = 18.sp, color = BillGenOrange)
                            Text("UPI ID: $upiId", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PhonePe Number", phonePeNumber)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "PhonePe number $phonePeNumber copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = BillGenOrange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        try {
                            val uri = Uri.parse("upi://pay?pa=$upiId&pn=BillGenAI&am=${amount.toInt()}&cu=INR")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Please open PhonePe / Paytm app and pay to $phonePeNumber", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F259F))
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open PhonePe / UPI App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Step 2: Enter 12-Digit UTR / Transaction ID",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = utrInput,
                    onValueChange = { utrInput = it.take(18) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("utr_number_input"),
                    placeholder = { Text("e.g. 428192847192 (12 digits)") },
                    label = { Text("PhonePe UTR Transaction ID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (utrInput.trim().length < 6) {
                            Toast.makeText(context, "Please enter valid 12-digit UTR from PhonePe receipt", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        viewModel.submitSubscriptionPayment(
                            planType = planKey,
                            billingCycle = billingCycle,
                            amount = amount,
                            utrNumber = utrInput.trim()
                        ) { success, message ->
                            isSubmitting = false
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                onSubmitted()
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_utr_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Submit Payment for Verification", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun UserSubscriptionStatusCard(
    viewModel: BillGenViewModel,
    onOpenUpgrade: () -> Unit
) {
    val requests by viewModel.userSubscriptionRequests.collectAsState()
    val activeReq = remember(requests) { requests.firstOrNull() }
    val currentTier = viewModel.retentionHelper.userPlanTier

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Stars, contentDescription = null, tint = BillGenOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Current Plan: $currentTier", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (viewModel.retentionHelper.planExpiresAt > 0L) {
                            val expDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(viewModel.retentionHelper.planExpiresAt))
                            Text(
                                "Expires: $expDate",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenUpgrade,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    Text(if (currentTier == "FREE") "Upgrade" else "Change Plan", fontSize = 11.sp)
                }
            }

            if (activeReq != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                when (activeReq.status) {
                    "PENDING" -> {
                        Surface(color = Color(0xFFFFF3CD), shape = RoundedCornerShape(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("⏳ Subscription Verification Pending", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF856404))
                                    Text("Plan: ${activeReq.planType} (₹${activeReq.amount.toInt()}) • UTR: ${activeReq.utrNumber}", fontSize = 11.sp, color = Color(0xFF856404))
                                    Text("Admin is verifying your PhonePe payment to 9050884894.", fontSize = 10.sp, color = Color(0xFF856404))
                                }
                            }
                        }
                    }
                    "APPROVED" -> {
                        Surface(color = Color(0xFFE9F8F0), shape = RoundedCornerShape(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15945B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("✓ Subscription Approved & Active", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15945B))
                                    Text("Plan: ${activeReq.planType} • UTR: ${activeReq.utrNumber}", fontSize = 11.sp, color = Color(0xFF15945B))
                                }
                            }
                        }
                    }
                    "REJECTED" -> {
                        Surface(color = Color(0xFFFDE8E8), shape = RoundedCornerShape(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFD94732), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("❌ Request Rejected", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD94732))
                                    Text("UTR: ${activeReq.utrNumber} • Invalid UTR or Payment Not Received.", fontSize = 11.sp, color = Color(0xFFD94732))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
