package com.example.ui.screens

import java.util.Locale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.HotPlatform
import com.example.data.model.HotSearchItem
import com.example.data.model.PlatformCategory

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
    subtitle: String = "从 dailyhotapi 节点拉取中",
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
            text = subtitle,
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
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.title + (item.url ?: "") }
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

/**
 * 热搜条目：点击打开预览；复制、分享按钮直接显示，不再依赖长按展开这种不易发现的手势。
 * 前三名用平台色标出排名，其余排名使用中性色。
 */
@Composable
private fun TrendItemCard(
    rank: Int,
    item: HotSearchItem,
    platform: HotPlatform,
    onClicked: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTopThree = rank <= 3

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClicked),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 14.dp, top = 12.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isTopThree) platform.brandColor.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isTopThree) platform.brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    modifier = Modifier.padding(end = 10.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                val displayDesc = item.desc?.trim()
                if (!displayDesc.isNullOrBlank() && displayDesc != "-" && displayDesc != "\u8BE5\u89C6\u9891\u6682\u65E0\u7B80\u4ECB") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayDesc,
                        modifier = Modifier.padding(end = 10.dp),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.hot.isNullOrBlank()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_whatshot),
                            contentDescription = "\u70ED\u5EA6",
                            tint = platform.brandColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formatHotNumber(item.hot),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onCopy, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_content_copy),
                            contentDescription = "\u590D\u5236\u6807\u9898\u548C\u94FE\u63A5",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = "\u5206\u4EAB",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
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
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = "\u9519\u8BEF",
            tint = platform.brandColor,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\u52A0\u8F7D\u5931\u8D25",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
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
                    text = "\u91CD\u65B0\u52A0\u8F7D",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

private fun formatHotNumber(hotStr: String): String {
    return hotStr.toDoubleOrNull()?.let { number ->
        when {
            number >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", number / 1_000_000)
            number >= 10_000 -> String.format(Locale.getDefault(), "%.1f\u4E07", number / 10_000)
            number >= 1000 -> String.format(Locale.getDefault(), "%.1fk", number / 1000)
            else -> hotStr
        }
    } ?: hotStr
}
