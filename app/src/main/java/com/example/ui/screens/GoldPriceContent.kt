package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GoldBankRecycleEntry
import com.example.data.model.GoldBrandEntry
import com.example.data.model.GoldMarketEntry
import com.example.ui.viewmodel.GoldPriceUiState

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
                        painter = painterResource(R.drawable.ic_warning),
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
private fun GoldSummaryCard(
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
private fun GoldSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 8.dp, start = 4.dp),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun GoldMarketCard(
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
                        // 国内习惯：涨红跌绿，持平用中性色
                        color = when {
                            entry.changeRate.startsWith("\u2191") -> Color(0xFFD32F2F)
                            entry.changeRate.startsWith("\u2193") -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
private fun GoldMetric(label: String, value: String) {
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
private fun GoldBrandCard(
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
private fun GoldBankRecycleCard(
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
                    text = listOf(entry.type, entry.updated).filter { it.isNotBlank() }.joinToString(" \u00B7 "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SelectionContainer {
                Text(
                    text = entry.price,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFFD4A017)
                )
            }
        }
    }
}

