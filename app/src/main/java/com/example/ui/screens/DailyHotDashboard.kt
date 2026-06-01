package com.example.ui.screens

import android.content.Context
import android.content.Intent
import java.util.Locale
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GoldBankRecycleEntry
import com.example.data.model.GoldBrandEntry
import com.example.data.model.GoldMarketEntry
import com.example.data.model.HotPlatform
import com.example.data.model.HotSearchItem
import com.example.data.model.OilPriceEntry
import com.example.data.model.PlatformCategory
import com.example.data.model.PROVINCES
import com.example.ui.viewmodel.AiChatViewModel
import com.example.ui.viewmodel.GoldPriceUiState
import com.example.ui.viewmodel.GoldPriceViewModel
import com.example.ui.viewmodel.HotSearchViewModel
import com.example.ui.viewmodel.OilPriceUiState
import com.example.ui.viewmodel.OilPriceViewModel
import com.example.ui.viewmodel.News60sUiState
import com.example.ui.viewmodel.News60sViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DashboardMode {
    NEWS_60S, HOT_SEARCH, OIL_PRICE, GOLD_PRICE
}

@Composable
fun DailyHotDashboard(
    hotViewModel: HotSearchViewModel,
    oilViewModel: OilPriceViewModel,
    goldViewModel: GoldPriceViewModel,
    news60sViewModel: News60sViewModel,
    aiChatViewModel: AiChatViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activePlatform by hotViewModel.activePlatform.collectAsState()
    val searchQuery by hotViewModel.searchQuery.collectAsState()
    val uiState by hotViewModel.uiState.collectAsState()
    val oilState by oilViewModel.uiState.collectAsState()
    val goldState by goldViewModel.uiState.collectAsState()
    val selectedProvince by oilViewModel.selectedProvince.collectAsState()
    val news60sState by news60sViewModel.uiState.collectAsState()
    var mode by remember { mutableStateOf(DashboardMode.NEWS_60S) }
    var selectedPlatformCategory by remember { mutableStateOf(PlatformCategory.ALL) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Web view preview state
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var previewTitle by remember { mutableStateOf<String?>(null) }

    // Floating action rotation animation state
    var isRotating by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotating) 360f else 0f,
        animationSpec = tween(durationMillis = 650),
        finishedListener = { isRotating = false }
    )

    // Update AI chat context when data changes
    LaunchedEffect(uiState, news60sState, oilState, goldState, selectedProvince) {
        val hotItems = (uiState as? UiState.Success)?.items?.take(15) ?: emptyList()
        val newsList = (news60sState as? News60sUiState.Success)?.newsList ?: emptyList()
        val oilEntries = (oilState as? OilPriceUiState.Success)?.entries ?: emptyList()
        val oilProvince = (oilState as? OilPriceUiState.Success)?.province
        val goldMarkets = (goldState as? GoldPriceUiState.Success)?.snapshot?.let {
            it.domesticMarkets + it.internationalMarkets
        } ?: emptyList()
        aiChatViewModel.updateContext(
            com.example.ui.viewmodel.AiContext(
                hotItems = hotItems,
                news60s = newsList,
                oilProvince = oilProvince,
                oilEntries = oilEntries,
                goldMarkets = goldMarkets
            )
        )
    }

    // Back handler to close native WebView preview
    if (previewUrl != null) {
        BackHandler {
            previewUrl = null
            previewTitle = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activePlatform.brandColor.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, 200f),
                            radius = size.width * 0.8f
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                HeaderSection(
                    activePlatform = activePlatform,
                    mode = mode,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onModeChange = { mode = it },
                    onRefresh = {
                        isRotating = true
                        when (mode) {
                            DashboardMode.HOT_SEARCH -> hotViewModel.refreshActivePlatform()
                            DashboardMode.OIL_PRICE -> oilViewModel.refresh()
                            DashboardMode.GOLD_PRICE -> goldViewModel.refresh()
                            DashboardMode.NEWS_60S -> news60sViewModel.refresh()
                        }
                    },
                    rotationAngle = rotationAngle
                )

                when (mode) {
                    DashboardMode.NEWS_60S -> {
                        News60sContent(news60sState = news60sState, onRefresh = { news60sViewModel.refresh() })
                    }
                    DashboardMode.HOT_SEARCH -> {
                        PlatformCategoryBar(
                            selectedCategory = selectedPlatformCategory,
                            onSelected = { selectedPlatformCategory = it }
                        )

                        PlatformsBar(
                            activePlatform = activePlatform,
                            platforms = HotPlatform.platformsByCategory(selectedPlatformCategory),
                            onSelected = { hotViewModel.selectPlatform(it) }
                        )

                        SearchSection(
                            query = searchQuery,
                            onQueryChanged = { hotViewModel.updateSearchQuery(it) },
                            platform = activePlatform
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (val state = uiState) {
                                is UiState.Loading -> {
                                    LoadingStateView(platform = activePlatform)
                                }
                                is UiState.Success -> {
                                    SuccessStateView(
                                        platform = activePlatform,
                                        items = state.items,
                                        updateTime = state.updateTime,
                                        onItemClicked = { item ->
                                            if (item.url != null) {
                                                previewUrl = item.url
                                                previewTitle = item.title ?: "\u70ED\u641C\u8BE6\u60C5"
                                            }
                                        },
                                        onCopyItem = { item ->
                                            copyToClipboard(context, (item.title ?: "\u6682\u65E0\u6807\u9898") + " " + (item.url ?: ""))
                                        },
                                        onShareItem = { item ->
                                            shareText(context, "\u3010\u805A\u5408\u70ED\u641C\u00B7${activePlatform.displayName}\u3011${item.title ?: "\u6682\u65E0\u6807\u9898"}\uFF1A${item.url ?: ""}")
                                        }
                                    )
                                }
                                is UiState.Error -> {
                                    ErrorStateView(
                                        message = state.message,
                                        onRetry = { hotViewModel.refreshActivePlatform() },
                                        platform = activePlatform
                                    )
                                }
                            }
                        }
                    }
                    DashboardMode.OIL_PRICE -> {
                        OilPriceContent(
                            oilState = oilState,
                            selectedProvince = selectedProvince,
                            onProvinceSelected = { oilViewModel.selectProvince(it) },
                            onRefresh = { oilViewModel.refresh() }
                        )
                    }
                    DashboardMode.GOLD_PRICE -> {
                        GoldPriceContent(
                            goldState = goldState,
                            onRefresh = { goldViewModel.refresh() }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = previewUrl != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 350)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut()
            ) {
                if (previewUrl != null) {
                    InAppBrowserPreview(
                        url = previewUrl!!,
                        title = previewTitle ?: "\u70ED\u641C\u8BE6\u60C5",
                        brandColor = activePlatform.brandColor,
                        onClose = {
                            previewUrl = null
                            previewTitle = null
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                AiChatFab(viewModel = aiChatViewModel)
            }
        }
    }
}

@Composable
fun HeaderSection(
    activePlatform: HotPlatform,
    mode: DashboardMode,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onModeChange: (DashboardMode) -> Unit,
    onRefresh: () -> Unit,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "\u805A\u5408\u70ED\u641C",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (mode) {
                                DashboardMode.NEWS_60S -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                DashboardMode.HOT_SEARCH -> activePlatform.brandColor.copy(alpha = 0.15f)
                                DashboardMode.OIL_PRICE -> Color(0xFFFF6B35).copy(alpha = 0.15f)
                                DashboardMode.GOLD_PRICE -> Color(0xFFD4A017).copy(alpha = 0.16f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (mode) {
                            DashboardMode.NEWS_60S -> "\uD83D\uDCF0"
                            DashboardMode.HOT_SEARCH -> activePlatform.infoEmoji
                            DashboardMode.OIL_PRICE -> "\u26FD"
                            DashboardMode.GOLD_PRICE -> "\uD83E\uDE99"
                        },
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = when (mode) {
                        DashboardMode.NEWS_60S -> "60\u79D2\u8BFB\u4E16\u754C"
                        DashboardMode.HOT_SEARCH -> "\u591A\u5E73\u53F0\u70ED\u641C"
                        DashboardMode.OIL_PRICE -> "\u6CB9\u4EF7\u67E5\u8BE2"
                        DashboardMode.GOLD_PRICE -> "\u91D1\u4EF7\u67E5\u8BE2"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onToggleTheme() }
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (isDarkTheme) "\u2600\uFE0F" else "\uD83C\uDF19",
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onRefresh() }
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "\u5237\u65B0",
                        tint = when (mode) {
                            DashboardMode.NEWS_60S -> Color(0xFF2196F3)
                            DashboardMode.HOT_SEARCH -> activePlatform.brandColor
                            DashboardMode.OIL_PRICE -> Color(0xFFFF6B35)
                            DashboardMode.GOLD_PRICE -> Color(0xFFD4A017)
                        },
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeToggle(
                mode = mode,
                onModeChange = onModeChange
            )
        }
    }
}

@Composable
fun ModeToggle(
    mode: DashboardMode,
    onModeChange: (DashboardMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val news60sBg by animateColorAsState(
        targetValue = if (mode == DashboardMode.NEWS_60S) Color(0xFF2196F3) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 200)
    )
    val hotBg by animateColorAsState(
        targetValue = if (mode == DashboardMode.HOT_SEARCH) Color(0xFFFF6B35) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 200)
    )
    val oilBg by animateColorAsState(
        targetValue = if (mode == DashboardMode.OIL_PRICE) Color(0xFFFF6B35) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 200)
    )
    val goldBg by animateColorAsState(
        targetValue = if (mode == DashboardMode.GOLD_PRICE) Color(0xFFD4A017) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 200)
    )
    val news60sContent by animateColorAsState(
        targetValue = if (mode == DashboardMode.NEWS_60S) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200)
    )
    val hotContent by animateColorAsState(
        targetValue = if (mode == DashboardMode.HOT_SEARCH) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200)
    )
    val oilContent by animateColorAsState(
        targetValue = if (mode == DashboardMode.OIL_PRICE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200)
    )
    val goldContent by animateColorAsState(
        targetValue = if (mode == DashboardMode.GOLD_PRICE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200)
    )

    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(news60sBg)
                    .clickable { onModeChange(DashboardMode.NEWS_60S) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "60S",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = news60sContent
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(hotBg)
                    .clickable { onModeChange(DashboardMode.HOT_SEARCH) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u70ED\u641C",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = hotContent
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(oilBg)
                    .clickable { onModeChange(DashboardMode.OIL_PRICE) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u6CB9\u4EF7",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = oilContent
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(goldBg)
                    .clickable { onModeChange(DashboardMode.GOLD_PRICE) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u91D1\u4EF7",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = goldContent
                )
            }
        }
    }
}

