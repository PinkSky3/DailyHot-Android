# 综合资讯 · DailyHot-Android

**综合资讯 (DailyHot-Android)** 是一款个人日常信息聚合 Android 客户端，聚合全网多平台热搜榜单、60 秒快新闻、气象预警与实时油价/金价数据，一站式满足日常信息获取需求。

## 功能特性

### 60 秒新闻

- 默认首页展示每日 **60 秒新闻** 图文速览
- 支持一键刷新，随时获取最新资讯
- 支持长图保存与分享
- 可切换热搜、气象预警、油价和金价页面

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
- 数据来源：60s API，内置 NXVAV、QQSUU、XXAPI 公开接口兜底（均为大陆可直连）
- 自动定位省份，切换即查

### 实时金价

- 展示 **国内黄金价格** 与 **国际市场行情**
- 展示 **品牌金店报价**（周大福、老凤祥等）
- 展示 **银行积存金** 与 **黄金现货** 价格
- 内置备用接口，主数据源失效时自动切换，保障可用性

## 技术栈

- **语言**：[Kotlin](https://kotlinlang.org/)
- **UI 框架**：[Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
- **架构**：ViewModel + StateFlow
- **网络**：Retrofit + OkHttp + Moshi
- **持续集成**：GitHub Actions

## 版本

当前版本：`1.3.2`

具体变化见 [更新日志](CHANGELOG.md)。

## 构建指南

构建 Debug APK：

```bash
./gradlew assembleDebug
```

APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 数据来源

- 热搜聚合：`dailyhotapi.3yu3.top`
- 60 秒新闻：`60s.viki.moe` 及公开备用实例
- 油价：`60s.viki.moe`、`api.nxvav.cn`、`60s.crystelf.top`、`api.qqsuu.cn`、`v2.xxapi.cn`（按顺序兜底）
- 金价：`60s.viki.moe`（主）、`push2.eastmoney.com`（兜底）、`v2.xxapi.cn`（品牌金店/银行金条/回收价）

---

*个人项目，仅用于学习和日常使用。*
