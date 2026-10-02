package com.example.ui.monetization

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BillGenOrange
import com.example.ui.theme.BillGenOrangeLight
import kotlinx.coroutines.delay

object AdsterraAdsConfig {
    // Extracted directly from user's HTML code
    const val BANNER_KEY = "0e2646541d90aca6dc3d2cd09bc02a41"
    const val BANNER_INVOKE_URL = "https://www.highrevenueformat.com/0e2646541d90aca6dc3d2cd09bc02a41/invoke.js"

    const val NATIVE_INVOKE_URL = "https://pl31537282.profitableratecpmnetwork.com/edd460171965647f099030c848d0ce48/invoke.js"
    const val NATIVE_CONTAINER_ID = "container-edd460171965647f099030c848d0ce48"

    const val SMARTLINK_URL = "https://www.profitableratecpmnetwork.com/e2905fw619?key=f95b245bc9c99d1770e9aa518049431b"
    const val POPUNDER_URL = "https://pl31001085.profitableratecpmnetwork.com/0a/46/b8/0a46b8ebc82f8e7138db80c9b0781365.js"

    const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
}

data class EarnKaroDeal(
    val id: String,
    val title: String,
    val subtitle: String,
    val discountBadge: String,
    val url: String,
    val category: String = "Shop Essentials"
)

