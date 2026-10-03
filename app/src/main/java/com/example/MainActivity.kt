package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.model.InvoiceEntity
import com.example.ui.BillGenViewModel
import com.example.ui.monetization.EarnKaroInterstitialDialog
import com.example.ui.monetization.ProUpgradeDialog
import com.example.ui.monetization.RewardedAdDialog
import com.example.ui.screens.*
import com.example.ui.theme.BillGenAITheme
import com.example.ui.theme.BillGenOrange

class MainActivity : ComponentActivity() {

    private val viewModel: BillGenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BillGenAITheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Dashboard)
    object Invoices : Screen("invoices", "Invoices", Icons.Default.ReceiptLong)
    object Udhaar : Screen("udhaar", "Udhaar", Icons.Default.AccountBalanceWallet)
    object Tools : Screen("tools", "Tools", Icons.Default.Build)
}

enum class InvoiceSubScreen {
    EDITOR,
    PREVIEW
}

@Composable
fun MainAppContent(viewModel: BillGenViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isSessionLoggedIn by viewModel.isSessionLoggedIn.collectAsState()

    // If not session logged in and not firebase authenticated, show Auth Screen (Signup / Login)
    if (!isSessionLoggedIn && currentUser == null) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                // Auth succeeded or entered guest mode
            }
        )
        return
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var invoiceSubScreen by remember { mutableStateOf(InvoiceSubScreen.EDITOR) }
    var editorInitialMode by remember { mutableStateOf("ai") }

    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var showProUpgradeDialog by remember { mutableStateOf(false) }
    var showInterstitialDialog by remember { mutableStateOf(false) }
    var actionCounter by remember { mutableIntStateOf(0) }

    val activePreviewInvoice by viewModel.activePreviewInvoice.collectAsState()
    val isPro = viewModel.retentionHelper.isProUser

    fun maybeTriggerInterstitial(force: Boolean = false) {
        if (isPro) return
        actionCounter++
        if (force || actionCounter % 2 == 0) {
            showInterstitialDialog = true
        }
    }

    BackHandler(enabled = currentScreen != Screen.Dashboard || (currentScreen == Screen.Invoices && invoiceSubScreen == InvoiceSubScreen.PREVIEW)) {
        if (currentScreen == Screen.Invoices && invoiceSubScreen == InvoiceSubScreen.PREVIEW) {
            invoiceSubScreen = InvoiceSubScreen.EDITOR
        } else {
            currentScreen = Screen.Dashboard
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val screens = listOf(Screen.Dashboard, Screen.Invoices, Screen.Udhaar, Screen.Tools)
                screens.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (screen == Screen.Invoices) {
                                if (activePreviewInvoice == null) {
                                    invoiceSubScreen = InvoiceSubScreen.EDITOR
                                }
                            }
                            currentScreen = screen
                            maybeTriggerInterstitial(force = false)
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BillGenOrange,
                            selectedTextColor = BillGenOrange,
                            indicatorColor = BillGenOrange.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCreate = {
                            viewModel.initNewInvoice()
                            editorInitialMode = "manual"
                            invoiceSubScreen = InvoiceSubScreen.EDITOR
                            currentScreen = Screen.Invoices
                        },
                        onNavigateToScan = {
                            viewModel.initNewInvoice()
                            editorInitialMode = "ai"
                            invoiceSubScreen = InvoiceSubScreen.EDITOR
                            currentScreen = Screen.Invoices
                        },
                        onNavigateToPreview = { invoice ->
                            viewModel.setPreviewInvoice(invoice)
                            invoiceSubScreen = InvoiceSubScreen.PREVIEW
                            currentScreen = Screen.Invoices
                            maybeTriggerInterstitial(force = true)
                        },
                        onNavigateToUdhaar = {
                            currentScreen = Screen.Udhaar
                        },
                        onOpenProUpgrade = {
                            showProUpgradeDialog = true
                        },
                        onOpenRewardedAd = {
                            showRewardedAdDialog = true
                        }
                    )
                }

                Screen.Invoices -> {
                    when (invoiceSubScreen) {
                        InvoiceSubScreen.EDITOR -> {
                            InvoiceEditorScreen(
                                viewModel = viewModel,
                                initialMode = editorInitialMode,
                                onInvoiceGenerated = { savedInvoice ->
                                    viewModel.setPreviewInvoice(savedInvoice)
                                    invoiceSubScreen = InvoiceSubScreen.PREVIEW
                                    maybeTriggerInterstitial(force = true)
                                }
                            )
                        }
                        InvoiceSubScreen.PREVIEW -> {
                            val inv = activePreviewInvoice
                            if (inv != null) {
                                InvoicePreviewScreen(
                                    invoice = inv,
                                    viewModel = viewModel,
                                    onEditInvoice = { toEdit ->
                                        viewModel.loadInvoiceForEditing(toEdit)
                                        invoiceSubScreen = InvoiceSubScreen.EDITOR
                                    },
                                    onBack = {
                                        invoiceSubScreen = InvoiceSubScreen.EDITOR
                                    }
                                )
                            } else {
                                invoiceSubScreen = InvoiceSubScreen.EDITOR
                            }
                        }
                    }
                }

                Screen.Udhaar -> {
                    UdhaarLedgerScreen(
                        viewModel = viewModel,
                        onNavigateToInvoice = { invoice ->
                            viewModel.setPreviewInvoice(invoice)
                            invoiceSubScreen = InvoiceSubScreen.PREVIEW
                            currentScreen = Screen.Invoices
                            maybeTriggerInterstitial(force = true)
                        }
                    )
                }

                Screen.Tools -> {
                    MoreToolsScreen(
                        viewModel = viewModel,
                        onOpenProUpgrade = {
                            showProUpgradeDialog = true
                        }
                    )
                }
            }
        }
    }

    // Interstitial Ad Dialog ("kabhi beech beech mein dikhana Interstitial maano")
    if (showInterstitialDialog) {
        EarnKaroInterstitialDialog(
            onDismiss = { showInterstitialDialog = false },
            onClaim = { showInterstitialDialog = false }
        )
    }

    // Rewarded Ad Dialog
    if (showRewardedAdDialog) {
        RewardedAdDialog(
            onDismiss = { showRewardedAdDialog = false },
            onRewardEarned = {
                viewModel.retentionHelper.addRewardCredits(5)
            }
        )
    }

    // Pro Upgrade Dialog
    if (showProUpgradeDialog) {
        ProUpgradeDialog(
            onDismiss = { showProUpgradeDialog = false },
            onUpgradeSuccess = {
                viewModel.retentionHelper.isProUser = true
            }
        )
    }
}
