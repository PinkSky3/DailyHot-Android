package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModelHealth
import com.example.ui.viewmodel.AiChatState
import com.example.ui.viewmodel.AiChatViewModel

@Composable
fun AiChatFab(
    viewModel: AiChatViewModel,
    modifier: Modifier = Modifier
) {
    val dialogVisible by viewModel.dialogVisible.collectAsState()

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0xFF2196F3))
            .clickable { viewModel.showDialog() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "AI\u95EE\u7B54",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }

    if (dialogVisible) {
        AiChatSheet(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSheet(viewModel: AiChatViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val messages by viewModel.messages.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val modelHealthList by viewModel.modelHealthList.collectAsState()
    val aiContext by viewModel.aiContext.collectAsState()

    val scrollState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scrollState.animateScrollToItem(messages.size - 1)
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.hideDialog() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            // Header: title + model dropdown + close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI\u95EE\u7B54",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModelDropdown(
                        modelHealthList = modelHealthList,
                        selectedModelId = selectedModel,
                        onSelect = { viewModel.selectModel(it) }
                    )
                    IconButton(onClick = { viewModel.hideDialog() }) {
                        Icon(Icons.Default.Close, contentDescription = "\u5173\u95ED")
                    }
                }
            }

            // Context summary
            val contextSummary = buildContextSummary(aiContext)
            if (contextSummary.isNotBlank()) {
                Text(
                    text = contextSummary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Messages
            if (messages.isEmpty() && chatState !is AiChatState.Loading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFF2196F3).copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\u5411 AI \u63D0\u95EE\u5427\uFF0C\u6211\u4F1A\u7ED3\u5408\u5F53\u524D\u6570\u636E\u56DE\u7B54\u4F60",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(message = msg)
                    }
                    if (chatState is AiChatState.Loading) {
                        item {
                            ChatBubble(
                                message = com.example.ui.viewmodel.ChatUiMessage(
                                    role = "assistant",
                                    content = "..."
                                ),
                                isLoading = true
                            )
                        }
                    }
                    if (chatState is AiChatState.Error) {
                        item {
                            Text(
                                text = (chatState as AiChatState.Error).message,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Input
            var input by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("\u8F93\u5165\u95EE\u9898...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(input.trim())
                                input = ""
                            }
                        }
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendMessage(input.trim())
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "\u53D1\u9001",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelDropdown(
    modelHealthList: List<ModelHealth>,
    selectedModelId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = modelHealthList.find { it.id == selectedModelId }

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2196F3).copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(isOnline = current?.isOnline ?: false)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = current?.displayName ?: selectedModelId,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2196F3)
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modelHealthList.forEach { model ->
                DropdownMenuItem(
                    onClick = {
                        onSelect(model.id)
                        expanded = false
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(isOnline = model.isOnline)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = model.displayName,
                                style = if (model.isOnline) MaterialTheme.typography.bodyMedium
                                    else MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusDot(isOnline: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336))
    )
}

@Composable
fun ChatBubble(
    message: com.example.ui.viewmodel.ChatUiMessage,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) Color(0xFF2196F3) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            if (isLoading) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\u601D\u8003\u4E2D...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

private fun buildContextSummary(ctx: com.example.ui.viewmodel.AiContext): String {
    val parts = mutableListOf<String>()
    if (ctx.hotItems.isNotEmpty()) parts.add("\u70ED\u641C${ctx.hotItems.size}\u6761")
    if (ctx.news60s.isNotEmpty()) parts.add("60S${ctx.news60s.size}\u6761")
    if (ctx.oilEntries.isNotEmpty()) parts.add("\u6CB9\u4EF7\u6570\u636E")
    if (ctx.goldMarkets.isNotEmpty()) parts.add("\u91D1\u4EF7\u6570\u636E")
    return if (parts.isNotEmpty()) "\u5DF2\u52A0\u8F7D: ${parts.joinToString(" / ")}" else ""
}
