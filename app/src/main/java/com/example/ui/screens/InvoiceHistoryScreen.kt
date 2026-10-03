package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Converters
import com.example.data.model.InvoiceEntity
import com.example.ui.BillGenViewModel
import com.example.ui.theme.BillGenOrange
import com.example.util.FormatUtils
import com.example.util.InvoicePrintHelper
import com.example.util.SharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryScreen(
    viewModel: BillGenViewModel,
    onNavigateToPreview: (InvoiceEntity) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val invoices by viewModel.allInvoices.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val converters = remember { Converters() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PAID, UNPAID

    val filteredInvoices = remember(invoices, searchQuery, selectedFilter) {
        invoices.filter { inv ->
            val matchesSearch = searchQuery.isBlank() ||
                    inv.customerName.contains(searchQuery, ignoreCase = true) ||
                    inv.invoiceNo.contains(searchQuery, ignoreCase = true) ||
                    inv.date.contains(searchQuery, ignoreCase = true) ||
                    inv.total.toString().contains(searchQuery)

            val matchesFilter = when (selectedFilter) {
                "PAID" -> inv.paymentStatus.equals("PAID", ignoreCase = true)
                "UNPAID" -> !inv.paymentStatus.equals("PAID", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedByDescending { it.createdAt }
    }

    val totalSales = remember(filteredInvoices) { filteredInvoices.sumOf { it.total } }
    val totalUnpaid = remember(filteredInvoices) {
        filteredInvoices.filter { !it.paymentStatus.equals("PAID", ignoreCase = true) }
            .sumOf { it.total - it.amountPaid }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Invoice History (${invoices.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "New Invoice",
                            tint = BillGenOrange
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_search_input"),
                placeholder = { Text("Search by customer, bill no, date...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BillGenOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("ALL" to "All Bills", "PAID" to "Paid", "UNPAID" to "Unpaid/Pending").forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (key == "PAID") Color(0xFF15945B) else if (key == "UNPAID") Color(0xFFD94732) else BillGenOrange,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_$key")
                    )
                }
            }

            // Overview Summary Box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                        Text("Filtered Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = FormatUtils.formatMoney(totalSales, "INR"),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (totalUnpaid > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pending Due", fontSize = 11.sp, color = Color(0xFFD94732))
                            Text(
                                text = FormatUtils.formatMoney(totalUnpaid, "INR"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFD94732)
                            )
                        }
                    }
                }
            }

            // Invoices List
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No invoices found for '$searchQuery'" else "No invoices saved yet",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredInvoices, key = { it.id }) { inv ->
                        InvoiceHistoryCard(
                            invoice = inv,
                            converters = converters,
                            onView = { onNavigateToPreview(inv) },
                            onPrint = {
                                activity?.let {
                                    InvoicePrintHelper.printInvoice(it, inv, converters.toLineItemList(inv.itemsJson))
                                }
                            },
                            onShare = {
                                val text = SharingHelper.buildInvoiceSummaryText(inv, converters.toLineItemList(inv.itemsJson))
                                SharingHelper.shareToWhatsApp(context, text, inv.customerPhone)
                            },
                            onDelete = {
                                viewModel.deleteInvoice(inv)
                                Toast.makeText(context, "Invoice #${inv.invoiceNo} deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceHistoryCard(
    invoice: InvoiceEntity,
    converters: Converters,
    onView: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val isPaid = invoice.paymentStatus.equals("PAID", ignoreCase = true)
    val statusColor = if (isPaid) Color(0xFF15945B) else Color(0xFFD94732)
    val statusBg = if (isPaid) Color(0xFFE9F8F0) else Color(0xFFFDE8E8)

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() }
            .testTag("history_card_${invoice.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Invoice #${invoice.invoiceNo}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = invoice.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isPaid) "PAID" else "UNPAID",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (invoice.customerName.isNotBlank()) invoice.customerName else "Walk-in Customer",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = FormatUtils.formatMoney(invoice.total, invoice.currency),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = BillGenOrange
                )
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onView, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPrint, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF15945B), modifier = Modifier.size(20.dp))
                    }
                }

                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD94732), modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Invoice #${invoice.invoiceNo}?") },
            text = { Text("Are you sure you want to delete this invoice? It will be removed locally and from cloud.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color(0xFFD94732), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
