---
status: accepted
---

# 使用 HTTPS 访问教务系统

`jwgl.gsau.edu.cn` 的登录页与教务接口已经支持有效的 HTTPS 连接，GSAULife 统一通过 HTTPS 传输教务会话和查询请求，并关闭应用的明文网络访问。这样可以避免教务 Cookie 与查询数据在传输过程中以明文出现，也不再需要为教务域名单独开放明文流量。
