package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BillGenViewModel
import com.example.ui.monetization.PhonePePaymentDialog
import com.example.ui.monetization.UserSubscriptionStatusCard
import com.example.ui.theme.BillGenOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    viewModel: BillGenViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Plans & Pricing", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            SubscriptionPlansContent(viewModel = viewModel)
        }
    }
}

@Composable
fun SubscriptionPlansContent(
    viewModel: BillGenViewModel
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
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UserSubscriptionStatusCard(
                viewModel = viewModel,
                onOpenUpgrade = { /* already on screen */ }
            )
        }

        // Monthly / Yearly Selector
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = if (!isYearly) BillGenOrange else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isYearly = false }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = "Monthly Billing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (!isYearly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (isYearly) BillGenOrange else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isYearly = true }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Yearly Billing",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isYearly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = Color(0xFF15945B), shape = RoundedCornerShape(4.dp)) {
                                    Text("Save 30%+", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. PRO PLAN CARD
        item {
            PlanDetailCard(
                title = "Pro Plan",
                themeName = "Royal Blue Clean & Professional",
                priceText = if (isYearly) "₹399 / year" else "₹49 / month",
                badge = "CLEAN & ESSENTIAL",
                primaryColor = Color(0xFF0052CC),
                isSelected = selectedPlanKey == "PRO",
                features = listOf(
                    "Royal Blue Clean Invoice Template",
                    "Unlimited Invoices & Receipts",
                    "Store Logo & Custom Branding",
                    "Remove BillGen AI Watermark",
                    "1-Click WhatsApp PDF Sharing",
                    "Udhaar Khata Credit Ledger"
                ),
                onClick = { selectedPlanKey = "PRO" }
            )
        }

        // 2. PRO PLUS PLAN CARD
        item {
            PlanDetailCard(
                title = "Pro Plus Plan",
                themeName = "Emerald Green Modern & Branded",
                priceText = if (isYearly) "₹799 / year" else "₹99 / month",
                badge = "POPULAR CHOICE",
                primaryColor = Color(0xFF00875A),
                isSelected = selectedPlanKey == "PRO_PLUS",
                features = listOf(
                    "Everything in Pro Plan",
                    "Emerald Green Branded Invoice Template",
                    "Show Business Shop Photo on Invoices",
                    "100 AI Photo Scans / Month",
                    "GST Tax Billing (CGST / SGST / IGST)",
                    "Inventory Stock Auto-deduction",
                    "Firebase Realtime Cloud Backup",
                    "Quotations & Estimates Generator"
                ),
                onClick = { selectedPlanKey = "PRO_PLUS" }
            )
        }

        // 3. PREMIUM PLAN CARD
        item {
            PlanDetailCard(
                title = "Premium Plan",
                themeName = "Luxury Gold & Dark Elegant",
                priceText = if (isYearly) "₹1,499 / year" else "₹199 / month",
                badge = "VIP FULL ACCESS",
                primaryColor = Color(0xFFC59B27),
                isSelected = selectedPlanKey == "PREMIUM",
                features = listOf(
                    "Everything in Pro Plus Plan",
                    "Luxury Gold & Dark Gradient Invoice Template",
                    "Header Scan & Pay Direct UPI QR Code",
                    "Unlimited AI Voice & Photo Scans",
                    "58mm / 80mm Bluetooth Thermal Printer Support",
                    "Multi-User Staff & Counter Access",
                    "GSTR-1 & GSTR-3B Tax Filing Excel Reports",
                    "24/7 Dedicated Priority Phone Support"
                ),
                onClick = { selectedPlanKey = "PREMIUM" }
            )
        }

        // Pay Button
        item {
            Button(
                onClick = { showPaymentDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedPlanKey) {
                        "PRO" -> Color(0xFF0052CC)
                        "PREMIUM" -> Color(0xFFC59B27)
                        else -> Color(0xFF00875A)
                    }
                )
            ) {
                Icon(imageVector = Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pay ₹${currentAmount.toInt()} via PhonePe / UPI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun PlanDetailCard(
    title: String,
    themeName: String,
    priceText: String,
    badge: String,
    primaryColor: Color,
    isSelected: Boolean,
    features: List<String>,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = primaryColor,
                        shape = RoundedCornerShape(6.dp)
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
                    fontSize = 16.sp,
                    color = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "🎨 Template: $themeName", fontSize = 11.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                features.forEach { ft ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = ft, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
