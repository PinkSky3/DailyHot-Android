package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R

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

    // 返回键：网页有上一页时先后退，没有时才关闭预览
    androidx.activity.compose.BackHandler {
        val webView = rootWebView
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            onClose()
        }
    }

    // 关闭预览时释放 WebView，避免后台继续播放视频或占用内存
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            rootWebView?.apply {
                stopLoading()
                destroy()
            }
        }
    }

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
                        painter = painterResource(R.drawable.ic_content_copy),
                        contentDescription = "\u590D\u5236\u94FE\u63A5",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = {
                    openInBrowser(context, url)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_language),
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

private fun openInBrowser(context: Context, url: String) {
    try {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "\u627E\u4E0D\u5230\u53EF\u4EE5\u6253\u5F00\u6D4F\u89C8\u5668\u7A0B\u5E8F\u7684\u5E94\u7528", Toast.LENGTH_SHORT).show()
    }
}
