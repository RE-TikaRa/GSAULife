---
status: accepted
---

# 建立甘农生活的独立发布关系

甘农生活发布到新的公开仓库 `RE-TikaRa/GSAULife`。`v*` 标签触发签名 APK 构建，产物命名为 `GSAULife-v*.apk`；应用通过现有 `gh.re-tikara.fun` 代理查询该仓库的 GitHub Releases。更新检查只在设置页由用户手动发起，发现新版本后打开对应的 Release 页面。稳定版标签必须递增，Android 版本码按 `主版本 × 1000000 + 次版本 × 1000 + 修订版本` 生成。甘农生活从首个版本起固定使用独立的发布签名密钥，密钥文件保存在仓库外，CI 通过 GitHub Secrets 使用。新仓库使产品名称、源代码、安装包与更新来源保持一致，也与 `com.tika.gsaulife` 的全新应用身份相对应。
