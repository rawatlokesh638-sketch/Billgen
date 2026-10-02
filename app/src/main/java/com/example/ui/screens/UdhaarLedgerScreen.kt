package com.example.ui.screens

import android.widget.Toast
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
import com.example.data.model.InvoiceEntity
import com.example.ui.BillGenViewModel
import com.example.ui.theme.BillGenOrange
import com.example.util.FormatUtils
import com.example.util.SharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarLedgerScreen(
    viewModel: BillGenViewModel,
    onNavigateToInvoice: (InvoiceEntity) -> Unit
) {
    val invoices by viewModel.allInvoices.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForPayment by remember { mutableStateOf<InvoiceEntity?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var paymentMethodInput by remember { mutableStateOf("UPI") }

    // Group invoices by customer
    val pendingInvoices = invoices.filter { it.pendingAmount > 0 }
    val totalPending = pendingInvoices.sumOf { it.pendingAmount }

    val filteredInvoices = if (searchQuery.isBlank()) {
        pendingInvoices
    } else {
        pendingInvoices.filter {
            it.customerName.contains(searchQuery, ignoreCase = true) ||
                    it.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    it.invoiceNo.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Credit / Udhaar Ledger", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0E9)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL PENDING UDHAAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD94732))
                        Text(
                            text = "₹${FormatUtils.formatMoney(totalPending, "").trim()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD94732)
                        )
                        Text("${pendingInvoices.size} unpaid / partial invoices", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD94732).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD94732))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by customer name or phone...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredInvoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF15945B), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Pending Credit!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("All customer payments are settled.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredInvoices) { inv ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToInvoice(inv) },
                            shape = RoundedCornerShape(14.dp),
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
                                        Text(inv.customerName.ifBlank { "Customer" }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${inv.invoiceNo} • ${inv.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (inv.customerPhone.isNotBlank()) {
                                            Text(inv.customerPhone, fontSize = 11.sp, color = BillGenOrange)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${inv.pendingAmount}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFD94732))
                                        Text("Total: ₹${inv.total}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            selectedInvoiceForPayment = inv
                                            paymentAmountInput = inv.pendingAmount.toString()
                                            showPaymentDialog = true
                                        },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B))
                                    ) {
                                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record Payment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val reminder = "Hello ${inv.customerName},\nThis is a gentle reminder for your pending invoice #${inv.invoiceNo}.\nPending Balance: ₹${inv.pendingAmount}\nTotal Amount: ₹${inv.total}\nPlease settle the payment when convenient. Thank you!"
                                            SharingHelper.shareToWhatsApp(context, reminder, inv.customerPhone)
                                        },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Send Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog && selectedInvoiceForPayment != null) {
        val invoice = selectedInvoiceForPayment!!
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Record Customer Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Customer: ${invoice.customerName} (${invoice.invoiceNo})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Current Pending: ₹${invoice.pendingAmount}", color = Color(0xFFD94732), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("Payment Received (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = paymentMethodInput,
                        onValueChange = { paymentMethodInput = it },
                        label = { Text("Payment Mode (UPI, Cash, Card, Bank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.recordPaymentForInvoice(
                                invoice = invoice,
                                amount = amount,
                                method = paymentMethodInput,
                                reference = "Settled via Udhaar Ledger"
                            )
                            Toast.makeText(context, "Payment of ₹$amount recorded & receipt created!", Toast.LENGTH_LONG).show()
                            showPaymentDialog = false
                        } else {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B))
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
