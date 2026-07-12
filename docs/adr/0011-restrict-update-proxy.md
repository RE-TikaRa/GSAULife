---
status: accepted
---

# 限制更新代理的认证范围

`gh.re-tikara.fun` 只为 GSAULife 与 GSAU-Card 的最新 Release 接口附加 `GITHUB_TOKEN`，其他 GitHub API 路径返回 404。成功的 Release 响应在 Cloudflare 边缘缓存五分钟，404 不缓存，避免首次发布被旧结果延迟。Raw 文件、GitHub 页面与 Release 资源继续使用不带认证的公开代理，下载请求保留 `Range` 头以支持断点续传。Worker 的名称、兼容日期与自定义域名保存在 `worker/wrangler.toml`，使线上部署能够从仓库复现。
