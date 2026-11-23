package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppUpdate
import com.example.data.model.HotPlatform
import com.example.data.model.PlatformCategory
import com.example.ui.theme.AppSkin
import com.example.ui.theme.isDark
import com.example.ui.viewmodel.GoldPriceViewModel
import com.example.ui.viewmodel.HotSearchViewModel
import com.example.ui.viewmodel.News60sViewModel
import com.example.ui.viewmodel.OilPriceViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WeatherAlertViewModel

enum class DashboardMode(
    val shortLabel: String,
    val title: String,
    val emoji: String,
    val color: Color
) {
    NEWS_60S("60\u79D2", "60\u79d2\u8bfb\u4e16\u754c", "\ud83d\udcf0", Color(0xFF2196F3)),
    HOT_SEARCH("\u70ed\u641c", "\u591a\u5e73\u53f0\u70ed\u641c", "", Color(0xFFFF6B35)),
    WEATHER_ALERT("\u9884\u8b66", "\u6c14\u8c61\u9884\u8b66", "\u26a0\ufe0f", Color(0xFF00ACC1)),
    OIL_PRICE("\u6cb9\u4ef7", "\u6cb9\u4ef7\u67e5\u8be2", "\u26fd", Color(0xFFFF6B35)),
    GOLD_PRICE("\u91d1\u4ef7", "\u91d1\u4ef7\u67e5\u8be2", "\ud83e\ude99", Color(0xFFD4A017));

    fun accentColor(activePlatform: HotPlatform): Color =
        if (this == HOT_SEARCH) activePlatform.brandColor else color

    fun badge(activePlatform: HotPlatform): String =
        if (this == HOT_SEARCH) activePlatform.infoEmoji else emoji
}

@Composable
fun DailyHotDashboard(
    hotViewModel: HotSearchViewModel,
    oilViewModel: OilPriceViewModel,
    goldViewModel: GoldPriceViewModel,
    news60sViewModel: News60sViewModel,
    weatherAlertViewModel: WeatherAlertViewModel,
    skin: AppSkin = AppSkin.SYSTEM,
    onSkinChange: (AppSkin) -> Unit = {},
    versionLabel: String = "",
    onCheckUpdate: () -> Unit = {},
    availableUpdate: AppUpdate? = null,
    isResolvingMirror: Boolean = false,
    onDismissUpdate: () -> Unit = {},
    onIgnoreUpdate: () -> Unit = {},
    onMirrorDownload: () -> Unit = {},
    onGithubDownload: () -> Unit = {},
    onBackupDownload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activePlatform by hotViewModel.activePlatform.collectAsState()
    val searchQuery by hotViewModel.searchQuery.collectAsState()
    val uiState by hotViewModel.uiState.collectAsState()
    val oilState by oilViewModel.uiState.collectAsState()
    val goldState by goldViewModel.uiState.collectAsState()
    val selectedProvince by oilViewModel.selectedProvince.collectAsState()
    val news60sState by news60sViewModel.uiState.collectAsState()
    val weatherAlertState by weatherAlertViewModel.uiState.collectAsState()
    var mode by rememberSaveable { mutableStateOf(DashboardMode.NEWS_60S) }
    var selectedPlatformCategory by rememberSaveable { mutableStateOf(PlatformCategory.ALL) }

    val context = LocalContext.current

    // 内置浏览器预览状态
    var previewUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var previewTitle by rememberSaveable { mutableStateOf<String?>(null) }

    // 刷新按钮旋转动画
    var isRotating by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotating) 360f else 0f,
        animationSpec = tween(durationMillis = 650),
        finishedListener = { isRotating = false }
    )

    // 预览打开时，返回键先关闭预览（网页内有历史时由预览页自行后退）
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                HeaderSection(
                    activePlatform = activePlatform,
                    mode = mode,
                    skin = skin,
                    onSkinChange = onSkinChange,
                    versionLabel = versionLabel,
                    onCheckUpdate = onCheckUpdate,
                    onModeChange = { mode = it },
                    onRefresh = {
                        isRotating = true
                        when (mode) {
                            DashboardMode.HOT_SEARCH -> hotViewModel.refreshActivePlatform()
                            DashboardMode.WEATHER_ALERT -> weatherAlertViewModel.refresh(force = true)
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
                    DashboardMode.WEATHER_ALERT -> {
                        WeatherAlertContent(
                            state = weatherAlertState,
                            onRefresh = { weatherAlertViewModel.refresh(force = true) },
                            onQuery = { weatherAlertViewModel.queryCity(it) }
                        )
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
                                            if (item.url.isNullOrBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "\u8FD9\u6761\u70ED\u641C\u6CA1\u6709\u53EF\u6253\u5F00\u7684\u94FE\u63A5",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                previewUrl = item.url
                                                previewTitle = item.title
                                            }
                                        },
                                        onCopyItem = { item ->
                                            copyToClipboard(context, listOfNotNull(item.title, item.url).joinToString(" "))
                                        },
                                        onShareItem = { item ->
                                            shareText(context, "\u3010\u805A\u5408\u70ED\u641C\u00B7${activePlatform.displayName}\u3011${item.title}\uFF1A${item.url ?: ""}")
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
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 250)
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
        }
    }

    availableUpdate?.let { update ->
        UpdateAvailableDialog(
            update = update,
            isResolvingMirror = isResolvingMirror,
            onDismiss = onDismissUpdate,
            onIgnore = onIgnoreUpdate,
            onMirrorDownload = onMirrorDownload,
            onGithubDownload = onGithubDownload,
            onBackupDownload = onBackupDownload
        )
    }
}