object EarnKaroDealsRepository {
    val defaultDeals = listOf(
        // Adsterra High-CPM Smartlink
        EarnKaroDeal("adsterra_smart", "Exclusive Premium Sponsor Offer", "High CPM sponsored deals & top verified merchant tools", "HOT DEAL", AdsterraAdsConfig.SMARTLINK_URL, "Sponsor"),

        // Flipkart Special Mega Deals (fktr.in)
        EarnKaroDeal("f1", "Flipkart Super Saver Deals", "Electronics, gadgets & mobile accessories wholesale", "UP TO 75% OFF", "https://fktr.in/w8f0x7Z", "Electronics"),
        EarnKaroDeal("f2", "Smartphones & Store Tablets Fest", "Top brand business smartphones & POS tablets", "FLAT 40% OFF", "https://fktr.in/9q5xQ2b", "Mobiles"),
        EarnKaroDeal("f3", "Home & Kitchen Appliances Fest", "Commercial blenders, electric kettles & shop gear", "SAVE 65%", "https://fktr.in/ld9oTiH", "Appliances"),
        EarnKaroDeal("f4", "Wireless Earbuds & Audio Gear", "Noise cancelling wireless earphones & headsets", "FROM ₹499", "https://fktr.in/k5lm8dN", "Electronics"),
        EarnKaroDeal("f5", "Fashion, Footwear & Apparel Sale", "Trending wholesale clothing, shirts & trousers", "MIN 60% OFF", "https://fktr.in/hhAg4jV", "Fashion"),
        EarnKaroDeal("f6", "Laptops, Monitors & Workstations", "Heavy duty shop billing laptops & laser printers", "SAVE ₹15,000", "https://fktr.in/T05bvnP", "Electronics"),
        EarnKaroDeal("f7", "Smartwatches & Fitness Bands", "Bluetooth calling smartwatches & displays", "UP TO 80% OFF", "https://fktr.in/o19Apt3", "Gadgets"),
        EarnKaroDeal("f8", "Fast Power Banks & Type-C Cables", "20000mAh fast charge battery packs for counter", "FLAT 55% OFF", "https://fktr.in/2Ov33r6", "Accessories"),
        EarnKaroDeal("f9", "Store Furniture & Display Shelves", "Commercial racks, counter chairs & storage units", "BIG DISCOUNTS", "https://fktr.in/fr2frWc", "Store Gear"),
        EarnKaroDeal("f10", "Daily Grocery & Wholesale Pantry", "Bulk grocery packs, cooking essentials & supplies", "EXTRA 30% OFF", "https://fktr.in/0etyI0Z", "Grocery"),
        EarnKaroDeal("f11", "CCTV Cameras & Shop Surveillance", "HD Wi-Fi security cameras & night vision kits", "FLAT 50% OFF", "https://fktr.in/2nig94h", "Security"),
        EarnKaroDeal("f12", "Flipkart Brand Mall Mega Clearance", "100% genuine verified products with express delivery", "EXCLUSIVE", "https://fktr.in/16xm203", "Top Brands"),
        EarnKaroDeal("f13", "Men & Women Footwear Bonanza", "Comfortable work shoes, sneakers & daily footwear", "SAVE 60%", "https://fktr.in/LXu0xLG", "Fashion"),
        EarnKaroDeal("f14", "Personal Care & Grooming Kits", "Top grooming trimmers & wellness accessories", "FLAT 45% OFF", "https://fktr.in/wue9FZ1", "Personal Care"),
        EarnKaroDeal("f15", "Festival Grand Offer Bonanza", "Limited-time deals with highest cashback multipliers", "EXTRA CASHBACK", "https://fktr.in/98QOt1A", "Bonanza"),

        // Merchant Hardware & POS Deals (bitli.in)
        EarnKaroDeal("b1", "Thermal Billing Printer & Scanner", "Fast wireless receipt printer for shops & counters", "FLAT 60% OFF", "https://bitli.in/QHjuDl9", "Billing"),
        EarnKaroDeal("b2", "Premium Barcode & Thermal Paper Rolls", "Pack of 20 high-grade rolls at wholesale price", "SAVE 50%", "https://bitli.in/khNhi7y", "Billing"),
        EarnKaroDeal("b3", "All-in-One Smart POS Terminal", "Touchscreen billing & payment machine with printer", "SPECIAL OFFER", "https://bitli.in/bXcGwla", "Billing"),
        EarnKaroDeal("b4", "Merchant Business Cashback Offer", "Get instant cashback & rewards on store purchases", "UP TO ₹750 BACK", "https://bitli.in/953pjSJ", "Cashback"),
        EarnKaroDeal("b5", "Top Store Essentials & Retail Gear", "Best-selling retail supplies & accessories", "MEGA SALE", "https://bitli.in/DzjJE9f", "Store Gear"),
        EarnKaroDeal("b6", "Merchant Finance & Zero-Interest Credit", "Quick working capital credit for shopkeepers", "LOWEST RATES", "https://bitli.in/EbGRCa7", "Finance"),
        EarnKaroDeal("b7", "Wireless Bluetooth Label & Bill Printer", "Pocket-sized 58mm mobile receipt printer", "LIMITED TIME", "https://bitli.in/O8ol02i", "Billing"),
        EarnKaroDeal("b8", "Heavy Duty Electronic Cash Drawer", "Auto-spring lock bill & coin cash box", "FLAT 45% OFF", "https://bitli.in/27TLoy2", "Store Gear"),
        EarnKaroDeal("b9", "Store Electronics & Gadgets Flash Sale", "Laptops, phones & barcode scanners wholesale", "UP TO 70% OFF", "https://bitli.in/9R0HRD7", "Electronics"),
        EarnKaroDeal("b10", "Zero-Fee Soundbox & QR Stand", "Instant audio payment notification speaker", "FREE DELIVERY", "https://bitli.in/8t09DAs", "Payments"),
        EarnKaroDeal("b11", "Wholesale Shopkeeper Supplies", "Bulk buy grocery, fashion & tech inventory", "BULK DISCOUNT", "https://bitli.in/fASPoy4", "Wholesale"),
        EarnKaroDeal("b12", "Fast UPI Acrylic QR Stand & Holder", "Durable counter display for Google Pay/PhonePe", "BEST DEAL", "https://bitli.in/khPN9ML", "Payments"),
        EarnKaroDeal("b13", "Top Merchant Hardware & Software", "Cloud backup & thermal printer accessories", "SAVE BIG", "https://bitli.in/y8KsZYM", "Billing"),
        EarnKaroDeal("b14", "Special Merchant Festival Bonanza", "Limited-time deals with extra affiliate cash", "EXTRA CASHBACK", "https://bitli.in/3hehenm", "Bonanza")
    )

