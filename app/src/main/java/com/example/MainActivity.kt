package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvoiceEntity
import com.example.ui.BillGenViewModel
import com.example.ui.ai.BillGenAIAgentSheet
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
    object Invoices : Screen("invoices", "Create", Icons.Default.AddCircle)
    object History : Screen("history", "History", Icons.Default.ReceiptLong)
    object Business : Screen("business", "Business", Icons.Default.Storefront)
    object Udhaar : Screen("udhaar", "Udhaar", Icons.Default.AccountBalanceWallet)
    object Tools : Screen("tools", "Tools", Icons.Default.Settings)
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val onboardingPrefs = remember { context.getSharedPreferences("billgen_onboarding_prefs", android.content.Context.MODE_PRIVATE) }
    var showOnboarding by remember { mutableStateOf(!onboardingPrefs.getBoolean("onboarding_completed", false)) }

    if (showOnboarding) {
        OnboardingScreen(
            onFinished = {
                onboardingPrefs.edit().putBoolean("onboarding_completed", true).apply()
                showOnboarding = false
            }
        )
        return
    }

    var recordAudioPermissionGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var showAIAgentSheet by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(context) {
        var ttsInstance: android.speech.tts.TextToSpeech? = null
        ttsInstance = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ttsInstance?.language = java.util.Locale("hi", "IN")
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    LaunchedEffect(viewModel.agentTtsTrigger) {
        viewModel.agentTtsTrigger.collect { text ->
            tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        recordAudioPermissionGranted = granted
        if (granted) {
            android.widget.Toast.makeText(context, "Voice Wake-Word Scanner Active! Say 'Hello BillGen'", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(recordAudioPermissionGranted, tts) {
        var voiceHelper: com.example.util.VoiceTriggerHelper? = null
        if (recordAudioPermissionGranted) {
            voiceHelper = com.example.util.VoiceTriggerHelper(
                context = context,
                onWakeWordDetected = {
                    try {
                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } catch (_: Exception) {}
                    
                    tts?.speak("Batao kya karna hai", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                    android.widget.Toast.makeText(context, "BillGen Copilot active! Speak your command...", android.widget.Toast.LENGTH_SHORT).show()
                    showAIAgentSheet = true
                },
                onCommandDetected = { command ->
                    android.widget.Toast.makeText(context, "Executing: $command", android.widget.Toast.LENGTH_LONG).show()
                    viewModel.askAgent(command)
                }
            )
            voiceHelper.startListening()
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }

        onDispose {
            voiceHelper?.stopListening()
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var toolsInitialSection by remember { mutableStateOf("catalog") }
    var isAdminSelectionMade by remember { mutableStateOf(false) }
    val isAdmin by viewModel.isAdmin.collectAsState()
    
    if (isAdmin && !isAdminSelectionMade) {
        AdminUserSelectionScreen(
            onNavigateToApp = { isAdminSelectionMade = true },
            onNavigateToAdminPanel = { 
                isAdminSelectionMade = true
                toolsInitialSection = "admin"
                currentScreen = Screen.Tools 
            }
        )
        return
    }

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
                val screens = listOf(Screen.Dashboard, Screen.Invoices, Screen.History, Screen.Business, Screen.Tools)
                screens.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (screen == Screen.Invoices) {
                                viewModel.initNewInvoice()
                                invoiceSubScreen = InvoiceSubScreen.EDITOR
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
                        },
                        onOpenAiAgent = {
                            showAIAgentSheet = true
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
                                    onNavigateToHistory = {
                                        currentScreen = Screen.History
                                    },
                                    onBack = {
                                        invoiceSubScreen = InvoiceSubScreen.EDITOR
                                    },
                                    onOpenProUpgrade = {
                                        showProUpgradeDialog = true
                                    }
                                )
                            } else {
                                invoiceSubScreen = InvoiceSubScreen.EDITOR
                            }
                        }
                    }
                }

                Screen.History -> {
                    InvoiceHistoryScreen(
                        viewModel = viewModel,
                        onNavigateToPreview = { invoice ->
                            viewModel.setPreviewInvoice(invoice)
                            invoiceSubScreen = InvoiceSubScreen.PREVIEW
                            currentScreen = Screen.Invoices
                            maybeTriggerInterstitial(force = true)
                        },
                        onNavigateToCreate = {
                            viewModel.initNewInvoice()
                            editorInitialMode = "manual"
                            invoiceSubScreen = InvoiceSubScreen.EDITOR
                            currentScreen = Screen.Invoices
                        }
                    )
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

                Screen.Business -> {
                    BusinessProfileScreen(viewModel = viewModel)
                }

                Screen.Tools -> {
                    MoreToolsScreen(
                        viewModel = viewModel,
                        initialSection = toolsInitialSection,
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

    // Subscription Plans Screen
    if (showProUpgradeDialog) {
        SubscriptionPlansScreen(
            viewModel = viewModel,
            onBack = { showProUpgradeDialog = false }
        )
    }

    // BillGen AI Agent Assistant Sheet
    if (showAIAgentSheet) {
        BillGenAIAgentSheet(
            viewModel = viewModel,
            onDismiss = { showAIAgentSheet = false }
        )
    }
}
