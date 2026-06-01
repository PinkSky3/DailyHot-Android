# 聚合智讯 Android

聚合智讯是一款面向日常信息浏览的 Android 客户端，集成 60 秒新闻、多平台热搜、实时油价、实时金价和 AI 问答。

## 功能

### 60 秒新闻

- 默认首页展示每日 60 秒新闻
- 支持一键刷新
- 可作为 AI 问答的实时上下文

### 多平台热搜

- 聚合微博、知乎、百度、B 站、抖音、贴吧、NGA、虎扑、ifanr 等 40+ 平台
- 支持按娱乐、社会、科技、体育、财经、教育、健康、国际、游戏等分类筛选
- 支持关键词搜索、内置 WebView 预览、复制链接和分享

### 实时油价

- 查询全国 31 个省、自治区、直辖市油价
- 支持 92#、95#、98# 汽油和 0# 柴油
- 数据来自 Pear API

### 实时金价

- 展示国内贵金属行情和国际市场行情
- 展示品牌金店报价
- 展示银行金条和黄金回收价格
- 支持主数据源失败后的备用接口降级

### AI 问答

- 底部悬浮入口，弹出式聊天面板
- 支持多个免费模型并显示模型可用状态
- 自动带入当前新闻、热搜、油价、金价上下文
- 模型列表接口失败时会回退到默认模型，不阻断提问

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel + StateFlow
- Retrofit + OkHttp + Moshi
- Coil
- GitHub Actions

## 版本

当前版本：`1.1.0`

## 本地构建

在项目根目录创建 `.env` 文件：

```bash
AI_API_KEY=sk-xxx
```

构建 Debug APK：

```bash
./gradlew assembleDebug
```

APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions 配置

在 GitHub 仓库中打开 `Settings -> Secrets and variables -> Actions`，添加：

| Name | Value |
| --- | --- |
| `PEAR_AI_API_KEY` | `sk-xxx` |

## 数据源

- 热搜聚合：`dailyhotapi.3yu3.top`
- 油价、60 秒新闻、AI 问答：`api.pearapi.ai`
- 金价：`tmini.net`、`api.freejk.com`、`v2.xxapi.cn`
