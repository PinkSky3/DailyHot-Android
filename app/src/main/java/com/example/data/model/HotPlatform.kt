package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class PlatformCategory(val displayName: String) {
    ALL("全部"),
    SOCIAL("社会热点"),
    TECH("科技数码"),
    GAME("游戏动漫"),
    ENTERTAINMENT("娱乐"),
    FINANCE("财经商业"),
    SPORTS("体育"),
    NEWS("新闻资讯"),
    OTHER("其他")
}

enum class HotPlatform(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val brandColor: Color,
    val infoEmoji: String,
    val category: PlatformCategory
) {
    WEIBO(
        key = "weibo",
        displayName = "微博",
        subtitle = "实时热度、滚动社会舆事趋势与关注点",
        brandColor = Color(0xFFDF2029),
        infoEmoji = "🔥",
        category = PlatformCategory.SOCIAL
    ),
    ZHIHU(
        key = "zhihu",
        displayName = "知乎",
        subtitle = "深度讨论、专业知识分享与高热问答",
        brandColor = Color(0xFF0084FF),
        infoEmoji = "💬",
        category = PlatformCategory.SOCIAL
    ),
    BAIDU(
        key = "baidu",
        displayName = "百度",
        subtitle = "热门检索、全网网民关注热词风向标",
        brandColor = Color(0xFF2B32B2),
        infoEmoji = "🔍",
        category = PlatformCategory.SOCIAL
    ),
    BILIBILI(
        key = "bilibili",
        displayName = "B站",
        subtitle = "热门视频、年轻人的文化潮流风向榜单",
        brandColor = Color(0xFFFB7299),
        infoEmoji = "📺",
        category = PlatformCategory.ENTERTAINMENT
    ),
    TOUTIAO(
        key = "toutiao",
        displayName = "今日头条",
        subtitle = "头条高热文章、网民关注的大事要闻",
        brandColor = Color(0xFFE53935),
        infoEmoji = "🚩",
        category = PlatformCategory.NEWS
    ),
    TIEBA(
        key = "tieba",
        displayName = "贴吧",
        subtitle = "贴吧热议榜、网民兴趣聚集地与名梗出处",
        brandColor = Color(0xFF004BEE),
        infoEmoji = "🏷️",
        category = PlatformCategory.SOCIAL
    ),
    SSPAI(
        key = "sspai",
        displayName = "少数派",
        subtitle = "高效工作、前沿科技产品与数字生活分享",
        brandColor = Color(0xFFDA5A47),
        infoEmoji = "💡",
        category = PlatformCategory.TECH
    ),
    KR36(
        key = "36kr",
        displayName = "36氪",
        subtitle = "科技前沿、创投动态、新商业思想高地",
        brandColor = Color(0xFF1A56DB),
        infoEmoji = "⚡",
        category = PlatformCategory.FINANCE
    ),
    CSDN(
        key = "csdn",
        displayName = "CSDN",
        subtitle = "技术分享、程序员高热度博文与前沿实战",
        brandColor = Color(0xFFFC5531),
        infoEmoji = "💻",
        category = PlatformCategory.TECH
    ),
    DOUYIN(
        key = "douyin",
        displayName = "抖音",
        subtitle = "极速热点、多元创意短视频与社会潮流",
        brandColor = Color(0xFF1C1F2B),
        infoEmoji = "🎵",
        category = PlatformCategory.SOCIAL
    ),
    DOUBAN_GROUP(
        key = "douban-group",
        displayName = "豆瓣小组",
        subtitle = "精选小组讨论、兴趣趋势与高分话题推荐",
        brandColor = Color(0xFF00B51D),
        infoEmoji = "👥",
        category = PlatformCategory.SOCIAL
    ),
    DOUBAN_MOVIE(
        key = "douban-movie",
        displayName = "豆瓣电影",
        subtitle = "最新影讯、影评评分与热门华语影片动态",
        brandColor = Color(0xFF2E963D),
        infoEmoji = "🎬",
        category = PlatformCategory.ENTERTAINMENT
    ),
    HISTORY(
        key = "history",
        displayName = "历史今日",
        subtitle = "探寻古昔、感怀那些尘封在岁月中的今日事",
        brandColor = Color(0xFF795548),
        infoEmoji = "⏳",
        category = PlatformCategory.OTHER
    ),
    FIFTYONE_CTO(
        key = "51cto",
        displayName = "51CTO",
        subtitle = "IT技术学习交流平台、技术人经验分享",
        brandColor = Color(0xFF005DA6),
        infoEmoji = "👨",
        category = PlatformCategory.TECH
    ),
    ACFUN(
        key = "acfun",
        displayName = "AcFun",
        subtitle = "A站快弹幕、动漫、短视频与二次元社区",
        brandColor = Color(0xFFFD4C5B),
        infoEmoji = "🅰️",
        category = PlatformCategory.GAME
    ),
    COOLAPK(
        key = "coolapk",
        displayName = "酷安",
        subtitle = "玩机数码、科技好物与数码生活圈子讨论",
        brandColor = Color(0xFF11AA55),
        infoEmoji = "💚",
        category = PlatformCategory.TECH
    ),
    EARTHQUAKE(
        key = "earthquake",
        displayName = "地震速报",
        subtitle = "国家地震台网发布的国内外最新震情数据",
        brandColor = Color(0xFF8E24AA),
        infoEmoji = "⚠️",
        category = PlatformCategory.OTHER
    ),
    GENSHIN(
        key = "genshin",
        displayName = "原神Tap",
        subtitle = "原神TapTap社区、最新同人话题与活动",
        brandColor = Color(0xFF4A90E2),
        infoEmoji = "🌟",
        category = PlatformCategory.GAME
    ),
    HELLOGITHUB(
        key = "hellogithub",
        displayName = "HelloGitHub",
        subtitle = "分享 GitHub 上有趣、实用的开源项目推荐",
        brandColor = Color(0xFF24292E),
        infoEmoji = "🐱",
        category = PlatformCategory.TECH
    ),
    HONKAI(
        key = "honkai",
        displayName = "星铁Tap",
        subtitle = "星穹铁道TapTap社区、崩坏系列最新动态",
        brandColor = Color(0xFF3F51B5),
        infoEmoji = "🌌",
        category = PlatformCategory.GAME
    ),
    HUPU(
        key = "hupu",
        displayName = "虎扑",
        subtitle = "热血体育论坛、高热赛事点评与街区热榜",
        brandColor = Color(0xFFDF2029),
        infoEmoji = "🏀",
        category = PlatformCategory.SPORTS
    ),
    HUXIU(
        key = "huxiu",
        displayName = "虎嗅",
        subtitle = "洞察商业消费、科技财经的前沿深度报道",
        brandColor = Color(0xFFCE9E54),
        infoEmoji = "📦",
        category = PlatformCategory.FINANCE
    ),
    IFANR(
        key = "ifanr",
        displayName = "爱范儿",
        subtitle = "新酷消费电子、前沿数码与科技新潮风向",
        brandColor = Color(0xFFE53935),
        infoEmoji = "📱",
        category = PlatformCategory.FINANCE
    ),
    ITHOME(
        key = "ithome",
        displayName = "IT之家",
        subtitle = "科技数码前沿资讯、IT从业者聚集的社区",
        brandColor = Color(0xFFF21515),
        infoEmoji = "🏡",
        category = PlatformCategory.TECH
    ),
    ITHOME_XIJIAYI(
        key = "ithome-xijiayi",
        displayName = "IT之家限免",
        subtitle = "Steam及各大游戏平台限免与优惠游戏列表",
        brandColor = Color(0xFFD32F2F),
        infoEmoji = "🎮",
        category = PlatformCategory.TECH
    ),
    JIANSHU(
        key = "jianshu",
        displayName = "简书",
        subtitle = "多元创作社区、海量精选感悟随笔与小说",
        brandColor = Color(0xFFEA6F5A),
        infoEmoji = "📝",
        category = PlatformCategory.ENTERTAINMENT
    ),
    JUEJIN(
        key = "juejin",
        displayName = "掘金",
        subtitle = "掘力成长、程序员技术博客与技术前沿专栏",
        brandColor = Color(0xFF007FFF),
        infoEmoji = "🚀",
        category = PlatformCategory.TECH
    ),
    LOL(
        key = "lol",
        displayName = "LOL",
        subtitle = "掌上英雄联盟、游戏赛事速递动态与热议",
        brandColor = Color(0xFFC8AA6E),
        infoEmoji = "⚔️",
        category = PlatformCategory.GAME
    ),
    NETEASE_NEWS(
        key = "netease-news",
        displayName = "网易新闻",
        subtitle = "网易新闻头条、各板块深度社会报道与锐评",
        brandColor = Color(0xFFE60012),
        infoEmoji = "📰",
        category = PlatformCategory.NEWS
    ),
    NGABBS(
        key = "ngabbs",
        displayName = "NGA",
        subtitle = "游戏爆料与玩家讨论社区、硬核热点榜单",
        brandColor = Color(0xFF7F6B56),
        infoEmoji = "🛡️",
        category = PlatformCategory.GAME
    ),
    NODESEEK(
        key = "nodeseek",
        displayName = "NodeSeek",
        subtitle = "NodeSeek极客社区、VPS、主机与建站讨论",
        brandColor = Color(0xFFFF9800),
        infoEmoji = "🕵️",
        category = PlatformCategory.TECH
    ),
    QQ_NEWS(
        key = "qq-news",
        displayName = "腾讯新闻",
        subtitle = "腾讯网热点头条、权威深度快讯与精选评论",
        brandColor = Color(0xFF1E88E5),
        infoEmoji = "🐧",
        category = PlatformCategory.NEWS
    ),
    SINA_NEWS(
        key = "sina-news",
        displayName = "新浪新闻",
        subtitle = "新浪新闻首页头条、全天候热点资讯速递",
        brandColor = Color(0xFFE53935),
        infoEmoji = "📡",
        category = PlatformCategory.NEWS
    ),
    SINA(
        key = "sina",
        displayName = "新浪网",
        subtitle = "新浪网焦点聚焦、国内要闻舆事与国际社会",
        brandColor = Color(0xFFFFB300),
        infoEmoji = "🌐",
        category = PlatformCategory.NEWS
    ),
    STARRAIL_MIYOUSHE(
        key = "starrail",
        displayName = "星穹米游社",
        subtitle = "崩坏星穹铁道米游社、社区优质攻略与活动",
        brandColor = Color(0xFF673AB7),
        infoEmoji = "🌠",
        category = PlatformCategory.GAME
    ),
    THEPAPER(
        key = "thepaper",
        displayName = "澎湃新闻",
        subtitle = "思想与温度、专注重大思想报道和时政评论",
        brandColor = Color(0xFF212121),
        infoEmoji = "🌊",
        category = PlatformCategory.NEWS
    ),
    V2EX(
        key = "v2ex",
        displayName = "V2EX",
        subtitle = "创意工作者社区、分享极客有趣新奇观点",
        brandColor = Color(0xFF333333),
        infoEmoji = "📣",
        category = PlatformCategory.TECH
    ),
    WEATHERALARM(
        key = "weatheralarm",
        displayName = "气象预警",
        subtitle = "国家气象局实时灾害、台风及极端天气预警",
        brandColor = Color(0xFF00ACC1),
        infoEmoji = "🌪️",
        category = PlatformCategory.OTHER
    ),
    WEREAD(
        key = "weread",
        displayName = "微信读书",
        subtitle = "热播图书推荐榜、精选用户高分评语与书单",
        brandColor = Color(0xFF43A047),
        infoEmoji = "📚",
        category = PlatformCategory.ENTERTAINMENT
    ),
    ZHIHU_DAILY(
        key = "zhihu-daily",
        displayName = "知乎日报",
        subtitle = "每日知乎精选、专业而有趣的日常深度问答",
        brandColor = Color(0xFF0066FF),
        infoEmoji = "📰",
        category = PlatformCategory.NEWS
    );

    companion object {
        fun fromKey(key: String): HotPlatform? {
            return values().find { it.key.equals(key, ignoreCase = true) }
        }

        fun platformsByCategory(category: PlatformCategory): List<HotPlatform> {
            return if (category == PlatformCategory.ALL) values().toList()
            else values().filter { it.category == category }
        }

        val categoryPlatformCounts: Map<PlatformCategory, Int> by lazy {
            PlatformCategory.values().associateWith { cat ->
                if (cat == PlatformCategory.ALL) values().size
                else values().count { it.category == cat }
            }
        }
    }
}
