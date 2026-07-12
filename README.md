# 甘农生活

甘农生活是面向甘肃农业大学学生的 Android 校园生活应用，将校园卡付款、教务信息和应用设置集中在一个入口中。

## 功能

- 默认进入校园卡付款码页面，支持多张校园卡、余额、备注、重新绑定与删除
- 付款码每 30 秒刷新、60 秒失效，并提供全屏付款页与桌面付款组件
- 教务四宫格包含成绩查询、我的课表、考试安排与学业排名
- 教务系统与学工系统分别认证，教务缓存支持离线查看
- 设置页提供跟随系统、浅色、深色三种外观，以及按需刷新与持续刷新
- 手动检查 GitHub Releases 更新

校园卡通过付款码页面复制出的链接完成绑定，与教务身份彼此独立。

## 工程

```text
app               应用入口、导航、主题、设置与更新
feature-card      校园卡、付款码、桌面组件与后台刷新
feature-academic  成绩、课表、考试、排名与学校会话
```

工程使用 Android API 37、Java 17、Android Gradle Plugin 9.2.1 和 Gradle 9.6.1，最低支持 Android 7.0（API 24）。

## 构建

```bash
./gradlew test lint :app:assembleDebug :app:assembleRelease --console=plain
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。未配置签名环境变量时，Release 构建生成未签名 APK。

## 发布

稳定版标签使用 `v主版本.次版本.修订版本` 格式。Release 工作流需要以下 GitHub Secrets：

```text
KEYSTORE_BASE64
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

工作流会执行全部测试、Lint、Debug 与签名 Release 构建，验证 APK 签名后发布 `GSAULife-v*.apk`。

## 数据

校园卡凭据、学校会话和教务缓存保存在应用私有存储中。应用关闭系统备份，不读取 GSAU-Card 或 GSAU-Academic-Hub 的数据。

## 许可证

[Apache License 2.0](LICENSE)
