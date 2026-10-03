package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvoiceEntity
import com.example.ui.BillGenViewModel
import com.example.ui.monetization.AdMobBannerCard
import com.example.ui.monetization.AdsterraBannerAd
import com.example.ui.monetization.AdsterraNativeAd
import com.example.ui.monetization.EarnKaroNativeAdCard
import com.example.ui.theme.BillGenOrange
import com.example.ui.theme.BillGenOrangeLight
import com.example.util.FormatUtils
import com.example.util.SharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BillGenViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToPreview: (InvoiceEntity) -> Unit,
    onNavigateToUdhaar: () -> Unit,
    onOpenProUpgrade: () -> Unit,
    onOpenRewardedAd: () -> Unit
) {
    val invoices by viewModel.allInvoices.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()
    val isPro = viewModel.retentionHelper.isProUser
    val streak = viewModel.retentionHelper.billingStreak
    val credits = viewModel.retentionHelper.aiScanCredits
    val context = LocalContext.current

    val totalSales = remember(invoices) { invoices.sumOf { it.total } }
    val totalPaid = remember(invoices) { invoices.sumOf { it.amountPaid } }
    val totalPending = remember(invoices) { (totalSales - totalPaid).coerceAtLeast(0.0) }
    
    val todayStr = remember { FormatUtils.currentDateFormatted() }
    val todayInvoices = remember(invoices, todayStr) { invoices.filter { it.date == todayStr } }
    val todaySales = remember(todayInvoices) { todayInvoices.sumOf { it.total } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(BillGenOrange, BillGenOrangeLight))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▤", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BillGen AI",
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = businessProfile.businessName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = Color(0xFFFFECE0),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🔥", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "$streak-Day",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BillGenOrange
                            )
                        }
                    }

                    if (isPro) {
                        Surface(
                            color = BillGenOrange,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "PRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { onOpenRewardedAd() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("⚡", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "$credits Scans",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.CameraAlt,
                        title = "Scan Screenshot",
                        subtitle = "Gemini AI",
                        isPrimary = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("action_scan_screenshot"),
                        onClick = onNavigateToScan
                    )
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        title = "New Invoice",
                        subtitle = "Manual Form",
                        isPrimary = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_new_invoice"),
                        onClick = onNavigateToCreate
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "TODAY'S SALES",
                            value = FormatUtils.formatMoney(todaySales, businessProfile.defaultCurrency),
                            subtext = "${todayInvoices.size} bills today",
                            indicatorColor = Color(0xFF15945B),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "TOTAL INVOICES",
                            value = "${invoices.size}",
                            subtext = FormatUtils.formatMoney(totalSales, businessProfile.defaultCurrency) + " volume",
                            indicatorColor = BillGenOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "COLLECTED",
                            value = FormatUtils.formatMoney(totalPaid, businessProfile.defaultCurrency),
                            subtext = "Received in full",
                            indicatorColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "PENDING UDHAAR",
                            value = FormatUtils.formatMoney(totalPending, businessProfile.defaultCurrency),
                            subtext = "Tap to collect",
                            indicatorColor = Color(0xFFD94732),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToUdhaar() }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onOpenProUpgrade() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF6366F1), BillGenOrange, Color(0xFF10B981))))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(BillGenOrange, Color(0xFF8B5CF6)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Store Plans & Pricing", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = BillGenOrange, shape = RoundedCornerShape(4.dp)) {
                                    Text("From ₹49/mo", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                "Royal Blue, Emerald & Luxury Gold templates, PhonePe UPI & GST billing",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "View Plans", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            item {
                AdsterraBannerAd(isProUser = isPro)
            }

            item {
                AdMobBannerCard(isProUser = isPro, onAdClick = onOpenProUpgrade)
            }

            if (totalPending > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onNavigateToUdhaar() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0E9)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD94732)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Pending Udhaar to Collect",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF902717)
                                )
                                Text(
                                    "Total outstanding: ${FormatUtils.formatMoney(totalPending, businessProfile.defaultCurrency)}. Tap to send WhatsApp reminders.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF702010)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF902717)
                            )
                        }
                    }
                }
            }

            item {
                val last7DaysSales = remember(invoices) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                    val list = mutableListOf<Triple<String, Double, String>>()
                    for (i in 6 downTo 0) {
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                        val dateStr = sdf.format(cal.time)
                        val dayLabel = if (i == 0) "Today" else dayFormat.format(cal.time)
                        val daySales = invoices.filter { it.date == dateStr }.sumOf { it.total }
                        list.add(Triple(dayLabel, daySales, dateStr))
                    }
                    list
                }
                val max7DaysSales = remember(last7DaysSales) {
                    val highest = last7DaysSales.maxOfOrNull { it.second } ?: 0.0
                    if (highest > 0.0) highest else 1000.0
                }
                var selectedDayInfo by remember { mutableStateOf<Pair<String, Double>?>(null) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Real Weekly Sales Trend", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (selectedDayInfo != null) {
                                    Text(
                                        "${selectedDayInfo!!.first}: ₹${selectedDayInfo!!.second.toInt()} sales",
                                        fontSize = 11.sp,
                                        color = BillGenOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text("Calculated from your actual bills", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("Last 7 Days", fontSize = 11.sp, color = BillGenOrange, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7DaysSales.forEach { (day, amount, dateStr) ->
                                val fraction = if (amount > 0.0) {
                                    (amount / max7DaysSales).toFloat().coerceIn(0.15f, 1f)
                                } else {
                                    0.06f // subtle base bar if 0
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedDayInfo = Pair(day, amount)
                                        }
                                ) {
                                    if (amount > 0.0) {
                                        Text(
                                            text = if (amount >= 1000) "${(amount / 1000).toInt()}k" else amount.toInt().toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BillGenOrange,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .fillMaxHeight(fraction)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                if (amount > 0.0) {
                                                    Brush.verticalGradient(listOf(BillGenOrangeLight, BillGenOrange))
                                                } else {
                                                    Brush.verticalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFD0D0D0)))
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = day,
                                        fontSize = 10.sp,
                                        fontWeight = if (day == "Today") FontWeight.Bold else FontWeight.Normal,
                                        color = if (day == "Today") BillGenOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // High CPM Adsterra Native Ad & Affiliate Card
            item {
                AdsterraNativeAd(isProUser = isPro)
            }

            item {
                EarnKaroNativeAdCard(isProUser = isPro)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Invoices",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${invoices.size} total",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (invoices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No invoices yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Tap 'Scan Screenshot' or 'New Invoice' to generate your first bill.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(invoices.take(15)) { invoice ->
                    InvoiceRowCard(
                        invoice = invoice,
                        onClick = { onNavigateToPreview(invoice) },
                        onShare = {
                            val summary = SharingHelper.buildInvoiceSummaryText(invoice, emptyList())
                            SharingHelper.shareToWhatsApp(context, summary, invoice.customerPhone)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(76.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) BillGenOrange else MaterialTheme.colorScheme.surface
        ),
        border = if (!isPrimary) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isPrimary) Color.White.copy(alpha = 0.25f) else BillGenOrange.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color.White else BillGenOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (isPrimary) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtext: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun InvoiceRowCard(
    invoice: InvoiceEntity,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .testTag("invoice_card_${invoice.invoiceNo}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (invoice.customerName.take(1).ifBlank { "C" }).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = invoice.customerName.ifBlank { "Customer" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "#${invoice.invoiceNo} • ${FormatUtils.formatDate(invoice.date)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatMoney(invoice.total, invoice.currency),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Surface(
                    color = if (invoice.effectiveStatus == "PAID") Color(0xFFE9F8F0) else Color(0xFFFFF0E9),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = invoice.effectiveStatus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (invoice.effectiveStatus == "PAID") Color(0xFF15945B) else Color(0xFFC04C1D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
