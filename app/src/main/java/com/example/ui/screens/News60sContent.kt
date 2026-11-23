package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.News60sUiState

@Composable
fun News60sContent(
    news60sState: News60sUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                val allNewsText = remember(state.newsList) {
                    state.newsList.mapIndexed { index, news ->
                        "${index + 1}. $news"
                    }.joinToString("\n")
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 顶部信息行：左侧显示日期和条数，右侧是带文字的「复制全部」按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listOfNotNull(
                                state.updateTime?.takeIf { it.isNotBlank() },
                                "\u5171 ${state.newsList.size} \u6761"
                            ).joinToString(" \u00B7 "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.TextButton(
                            onClick = { copyToClipboard(context, allNewsText) },
                            enabled = allNewsText.isNotBlank()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy),
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "\u590D\u5236\u5168\u90E8",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }

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
                            painter = painterResource(R.drawable.ic_warning),
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
private fun News60sCard(
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
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