@Composable
fun OilPriceContent(
    oilState: OilPriceUiState,
    selectedProvince: String,
    onProvinceSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        ProvinceSelector(
            selectedProvince = selectedProvince,
            onProvinceSelected = onProvinceSelected
        )

        when (val state = oilState) {
            is OilPriceUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF6B35),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "\u6B63\u5728\u67E5\u8BE2 ${selectedProvince} \u5B9E\u65F6\u6CB9\u4EF7...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            is OilPriceUiState.Success -> {
                val oilColor = Color(0xFFFF6B35)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = oilColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${selectedProvince}\u7701\u5B9E\u65F6\u6CB9\u4EF7",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = state.entries,
                        key = { _, entry -> entry.label }
                    ) { _, entry ->
                        OilPriceCard(entry = entry, accentColor = oilColor)
                    }
                }
            }
            is OilPriceUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onRefresh() },
                            color = Color(0xFFFF6B35),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "\u91CD\u65B0\u52A0\u8F7D",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoldPriceContent(
    goldState: GoldPriceUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goldColor = Color(0xFFD4A017)
    when (val state = goldState) {
        is GoldPriceUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = goldColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "\u6B63\u5728\u83B7\u53D6\u5B9E\u65F6\u91D1\u4EF7...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
        is GoldPriceUiState.Success -> {
            val snapshot = state.snapshot
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    GoldSummaryCard(
                        source = snapshot.source,
                        updateTime = snapshot.updateTime,
                        accentColor = goldColor
                    )
                }

                if (snapshot.domesticMarkets.isNotEmpty()) {
                    item { GoldSectionTitle(text = "\u56FD\u5185\u8D35\u91D1\u5C5E") }
                    itemsIndexed(snapshot.domesticMarkets, key = { _, item -> "domestic-${item.name}" }) { _, item ->
                        GoldMarketCard(entry = item, accentColor = goldColor)
                    }
                }

                if (snapshot.internationalMarkets.isNotEmpty()) {
                    item { GoldSectionTitle(text = "\u56FD\u9645\u5E02\u573A") }
                    itemsIndexed(snapshot.internationalMarkets, key = { _, item -> "international-${item.name}" }) { _, item ->
                        GoldMarketCard(entry = item, accentColor = Color(0xFF5B8DEF))
                    }
                }

                if (snapshot.brands.isNotEmpty()) {
                    item { GoldSectionTitle(text = "\u54C1\u724C\u91D1\u5E97") }
                    itemsIndexed(snapshot.brands.take(30), key = { index, item -> "brand-$index-${item.brand}" }) { _, item ->
                        GoldBrandCard(entry = item)
                    }
                }

                if (snapshot.bankRecycle.isNotEmpty()) {
                    item { GoldSectionTitle(text = "\u94F6\u884C\u91D1\u6761 / \u56DE\u6536\u4EF7") }
                    itemsIndexed(snapshot.bankRecycle.take(30), key = { index, item -> "bank-$index-${item.name}" }) { _, item ->
                        GoldBankRecycleCard(entry = item)
                    }
                }
            }
        }
        is GoldPriceUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = goldColor,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRefresh() },
                        color = goldColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "\u91CD\u65B0\u52A0\u8F7D",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoldSummaryCard(
    source: String,
    updateTime: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83E\uDE99", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\u5B9E\u65F6\u91D1\u4EF7\u884C\u60C5",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "\u6765\u6E90: $source",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "\u66F4\u65B0: $updateTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GoldSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 8.dp, start = 4.dp),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun GoldMarketCard(
    entry: GoldMarketEntry,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = entry.sellPrice,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = accentColor
                    )
                    Text(
                        text = entry.changeRate,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (entry.changeRate.startsWith("\u2193")) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GoldMetric("\u5F00\u76D8", entry.openPrice)
                GoldMetric("\u6700\u9AD8", entry.highPrice)
                GoldMetric("\u6700\u4F4E", entry.lowPrice)
            }
        }
    }
}

