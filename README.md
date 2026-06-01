# 综合资讯 · DailyHot-Android

**综合资讯 (DailyHot-Android)** 是一款个人日常信息聚合 Android 客户端，聚合全网多平台热搜榜单、60 秒快新闻、实时油价/金价数据，并集成 AI 问答功能，一站式满足日常信息获取需求。

## 功能特性

### 60 秒新闻

- 默认首页展示每日 **60 秒新闻** 图文速览
- 支持一键刷新，随时获取最新资讯
- 支持长图保存与分享
- 可切换为 AI 问答或热搜榜单作为默认首页

### 多平台热搜聚合

- 聚合 **40+ 平台** 热搜榜单，覆盖主流中文社区：
  - **社交媒体**：微博、知乎、百度、贴吧、豆瓣、小红书
  - **视频平台**：B 站、抖音、快手
  - **科技媒体**：36氪、少数派、ifanr、CSDN、GitHub
  - **游戏/社区**：NGA、游民星空、TapTap、V2EX
  - **综合资讯**：今日头条、澎湃新闻、网易新闻、新浪新闻
- 支持按 **分类筛选**：全站、社会、科技、财经、娱乐、体育、游戏、音乐等
- 支持 **关键词搜索** 过滤榜单
- 内置 WebView 预加载，点击即可快速预览原文
- 支持 **复制链接** 和 **系统分享** 到其他应用

### 实时油价

- 查询 **全国 31 个省/自治区/直辖市** 最新油价
- 支持 **92#、95#、98# 汽油** 和 **0# 柴油**
- 数据来源：Pear API（每日更新）
- 自动定位省份，切换即查

### 实时金价

- 展示 **国内黄金价格** 与 **国际市场行情**
- 展示 **品牌金店报价**（周大福、老凤祥等）
- 展示 **银行积存金** 与 **黄金现货** 价格
- 内置备用接口，主数据源失效时自动切换，保障可用性

### AI 问答

- 内置对话界面，支持流式流式对话
- 支持 **多模型切换**，实时显示模型状态
- 自动注入当前新闻、热搜、油价金价作为上下文，让 AI 回答更具时效性
- 模型列表接口不可用时自动回退默认模型，保证基础可用

## 技术栈

- **语言**：[Kotlin](https://kotlinlang.org/)
- **UI 框架**：[Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
- **架构**：ViewModel + StateFlow
- **网络**：Retrofit + OkHttp + Moshi
- **图片加载**：[Coil](https://coil-kt.github.io/coil/)
- **持续集成**：GitHub Actions

## 版本

当前版本：`1.1.0`

## 构建指南

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

在仓库 `Settings -> Secrets and variables -> Actions` 中添加：

| Name | Value |
| --- | --- |
| `PEAR_AI_API_KEY` | `sk-xxx` |

## 数据来源

- 热搜聚合：`dailyhotapi.3yu3.top`
- 油价 / 60 秒新闻 / AI 问答：`api.pearapi.ai`
- 金价：`tmini.net`、`api.freejk.com`、`v2.xxapi.cn`

---

*个人项目，仅用于学习和日常使用。*