/**
 * 顶部栏：左侧应用名和版本号（点击检查更新），右侧皮肤和刷新按钮，下方为页面切换栏。
 * 当前页面已在切换栏中高亮，不再在标题旁重复显示。
 */
@Composable
fun HeaderSection(
    activePlatform: HotPlatform,
    mode: DashboardMode,
    skin: AppSkin = AppSkin.SYSTEM,
    onSkinChange: (AppSkin) -> Unit = {},
    versionLabel: String = "",
    onCheckUpdate: () -> Unit = {},
    onModeChange: (DashboardMode) -> Unit,
    onRefresh: () -> Unit,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u805A\u5408\u667A\u8BAF",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (versionLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onCheckUpdate,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "v$versionLabel",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SkinMenuButton(skin = skin, onSkinChange = onSkinChange)
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "\u5237\u65B0\u5F53\u524D\u9875\u9762",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        ModeToggle(
            mode = mode,
            onModeChange = onModeChange
        )
    }
}

@Composable
private fun SkinMenuButton(
    skin: AppSkin,
    onSkinChange: (AppSkin) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                painter = painterResource(if (skin.isDark()) R.drawable.ic_dark_mode else R.drawable.ic_light_mode),
                contentDescription = "\u5207\u6362\u76AE\u80A4\uFF0C\u5F53\u524D\uFF1A${skin.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppSkin.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.label) },
                    leadingIcon = { RadioButton(selected = option == skin, onClick = null) },
                    onClick = {
                        expanded = false
                        if (option != skin) onSkinChange(option)
                    }
                )
            }
        }
    }
}

@Composable
fun ModeToggle(
    mode: DashboardMode,
    onModeChange: (DashboardMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardMode.entries.forEach { option ->
                ModeToggleItem(
                    option = option,
                    selected = mode == option,
                    onSelected = { onModeChange(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 选中项使用卡片底色 + 细边框 + 加粗文字，各皮肤下对比度一致，不使用大面积彩色块。 */
@Composable
private fun ModeToggleItem(
    option: DashboardMode,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(durationMillis = 150)
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 150)
    )
    val borderColor = if (selected) MaterialTheme.colorScheme.outlineVariant else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .selectable(
                selected = selected,
                onClick = onSelected,
                role = Role.Tab
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.shortLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
