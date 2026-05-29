package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class HotPlatform(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val brandColor: Color,
    val infoEmoji: String
) {
    WEIBO(
        key = "weibo",
        displayName = "微博",
        subtitle = "实时热度、滚动社交舆论关注点",
        brandColor = Color(0xFFDF2029),
        infoEmoji = "🔥"
    ),
    ZHIHU(
        key = "zhihu",
        displayName = "知乎",
        subtitle = "深度讨论、专业知识问答与深度探索",
        brandColor = Color(0xFF0084FF),
        infoEmoji = "💬"
    ),
    BAIDU(
        key = "baidu",
        displayName = "百度",
        subtitle = "热门检索、全网网民关注热词风向标",
        brandColor = Color(0xFF2B32B2),
        infoEmoji = "🔍"
    ),
    BILIBILI(
        key = "bilibili",
        displayName = "B站",
        subtitle = "热门视频、年轻人的文化潮流风向榜单",
        brandColor = Color(0xFFFB7299),
        infoEmoji = "📺"
    ),
    SSPAI(
        key = "sspai",
        displayName = "少数派",
        subtitle = "高效工作、前沿科技产品与生活硬件分享",
        brandColor = Color(0xFFDA5A47),
        infoEmoji = "💡"
    ),
    KR36(
        key = "36kr",
        displayName = "36氪",
        subtitle = "科技前沿、创投动态、新商业思想高地",
        brandColor = Color(0xFF1A56DB),
        infoEmoji = "⚡"
    ),
    CSDN(
        key = "csdn",
        displayName = "CSDN",
        subtitle = "技术分享、程序员高热度博文与前沿实战",
        brandColor = Color(0xFFFC5531),
        infoEmoji = "💻"
    ),
    HISTORY(
        key = "history",
        displayName = "历史今日",
        subtitle = "探寻古昔、感怀那些尘封在岁月中的今日事",
        brandColor = Color(0xFF795548),
        infoEmoji = "⏳"
    ),
    DOUYIN(
        key = "douyin",
        displayName = "抖音",
        subtitle = "美好生活、极速热点与短视频潮流动态",
        brandColor = Color(0xFF1C1F2B),
        infoEmoji = "🎵"
    );

    companion object {
        fun fromKey(key: String): HotPlatform? {
            return values().find { it.key.equals(key, ignoreCase = true) }
        }
    }
}
