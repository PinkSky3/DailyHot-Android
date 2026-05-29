# 聚合热搜 + 实时油价

多平台热搜聚合与实时油价查询的 Android 客户端。

## 功能

### 🔥 热搜
- 多平台热搜数据聚合（微博、知乎、百度、B站、NGA、抖音、贴吧等 40+ 平台）
- 自动二级分类（娱乐、社会、科技、体育、财经、教育、健康、国际、游戏）
- 关键词搜索过滤
- 内置 WebView 预览 / 复制链接 / 分享

### ⛽ 油价
- 全国 31 个省/自治区/直辖市实时油价
- 支持 92#、95#、98# 汽油及多种柴油型号
- 双 API 自动切换（主 API 故障时启用备用）

## 技术栈

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Retrofit** + **Moshi** 网络请求
- **Coil** 图片加载
- **ViewModel** + **StateFlow** 状态管理
- **GitHub Actions** CI/CD 自动构建

## 构建

```bash
./gradlew assembleDebug
```

APK 输出至 `app/build/outputs/apk/debug/app-debug.apk`
