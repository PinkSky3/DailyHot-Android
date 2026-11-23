package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppUpdate

/**
 * 更新提示：先看更新内容，再从三种下载方式中选一种。
 * 国内直链放在第一位并标为推荐；GitHub 原站不经过加速；蓝奏云作为最后的备用。
 */
@Composable
internal fun UpdateAvailableDialog(
    update: AppUpdate,
    isResolvingMirror: Boolean,
    onDismiss: () -> Unit,
    onIgnore: () -> Unit,
    onMirrorDownload: () -> Unit,
    onGithubDownload: () -> Unit,
    onBackupDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "发现新版本 ${update.version}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = update.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = update.notes.ifBlank { "新版本已通过正式发布流程。" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "选择下载方式",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onMirrorDownload,
                    enabled = !isResolvingMirror,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isResolvingMirror) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在连接国内线路…")
                    } else {
                        Text("国内直链下载（推荐）")
                    }
                }

                OutlinedButton(
                    onClick = onGithubDownload,
                    enabled = !isResolvingMirror,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GitHub 原站下载")
                }

                TextButton(
                    onClick = onBackupDownload,
                    enabled = !isResolvingMirror,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("蓝奏云备用（自动复制密码 52pj）")
                }

                Text(
                    text = "国内直链通过 gh-proxy 加速下载 GitHub 上的同一个安装包；GitHub 原站需要网络能访问 GitHub。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        },
        dismissButton = {
            TextButton(onClick = onIgnore) {
                Text("忽略此版本")
            }
        }
    )
}