@Composable
fun GoldMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GoldBrandCard(
    entry: GoldBrandEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.brand,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.updated,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GoldMetric("\u9EC4\u91D1", entry.goldPrice)
                GoldMetric("\u91D1\u6761", entry.bullionPrice)
                GoldMetric("\u94C2\u91D1", entry.platinumPrice)
            }
        }
    }
}

@Composable
fun GoldBankRecycleCard(
    entry: GoldBankRecycleEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.type}  ${entry.updated}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = entry.price,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = Color(0xFFD4A017)
            )
        }
    }
}

@Composable
fun News60sContent(
    news60sState: News60sUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (val state = news60sState) {
            is News60sUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF2196F3),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "60秒读世界加载中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            is News60sUiState.Success -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.newsList.forEachIndexed { index, news ->
                        News60sCard(index = index + 1, content = news)
                    }
                }
            }
            is News60sUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("\u91CD\u8BD5")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun News60sCard(
    index: Int,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2196F3).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2196F3)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ProvinceSelector(
    selectedProvince: String,
    onProvinceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "\u9009\u62E9\u7701\u4EFD",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedProvince,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "\u25BC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(300.dp)
        ) {
            PROVINCES.forEach { province ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = province,
                            fontWeight = if (province == selectedProvince) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onProvinceSelected(province)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun OilPriceCard(
    entry: OilPriceEntry,
    accentColor: Color = Color(0xFFFF6B35),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "\uFFE5${entry.price} /L",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = accentColor
            )
        }
    }
}

