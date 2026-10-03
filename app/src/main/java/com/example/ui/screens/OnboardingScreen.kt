package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BillGenOrange

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "✨ AI Smart Scanner",
            subtitle = "Extract Bills From Screenshots",
            description = "WhatsApp screenshots, hand bills, paper bills ya customer list ki photo kheechiye. BillGen AI unhe automatically read karke perfect digital tax invoice bana dega!",
            icon = Icons.Default.CameraAlt,
            iconColor = BillGenOrange
        ),
        OnboardingPage(
            title = "💬 Voice Copilot Active",
            subtitle = "Hello BillGen Wake-Word",
            description = "App me kahi bhi boliyye 'Hello BillGen', hamara AI active ho jayega. Bolein 'Rahul ka ₹500 ka bill banao' ya 'Settle debt' aur AI background me sab karke dega!",
            icon = Icons.Default.Mic,
            iconColor = Color(0xFF6366F1)
        ),
        OnboardingPage(
            title = "📊 Cloud Sync & Udhaar",
            subtitle = "Safe Firebase Backup",
            description = "Apne customers ka ledger (Udhaar khata) aur bills safe Firebase Cloud par sync kijiye. Single click me customized reminders ya payment receipts WhatsApp par share kijiye!",
            icon = Icons.Default.CloudSync,
            iconColor = Color(0xFF10B981)
        )
    )

    var currentPage by remember { mutableStateOf(0) }
    val page = pages[currentPage]

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentPage < pages.lastIndex) {
                    TextButton(onClick = onFinished) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Visual Center Illustration Box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Interactive Bouncing Circle containing the icon
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(page.iconColor.copy(alpha = 0.12f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(page.iconColor, page.iconColor.copy(alpha = 0.7f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Text Content
                Text(
                    text = page.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = page.subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = page.iconColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                Text(
                    text = page.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Bottom Indicators and Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page Indicator Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    pages.forEachIndexed { idx, _ ->
                        val active = idx == currentPage
                        val width by animateDpAsState(
                            targetValue = if (active) 20.dp else 6.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "indicatorWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(width)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) page.iconColor else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                // Next / Get Started Buttons
                if (currentPage < pages.lastIndex) {
                    Button(
                        onClick = { currentPage++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = page.iconColor)
                    ) {
                        Text("Next Feature", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Button(
                        onClick = onFinished,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = page.iconColor)
                    ) {
                        Text("Get Started 🚀", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
