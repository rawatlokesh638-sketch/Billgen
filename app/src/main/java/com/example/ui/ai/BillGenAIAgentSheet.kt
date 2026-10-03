package com.example.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentMessageEntity
import com.example.ui.BillGenViewModel
import com.example.ui.theme.BillGenOrange
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillGenAIAgentSheet(
    viewModel: BillGenViewModel,
    onDismiss: () -> Unit
) {
    val messages by viewModel.agentMessages.collectAsState()
    val isThinking by viewModel.isAgentThinking.collectAsState()
    val liveActionStep by viewModel.agentLiveActionStep.collectAsState()

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var inputPrompt by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("chat") } // "chat" or "history"
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    val quickChips = listOf(
        "📊 Aaj ki sale kitni hai?",
        "💰 Kiska kitna udhaar baki hai?",
        "🧾 Rahul ka ₹500 ka bill banao",
        "📝 Amit ka ₹1200 udhaar likho",
        "✅ Rahul ka udhaar chukta karo",
        "📦 Stock summary dikhao"
    )

    LaunchedEffect(messages.size, liveActionStep) {
        if (messages.isNotEmpty() && activeTab == "chat") {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.94f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(BillGenOrange, Color(0xFF8B5CF6)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("BillGen AI Agent", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = if (liveActionStep != null) "Executing Task" else "Online",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text("Autonomous Store Assistant • Invoices & Khata", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { showConfirmClearDialog = true }) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Tab Selector: Live Chat vs History
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    Surface(
                        color = if (activeTab == "chat") MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(9.dp),
                        shadowElevation = if (activeTab == "chat") 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = "chat" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (activeTab == "chat") BillGenOrange else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Assistant",
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == "chat") FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == "chat") BillGenOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (activeTab == "history") MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(9.dp),
                        shadowElevation = if (activeTab == "history") 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = "history" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (activeTab == "history") BillGenOrange else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "History (${messages.size})",
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == "history") FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == "history") BillGenOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (activeTab == "history") {
                // Dedicated History & Past Tasks Section
                AgentHistoryView(
                    messages = messages,
                    onClearHistory = { showConfirmClearDialog = true },
                    onStartNewChat = { activeTab = "chat" },
                    onAskPrompt = { prompt ->
                        activeTab = "chat"
                        viewModel.askAgent(prompt)
                    }
                )
            } else {
                // Live Chat View
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF0E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BillGenOrange, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Namaste! Main BillGen AI Agent hoon.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Aap mujhse store ki sale, udhaar khata, ya koi bhi kaam karwa sakte hain.\nRealtime me background me kaam karke yahi update dunga.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            AgentMessageBubble(msg)
                        }

                        // Live Background Task Progress Indicator
                        if (liveActionStep != null) {
                            item {
                                Surface(
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                                    shape = RoundedCornerShape(14.dp),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.5.dp, color = Color(0xFF10B981))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "REALTIME TASK IN PROGRESS",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF15803D),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Live Active",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF15803D),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = liveActionStep ?: "Executing background operation...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF14532D)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Applying and syncing store updates automatically in realtime.",
                                            fontSize = 10.sp,
                                            color = Color(0xFF166534).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        } else if (isThinking) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFF0E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BillGenOrange, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BillGenOrange)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Agent is analyzing...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Chips Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickChips) { chip ->
                        Surface(
                            color = Color(0xFFFFF0E9),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BillGenOrange.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                viewModel.askAgent(chip)
                            }
                        ) {
                            Text(
                                text = chip,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BillGenOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Input Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            placeholder = { Text("Poochiye ya kaam bataiye (e.g. Rahul ka bill banao 500)...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("agent_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputPrompt.isNotBlank() && !isThinking) {
                                        val q = inputPrompt
                                        inputPrompt = ""
                                        viewModel.askAgent(q)
                                        focusManager.clearFocus()
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputPrompt.isNotBlank() && !isThinking) {
                                    val q = inputPrompt
                                    inputPrompt = ""
                                    viewModel.askAgent(q)
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = inputPrompt.isNotBlank() && !isThinking,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (inputPrompt.isNotBlank() && !isThinking) BillGenOrange else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("send_agent_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputPrompt.isNotBlank() && !isThinking) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("Clear Chat History?") },
            text = { Text("Kya aap pichli saari chats aur task records delete karna chahte hain?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAgentHistory()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AgentMessageBubble(msg: AgentMessageEntity) {
    val isUser = msg.sender == "user"
    val context = LocalContext.current
    val cleanText = remember(msg.text) {
        msg.text
            .replace("**", "")
            .replace(Regex("""(?m)^\s*[\*\-]\s+"""), "• ")
            .replace("*", "")
            .replace(Regex("""#{1,6}\s*"""), "")
            .replace("`", "")
            .replace("###", "")
            .replace("##", "")
            .trim()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BillGenOrange, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 295.dp)
        ) {
            Surface(
                color = if (isUser) BillGenOrange else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 14.dp
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = cleanText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    if (!msg.actionType.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFF15945B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(msg.actionType, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Agent Response", cleanText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
            Text(timeStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
        }
    }
}

@Composable
private fun AgentHistoryView(
    messages: List<AgentMessageEntity>,
    onClearHistory: () -> Unit,
    onStartNewChat: () -> Unit,
    onAskPrompt: (String) -> Unit
) {
    if (messages.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No Past History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Aapki chats aur background task execution yahan save hoti hain.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onStartNewChat,
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    Text("Start a Conversation")
                }
            }
        }
    } else {
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
                Text(
                    text = "All Past Tasks & Conversations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onClearHistory) {
                    Text("Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(messages.reversed(), key = { it.id }) { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (msg.sender == "user") BillGenOrange.copy(alpha = 0.15f) else Color(0xFF15945B).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (msg.sender == "user") "USER TASK" else "AGENT REPLY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.sender == "user") BillGenOrange else Color(0xFF15945B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (!msg.actionType.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = Color(0xFF15945B), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "✓ " + msg.actionType,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                                Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val cleanHistoryText = remember(msg.text) {
                                msg.text
                                    .replace("**", "")
                                    .replace(Regex("""(?m)^\s*[\*\-]\s+"""), "• ")
                                    .replace("*", "")
                                    .replace(Regex("""#{1,6}\s*"""), "")
                                    .replace("`", "")
                                    .replace("###", "")
                                    .replace("##", "")
                                    .trim()
                            }
                            Text(
                                text = cleanHistoryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (msg.sender == "user") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(
                                        onClick = { onAskPrompt(msg.text) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Re-run Task", fontSize = 11.sp)
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