@Composable
fun PlatformCategoryBar(
    selectedCategory: PlatformCategory,
    onSelected: (PlatformCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(PlatformCategory.values().size) { index ->
            val cat = PlatformCategory.values()[index]
            val count = if (cat == PlatformCategory.ALL) HotPlatform.values().size
                        else HotPlatform.values().count { it.category == cat }
            if (count == 0) return@items
            val isSelected = selectedCategory == cat
            val chipBg by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFF6B35) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                animationSpec = tween(durationMillis = 200)
            )
            val chipContentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 200)
            )

            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelected(cat) },
                color = chipBg,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${cat.displayName} ($count)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = chipContentColor
                    )
                }
            }
        }
    }
}

@Composable
fun PlatformsBar(
    activePlatform: HotPlatform,
    platforms: List<HotPlatform> = HotPlatform.values().toList(),
    onSelected: (HotPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(platforms.size) { index ->
            val platform = platforms[index]
            val isSelected = activePlatform == platform
            
            val chipBg by animateColorAsState(
                targetValue = if (isSelected) platform.brandColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                animationSpec = tween(durationMillis = 200)
            )
            val chipContentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 200)
            )

            Surface(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelected(platform) },
                color = chipBg,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = platform.infoEmoji,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = platform.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = chipContentColor
                    )
                }
            }
        }
    }
}

