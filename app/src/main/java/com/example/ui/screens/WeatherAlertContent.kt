package com.example.ui.screens

import java.util.Locale
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.model.WeatherAlertItem
import com.example.data.model.WeatherAlertResponse
import com.example.data.model.WeatherForecastDay
import com.example.ui.viewmodel.WeatherAlertSnapshot
import com.example.ui.viewmodel.WeatherAlertUiState

@Composable
fun WeatherAlertContent(
    state: WeatherAlertUiState,
    onRefresh: () -> Unit,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alertColor = Color(0xFF00ACC1)
    var selectedProvince by rememberSaveable { mutableStateOf("") }
    when (state) {
        is WeatherAlertUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = alertColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在按当前 IP 定位并查询气象预警...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
        is WeatherAlertUiState.Success -> {
            WeatherAlertSuccessContent(
                snapshot = state.snapshot,
                fetchedTime = state.fetchedTime,
                fromCache = state.fromCache,
                alertColor = alertColor,
                onRefresh = onRefresh,
                selectedProvince = selectedProvince,
                onQuery = { province ->
                    selectedProvince = province
                    onQuery(province)
                },
                modifier = modifier
            )
        }
        is WeatherAlertUiState.Error -> {
            val fallback = state.lastSuccess
            if (fallback != null) {
                WeatherAlertSuccessContent(
                    snapshot = fallback,
                    fetchedTime = "上次成功数据",
                    fromCache = true,
                    alertColor = alertColor,
                    onRefresh = onRefresh,
                    selectedProvince = selectedProvince,
                    onQuery = { province ->
                        selectedProvince = province
                        onQuery(province)
                    },
                    modifier = modifier
                )
            } else {
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
                            tint = alertColor,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        WeatherRefreshButton(alertColor = alertColor, onRefresh = onRefresh)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherAlertSuccessContent(
    snapshot: WeatherAlertSnapshot,
    fetchedTime: String,
    fromCache: Boolean,
    alertColor: Color,
    onRefresh: () -> Unit,
    selectedProvince: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val weather = snapshot.primary
    val regionalWeather = snapshot.regional
    val alerts = snapshot.allDisplayAlerts()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            WeatherSummaryCard(
                weather = weather,
                fetchedTime = fetchedTime,
                fromCache = fromCache,
                alertCount = alerts.size,
                regionalCount = regionalWeather.size,
                alertColor = alertColor,
                onRefresh = onRefresh
            )
        }

        item {
            WeatherQueryCard(
                selectedProvince = selectedProvince,
                onQuery = onQuery,
                alertColor = alertColor
            )
        }

        if (alerts.isEmpty()) {
            item { NoWeatherAlertCard(alertColor = alertColor) }
        } else {
            itemsIndexed(alerts, key = { index, alert -> "${alert.title.orEmpty()}-${alert.location}-$index" }) { _, alert ->
                WeatherAlertCard(alert = alert, alertColor = alertColor)
            }
        }

        if (regionalWeather.isNotEmpty()) {
            item {
                WeatherInfoCard(
                    title = "自定义查询结果",
                    body = "已补充查询 ${regionalWeather.joinToString("、") { it.locationLabel() }}。可在上方下拉框选择省份或城市补查，避免 IP 定位点过窄漏掉省内预警。",
                    footnote = null,
                    alertColor = alertColor
                )
            }
        }

        val minutely = weather.minutelyForecast ?: weather.minutelyPrecip
        if (!minutely?.summary.isNullOrBlank()) {
            item {
                WeatherInfoCard(
                    title = "分钟级降水",
                    body = minutely?.summary.orEmpty(),
                    footnote = minutely?.updateTime,
                    alertColor = alertColor
                )
            }
        }

        val riskyForecast = weather.forecast.orEmpty().filter { day ->
            val text = listOfNotNull(day.weatherDay, day.weatherNight).joinToString(" ")
            val precip = day.precip ?: 0.0
            precip >= 10.0 || RISKY_WEATHER_KEYWORDS.any { text.contains(it) }
        }.take(4)
        if (riskyForecast.isNotEmpty()) {
            item {
                Text(
                    text = "天气风险趋势",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }
            itemsIndexed(riskyForecast, key = { index, day -> day.date ?: index.toString() }) { _, day ->
                WeatherForecastRiskCard(day = day, alertColor = alertColor)
            }
        }
    }
}

@Composable
private fun WeatherSummaryCard(
    weather: WeatherAlertResponse,
    fetchedTime: String,
    fromCache: Boolean,
    alertCount: Int,
    regionalCount: Int,
    alertColor: Color,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location_on),
                        contentDescription = null,
                        tint = alertColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = weather.locationLabel(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "IP 自动定位${if (regionalCount > 0) " · 自定义查询 $regionalCount 条" else ""} · ${if (fromCache) "缓存" else "刷新"} $fetchedTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新预警",
                        tint = alertColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WeatherMetricChip("天气", weather.weather.orEmpty().ifBlank { "未知" }, alertColor, Modifier.weight(1f))
                WeatherMetricChip("气温", weather.temperature.formatTemp(), alertColor, Modifier.weight(1f))
                WeatherMetricChip("预警", if (alertCount > 0) "${alertCount} 条" else "暂无", alertColor, Modifier.weight(1f))
            }
            if (!weather.reportTime.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "气象数据：${weather.reportTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun WeatherQueryCard(
    selectedProvince: String,
    onQuery: (String) -> Unit,
    alertColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "按省份或城市补查",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = alertColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = selectedProvince.ifBlank { "选择省份 / 城市" },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    WEATHER_PROVINCES.forEach { province ->
                        DropdownMenuItem(
                            text = { Text(province) },
                            onClick = {
                                expanded = false
                                onQuery(province)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "默认只按 IP 查一次；需要看省级预警时从下拉框选择，避免免费接口被批量限流。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
    }
}

private val WEATHER_PROVINCES = listOf(
    "北京", "天津", "上海", "重庆",
    "河北", "山西", "辽宁", "吉林", "黑龙江", "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南", "广东", "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海", "台湾",
    "内蒙古", "广西", "西藏", "宁夏", "新疆", "香港", "澳门"
)

@Composable
private fun WeatherMetricChip(
    label: String,
    value: String,
    alertColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(alertColor.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NoWeatherAlertCard(alertColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = alertColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "暂无有效气象预警",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "当前定位地区没有正在生效的官方气象预警。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun WeatherAlertCard(alert: DisplayWeatherAlert, alertColor: Color) {
    val levelColor = weatherLevelColor(alert.level, alertColor)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(levelColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = alert.title.ifBlank { "气象预警" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeatherPill(text = alert.location.ifBlank { "当前位置" }, color = levelColor)
                WeatherPill(text = alert.type.ifBlank { "预警" }, color = levelColor)
                WeatherPill(text = alert.level.ifBlank { "级别未知" }, color = levelColor)
            }
            val meta = listOf(alert.publisher, alert.publishTime).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (alert.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                SelectionContainer {
                    Text(
                        text = alert.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
            val guidance = alert.guidance.filter { it.isNotBlank() }
            if (guidance.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "防御指引",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = levelColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                guidance.take(6).forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherPill(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun WeatherInfoCard(
    title: String,
    body: String,
    footnote: String?,
    alertColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = alertColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!footnote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = footnote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun WeatherForecastRiskCard(day: WeatherForecastDay, alertColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(day.date, day.week).joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOfNotNull(day.weatherDay, day.weatherNight).joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${day.tempMin.formatTemp()} / ${day.tempMax.formatTemp()}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = alertColor
                )
                Text(
                    text = "降水 ${day.precip?.let { "${it.trimNumber()}mm" } ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                )
            }
        }
    }
}

@Composable
private fun WeatherRefreshButton(alertColor: Color, onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onRefresh() },
        color = alertColor,
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
                text = "重新加载",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

private val RISKY_WEATHER_KEYWORDS = listOf("暴雨", "大雨", "雷阵雨", "雷雨", "台风", "大风", "暴雪", "冰雹")

private data class DisplayWeatherAlert(
    val location: String,
    val title: String,
    val type: String,
    val level: String,
    val text: String,
    val publishTime: String,
    val publisher: String,
    val guidance: List<String>
)

private fun WeatherAlertSnapshot.allDisplayAlerts(): List<DisplayWeatherAlert> {
    return (listOf(primary) + regional)
        .flatMap { weather ->
            weather.alerts.orEmpty()
                .filter { !it.title.isNullOrBlank() || !it.text.isNullOrBlank() }
                .map { alert -> alert.toDisplayAlert(weather.locationLabel()) }
        }
        .distinctBy { listOf(it.location, it.title, it.publishTime, it.text).joinToString("|") }
}

private fun WeatherAlertItem.toDisplayAlert(location: String): DisplayWeatherAlert {
    return DisplayWeatherAlert(
        location = location,
        title = title.orEmpty(),
        type = type.orEmpty(),
        level = level.orEmpty(),
        text = text.orEmpty(),
        publishTime = publishTime.orEmpty(),
        publisher = publisher.orEmpty(),
        guidance = guidance.orEmpty()
    )
}

private fun WeatherAlertResponse.locationLabel(): String {
    return listOfNotNull(province, city, district)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" · ")
        .ifBlank { "当前位置" }
}

private fun Double?.formatTemp(): String {
    return this?.let { "${it.trimNumber()}°C" } ?: "--"
}

private fun Double.trimNumber(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)
}

private fun weatherLevelColor(level: String?, fallback: Color): Color {
    return when {
        level?.contains("红") == true -> Color(0xFFE53935)
        level?.contains("橙") == true -> Color(0xFFFF8F00)
        level?.contains("黄") == true -> Color(0xFFFBC02D)
        level?.contains("蓝") == true -> Color(0xFF1E88E5)
        else -> fallback
    }
}

