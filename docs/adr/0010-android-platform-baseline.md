---
status: accepted
---

# 保持 Android 7.0 兼容并面向 API 37

GSAULife 保持两个旧项目的最低版本 API 24，以覆盖原有可安装设备；新工程使用 API 37.0 SDK 编译，并将目标版本设为 API 37。该目标版本使 Android 15 及以上的 `dataSync` 前台服务时限成为必须实现和验证的应用行为。构建使用 Java 17、Android Gradle Plugin 9.2.1、内置 Kotlin 与 Gradle 9.6.1，这组稳定版本支持 API 37.0。