@Composable
fun SearchSection(
    query: String,
    onQueryChanged: (String) -> Unit,
    platform: HotPlatform,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        placeholder = {
            Text(
                text = "\u6B63\u5728\u8FC7\u6EE4 ${platform.displayName} \u7684\u70ED\u5EA6\u8D8B\u52BF...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = platform.brandColor.copy(alpha = 0.8f)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "\u6E05\u9664\u641C\u7D22",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = platform.brandColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun LoadingStateView(
    platform: HotPlatform,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = platform.brandColor,
            strokeWidth = 3.dp,
            modifier = Modifier.size(46.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\u6B63\u5728\u540C\u6B65 ${platform.displayName} \u7684\u5B9E\u65F6\u6570\u636E...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = "\u4ECE dailyhotapi \u8282\u70B9\u62C9\u53D6\u4E2D",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SuccessStateView(
    platform: HotPlatform,
    items: List<HotSearchItem>,
    updateTime: String?,
    onItemClicked: (HotSearchItem) -> Unit,
    onCopyItem: (HotSearchItem) -> Unit,
    onShareItem: (HotSearchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        EmptyStateView()
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u5171\u540C\u6355\u6349 ${items.size} \u6761\u70ED\u641C\u52A8\u6001",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = "\u66F4\u65B0\u65F6\u95F4: ${updateTime ?: "\u521A\u521A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> (item.title ?: "") + (item.url ?: "") }
                ) { index, item ->
                    TrendItemCard(
                        rank = index + 1,
                        item = item,
                        platform = platform,
                        onClicked = { onItemClicked(item) },
                        onCopy = { onCopyItem(item) },
                        onShare = { onShareItem(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrendItemCard(
    rank: Int,
    item: HotSearchItem,
    platform: HotPlatform,
    onClicked: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    var isExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClicked,
                onLongClick = { isExpanded = !isExpanded }
            ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (rank <= 3) {
                                Brush.linearGradient(
                                    colors = listOf(rankColor, rankColor.copy(alpha = 0.7f))
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title ?: "\u6682\u65E0\u6807\u9898",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) 5 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val coverMedia = resolveCoverUrl(platform, item.cover ?: item.pic)
                    if (!coverMedia.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverMedia)
                                .crossfade(true)
                                .build(),
                            contentDescription = "\u70ED\u70B9\u5C01\u9762\u56FE",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = android.R.drawable.stat_notify_error),
                            fallback = painterResource(id = android.R.drawable.stat_notify_error)
                        )
                    }

                    val displayDesc = item.desc?.trim()
                    if (!displayDesc.isNullOrBlank() && displayDesc != "-" && displayDesc != "\u8BE5\u89C6\u9891\u6682\u65E0\u7B80\u4ECB") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = displayDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = if (isExpanded) 10 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val authorLabel = item.author?.trim()
                            if (!authorLabel.isNullOrBlank()) {
                                Text(
                                    text = "@$authorLabel",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = platform.brandColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                            } else {
                                Text(
                                    text = platform.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = platform.brandColor.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (item.hot != null && item.hot.value.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "\u70ED\u5EA6",
                                    tint = platform.brandColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatHotNumber(item.hot.value),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "\u590D\u5236\u94FE\u63A5",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "\u5206\u4EAB\u70ED\u70B9",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "\u6CA1\u6709\u627E\u5230\u7B26\u5408\u6761\u4EF6\u7684\u70ED\u641C",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "\u5C1D\u8BD5\u4FEE\u6539\u6216\u6E05\u9664\u4F60\u7684\u641C\u7D22\u8BCD",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    platform: HotPlatform,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "\u9519\u8BEF",
            tint = platform.brandColor,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\u6293\u53D6\u9047\u5230\u5F02\u5E38",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        Surface(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onRetry() },
            color = platform.brandColor,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "\u91CD\u65B0\u52A0\u8F7D\u6570\u636E",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun InAppBrowserPreview(
    url: String,
    title: String,
    brandColor: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webProgress by remember { mutableStateOf(0f) }
    var rootWebView by remember { mutableStateOf<WebView?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "\u8FD4\u56DE",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = {
                    copyToClipboard(context, url)
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "\u590D\u5236\u94FE\u63A5",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = {
                    openInBrowser(context, url)
                }) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "\u7CFB\u7EDF\u9ED8\u8BA4\u6D4F\u89C8\u5668\u6253\u5F00",
                        tint = brandColor
                    )
                }
            }

            if (webProgress > 0f && webProgress < 1f) {
                LinearProgressIndicator(
                    progress = { webProgress },
                    color = brandColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            supportZoom()
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val requestUrl = request?.url?.toString() ?: ""
                                return if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                                    false
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                                        ctx.startActivity(intent)
                                    } catch (e: Exception) {
                                    }
                                    true
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                webProgress = newProgress / 100f
                            }
                        }
                        loadUrl(url)
                        rootWebView = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

fun resolveCoverUrl(platform: HotPlatform, rawUrl: String?): String? {
    if (rawUrl.isNullOrBlank()) return null
    if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) return rawUrl
    if (rawUrl.startsWith("//")) return "https:$rawUrl"

    return when (platform) {
        HotPlatform.SSPAI -> "https://cdn.sspai.com/$rawUrl"
        else -> rawUrl
    }
}

fun formatHotNumber(hotStr: String): String {
    return try {
        val num = hotStr.toDouble()
        when {
            num >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", num / 1_000_000)
            num >= 10_000 -> String.format(Locale.getDefault(), "%.1f\u4E07", num / 10_000)
            num >= 1000 -> String.format(Locale.getDefault(), "%.1fk", num / 1000)
            else -> hotStr
        }
    } catch (e: Exception) {
        hotStr
    }
}

fun copyToClipboard(context: Context, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("hot_search_link", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "\u94FE\u63A5\u4E0E\u6807\u9898\u5DF2\u590D\u5236\u5230\u526A\u8D34\u677F", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "\u590D\u5236\u5931\u8D25", Toast.LENGTH_SHORT).show()
    }
}

fun shareText(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "\u53D1\u9001\u70ED\u641C\u81F3"))
    } catch (e: Exception) {
        Toast.makeText(context, "\u5206\u4EAB\u5931\u8D25", Toast.LENGTH_SHORT).show()
    }
}

fun openInBrowser(context: Context, url: String) {
    try {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "\u627E\u4E0D\u5230\u53EF\u4EE5\u6253\u5F00\u6D4F\u89C8\u5668\u7A0B\u5E8F\u7684\u5E94\u7528", Toast.LENGTH_SHORT).show()
    }
}
