# 甘农生活第三方服务说明

- 文档版本：1.0
- 生效日期：2026 年 7 月 13 日
- 最后更新：2026 年 7 月 13 日
- 适用应用：甘农生活（`com.tika.gsaulife`）

## 1. 说明

甘农生活通过学校服务完成校园卡和教务功能，并使用 Android 平台、GitHub、Cloudflare 与若干开源组件。第三方可能按照自己的隐私政策、服务条款和许可证处理请求或提供代码。

本文件区分“网络服务”和“软件依赖”。软件依赖运行在应用进程内，不表示对应项目会收到甘农生活的用户数据。

## 2. 网络服务

| 服务 | 域名或入口 | 用途 | 应用发送的数据 |
|---|---|---|---|
| 甘肃农业大学校园卡系统 | `yktapp.gsau.edu.cn` | 获取付款字符串、姓名、卡号和余额 | `openid`、卡片编号和常规 HTTPS 请求信息 |
| 甘肃农业大学教务系统 | `jwgl.gsau.edu.cn` | 查询成绩、课表和考试安排 | 教务 Cookie、查询参数和常规 HTTPS 请求信息 |
| 甘肃农业大学学工系统 | `xgfw.gsau.edu.cn` | 查询学业排名 | 学工 Cookie、查询参数和常规 HTTPS 请求信息 |
| 学校统一认证 | 由学校页面跳转的认证服务 | 用户登录教务和学工系统 | 用户在学校 WebView 页面输入的信息 |
| Cloudflare Worker | `gh.re-tikara.fun` | 代理公开 Release API 和发布页 | 更新检查请求；不附带校园卡和教务数据 |
| GitHub API / Releases | `api.github.com`、`github.com` | 获取版本号、更新说明和 APK 发布页 | 由 Worker 转发的公开 Release 请求 |
| Android 系统服务 | Android、WebView、桌面组件、通知、前台服务 | 运行应用与系统功能 | 由 Android 按系统机制处理 |

学校服务接收功能所需的校园数据。Cloudflare 与 GitHub 不接收应用本地保存的校园卡字段、Cookie、成绩、课表、考试或排名；但会处理网络连接自然产生的 IP 地址、时间和 HTTP/TLS 元数据。

## 3. 第三方政策

- 甘肃农业大学服务：适用学校发布的账号、网络、校园卡、教务和信息系统规定。
- GitHub：[GitHub General Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement)
- Cloudflare：[Cloudflare Privacy Policy](https://www.cloudflare.com/privacypolicy/)
- Google / Android：[Google Privacy Policy](https://policies.google.com/privacy)

学校页面可能加载其选择的脚本、样式、认证资源或其他服务。此类内容由学校页面控制，项目无法完整列举其运行时依赖。

## 4. 核心开源依赖

以下版本来自当前 `gradle/libs.versions.toml` 和模块构建文件。

| 组件 | 当前版本 | 用途 | 许可证 |
|---|---:|---|---|
| Kotlin | 由 Android Gradle Plugin 工具链提供 | 应用开发语言与运行时 | Apache License 2.0 |
| Kotlin Coroutines Android | 1.11.0 | 异步请求、生命周期任务与后台刷新 | Apache License 2.0 |
| AndroidX Activity | 1.13.0 | Activity 基础能力 | Apache License 2.0 |
| AndroidX AppCompat | 1.7.1 | Android 兼容界面 | Apache License 2.0 |
| AndroidX Core | 1.19.0 | Android 核心扩展 | Apache License 2.0 |
| AndroidX Fragment | 1.8.9 | 页面模块与生命周期 | Apache License 2.0 |
| AndroidX ConstraintLayout | 2.2.1 | 主界面布局 | Apache License 2.0 |
| AndroidX GridLayout | 1.1.0 | 教务网格布局 | Apache License 2.0 |
| AndroidX Lifecycle | 2.11.0 | 应用和页面生命周期 | Apache License 2.0 |
| AndroidX RecyclerView | 1.4.0 | 校园卡和教务列表 | Apache License 2.0 |
| Material Components for Android | 1.14.0 | Material 界面、弹窗、日期选择器和 Snackbar | Apache License 2.0 |
| OkHttp | 5.4.0 | HTTPS 网络请求 | Apache License 2.0 |
| jsoup | 1.22.2 | 教务 HTML 解析 | MIT License |
| ZXing Core | 3.5.4 | 本地二维码生成 | Apache License 2.0 |

AndroidX 各模块还会引入 SavedState、Annotation、Collection、Profile Installer 等传递依赖。其具体版本以 Gradle 解析结果和构建产物为准。

## 5. 开源项目链接

- Kotlin：[github.com/JetBrains/kotlin](https://github.com/JetBrains/kotlin)
- Kotlin Coroutines：[github.com/Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- AndroidX：[source.android.com/docs/setup/about/licenses](https://source.android.com/docs/setup/about/licenses)
- Material Components：[github.com/material-components/material-components-android](https://github.com/material-components/material-components-android)
- OkHttp：[github.com/square/okhttp](https://github.com/square/okhttp)
- jsoup：[jsoup.org](https://jsoup.org/)
- ZXing：[github.com/zxing/zxing](https://github.com/zxing/zxing)

## 6. 数据共享边界

使用开源库不等于向其作者发送数据。当前依赖中没有广告、统计、远程配置或崩溃上报 SDK。

- ZXing 在设备内存中生成二维码，不进行网络通信。
- jsoup 在本地解析应用已经取得的 HTML，不进行网络通信。
- AndroidX、Material Components 和 Coroutines 提供本地运行能力。
- OkHttp 仅执行应用代码发起的网络请求；请求地址和重定向行为由对应客户端配置与服务器响应共同决定。

## 7. 变更

依赖升级、增加网络服务或更改更新渠道时，本文件应同步更新。完整依赖树可以通过 Gradle 的 `dependencies` 任务查看。

相关文件：[隐私政策](Privacy-Policy.md) · [数据处理说明](Data-Policy.md) · [开源软件声明](Open-Source.md) · [安全说明](Security.md)