    fun getDeals(context: Context): List<EarnKaroDeal> = defaultDeals

    fun getRandomDeal(context: Context): EarnKaroDeal = defaultDeals.random()
}

fun openAffiliateUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Opening offer...", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Live Adsterra Banner Ad Component (468x60 / Responsive)
 * Loads the exact banner code from user's HTML: key '0e2646541d90aca6dc3d2cd09bc02a41'
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdsterraBannerAd(
    modifier: Modifier = Modifier,
    isProUser: Boolean = false
) {
    if (isProUser) return
    val context = LocalContext.current

    val bannerHtml = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
          * { box-sizing: border-box; margin: 0; padding: 0; }
          html, body { 
            background: transparent; 
            width: 100%;
            height: 100%;
            overflow: hidden;
            display: flex;
            justify-content: center;
            align-items: center;
          }
          iframe { max-width: 100% !important; border: 0 !important; }
        </style>
        </head>
        <body>
        <script type="text/javascript">
            atOptions = {
                'key' : '${AdsterraAdsConfig.BANNER_KEY}',
                'format' : 'iframe',
                'height' : 60,
                'width' : 468,
                'params' : {}
            };
        </script>
        <script type="text/javascript" src="${AdsterraAdsConfig.BANNER_INVOKE_URL}"></script>
        </body>
        </html>
    """.trimIndent()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("adsterra_banner_card")
            .clickable { openAffiliateUrl(context, AdsterraAdsConfig.SMARTLINK_URL) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFFBBF24).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Ad • Sponsored Adsterra Banner",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = "Ad Info",
                    tint = Color.Gray,
                    modifier = Modifier.size(13.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(0x00000000)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = AdsterraAdsConfig.MOBILE_USER_AGENT
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(true)
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    openAffiliateUrl(ctx, url)
                                    return true
                                }
                            }
                            loadDataWithBaseURL("https://www.highrevenueformat.com", bannerHtml, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Live Adsterra Native Ad Component
 * Loads exact Native Ad code: 'container-edd460171965647f099030c848d0ce48'
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdsterraNativeAd(
    modifier: Modifier = Modifier,
    isProUser: Boolean = false
) {
    if (isProUser) return
    val context = LocalContext.current

    val nativeHtml = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
          * { box-sizing: border-box; margin: 0; padding: 0; }
          html, body { 
            background: transparent; 
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            padding: 4px;
            width: 100%;
            height: 100%;
          }
          #${AdsterraAdsConfig.NATIVE_CONTAINER_ID} { width: 100% !important; min-height: 90px; }
          a { text-decoration: none; }
        </style>
        </head>
        <body>
        <script async="async" data-cfasync="false" src="${AdsterraAdsConfig.NATIVE_INVOKE_URL}"></script>
        <div id="${AdsterraAdsConfig.NATIVE_CONTAINER_ID}"></div>
        </body>
        </html>
    """.trimIndent()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("adsterra_native_card")
            .clickable { openAffiliateUrl(context, AdsterraAdsConfig.SMARTLINK_URL) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFFBBF24).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Native Ad • High CPM Sponsored",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(0xFFE9F8F0),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF15945B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 150.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setBackgroundColor(0x00000000)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = AdsterraAdsConfig.MOBILE_USER_AGENT
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(true)
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    openAffiliateUrl(ctx, url)
                                    return true
                                }
                            }
                            loadDataWithBaseURL("https://pl31537282.profitableratecpmnetwork.com", nativeHtml, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * High-CPM Banner Ad with Rotating Deals
 */
@Composable
fun AdMobBannerCard(
    modifier: Modifier = Modifier,
    isProUser: Boolean = false,
    onAdClick: () -> Unit = {}
) {
    if (isProUser) return
    val context = LocalContext.current
    var dealIndex by remember { mutableIntStateOf(0) }
    val deals = remember { EarnKaroDealsRepository.getDeals(context) }
    val currentDeal = deals.getOrElse(dealIndex % deals.size) { deals.first() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(7000)
            dealIndex = (dealIndex + 1) % deals.size
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("admob_banner_card")
            .clickable {
                openAffiliateUrl(context, currentDeal.url)
                onAdClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Sponsored • High CPM Deal",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFE9F8F0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = currentDeal.discountBadge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15945B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = "Open Deal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(BillGenOrange, BillGenOrangeLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = "Deal Icon",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentDeal.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = currentDeal.subtitle,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        openAffiliateUrl(context, currentDeal.url)
                        onAdClick()
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("admob_action_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    Text("Grab Deal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Native Ad Component for In-Feed lists
 */
@Composable
fun EarnKaroNativeAdCard(
    modifier: Modifier = Modifier,
    isProUser: Boolean = false
) {
    if (isProUser) return
    val context = LocalContext.current
    var dealIndex by remember { mutableIntStateOf(0) }
    val deals = remember { EarnKaroDealsRepository.getDeals(context) }
    val deal = deals.getOrElse(dealIndex % deals.size) { deals.first() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            dealIndex = (dealIndex + 1) % deals.size
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { openAffiliateUrl(context, deal.url) },
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
                Surface(
                    color = Color(0xFFFBBF24).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Native Ad • Featured Flipkart / Merchant Deal",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(0xFFE9F8F0),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = deal.discountBadge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF15945B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = deal.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = deal.subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { openAffiliateUrl(context, deal.url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B))
            ) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Offer on Flipkart / Store →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Interstitial Ad Dialog ("kabhi beech beech mein dikhana Interstitial maano")
 */
@Composable
fun EarnKaroInterstitialDialog(
    onDismiss: () -> Unit,
    onClaim: () -> Unit = {}
) {
    val context = LocalContext.current
    val deal = remember { EarnKaroDealsRepository.getRandomDeal(context) }
    var countdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Dialog(onDismissRequest = { if (countdown <= 0) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFFBBF24).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Interstitial Ad • Special Sponsor",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (countdown > 0) {
                        Text(
                            text = "Skip in $countdown s",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(BillGenOrange, Color(0xFF15945B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = Color(0xFFFFF0E9),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = deal.discountBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = BillGenOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = deal.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    text = deal.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        openAffiliateUrl(context, deal.url)
                        onClaim()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("interstitial_claim_deal"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Claim Deal Now →", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (countdown <= 0) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Continue to Bill", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * Rewarded Ad Dialog
 */
@Composable
fun RewardedAdDialog(
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(5) }
    var isCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        isCompleted = true
    }

    Dialog(onDismissRequest = { if (isCompleted) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Rewarded Ad • Sponsored Sponsor",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(BillGenOrange, Color(0xFF15945B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Reward",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Unlocking AI Screenshot Scans",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (!isCompleted) "Reward unlocks in $countdown seconds..." else "Reward Ready! You earned +5 Free AI Scans.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!isCompleted) {
                    LinearProgressIndicator(
                        progress = { (5 - countdown) / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BillGenOrange
                    )
                } else {
                    Button(
                        onClick = {
                            onRewardEarned()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("claim_reward_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15945B))
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Claim +5 Free Scans", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Pro Upgrade Dialog
 */
@Composable
fun ProUpgradeDialog(
    onDismiss: () -> Unit,
    onUpgradeSuccess: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = BillGenOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "⭐ BILLGEN PRO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = BillGenOrange,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Supercharge Your Store",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Professional billing without limits",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProFeatureRow("🚫 100% Ad-Free Experience")
                    ProFeatureRow("⚡ Unlimited Gemini AI Screenshot Scans")
                    ProFeatureRow("🎨 All 15 Premium Invoice Templates")
                    ProFeatureRow("🖨 Wireless & 80mm Thermal Printer Support")
                    ProFeatureRow("☁ Automatic Local & Backup Sync")
                    ProFeatureRow("💼 Custom Watermark & Brand Logo on PDFs")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onUpgradeSuccess()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pro_upgrade_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BillGenOrange)
                ) {
                    Text("Unlock Pro • ₹99 / Month", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Continue with Free Version", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF15945B),
            modifier = Modifier.size(16.dp)
        )
        Text(text = text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
