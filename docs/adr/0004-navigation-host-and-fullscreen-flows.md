---
status: accepted
---

# 主导航与全屏流程分离

GSAULife 使用一个主 Activity 和 Fragment 导航承载教务、付款码、设置及教务功能页，使底栏与返回行为由同一导航状态管理。全屏付款和统一认证继续使用独立 Activity，因为它们分别需要沉浸付款体验与独立 WebView 登录流程；这一结构避免在多个普通页面重复维护底栏。
