# 甘农统一登录 链路调研结论

调研目标: 用 Python 把三种登录方式验通, 判定标准是"软件真正要的数据(成绩/课表/考试/排名)能取出来", 暂不改主程序。

## 底层机制

两个业务系统共用同一套 wisedu CAS: `authserver.gsau.edu.cn`。
- 教务 jwgl.gsau.edu.cn (成绩/课表/考试)
- 学工 xgfw.gsau.edu.cn (排名)

CAS 登录成功后拿到 `CASTGC`(TGC), 跨 service 免密。任何一种登录方式最后落地的都是同一个 CAS 会话。

### wengine 人机检测壳页

访问 jwgl / xgfw 时先撞 `web.gsau.edu.cn` 的人机检测壳页(lds-roller 动画 + packed JS), 返回 200 但不是目标页。壳页约 2750 字节。

放行三步(纯 requests 能过, 不用 WebView):
1. GET `/` 抓页面里 `id="uid"` 的值
2. GET `/wengine-auth/human-detect.js?token={uid}`
3. sleep 约 6 秒, 再 GET `/`

壳页 JS 本质就是 5 秒后 `location.href="/"`。

## 三种登录方式

### 1. 账密登录 — 纯 HTTP 可行

链路: 过 jwgl 人机检测 → GET authserver 登录页取 salt+execution → checkNeedCaptcha → 加密密码 → POST。

实测取到成绩 66 行真数据。

密码加密(encrypt.js 逆向):
- AES-CBC / Pkcs7
- key = 页面 `pwdEncryptSalt`(16 字节明文)
- iv = 随机 16 字符(不传服务端)
- 明文 = randomString(64) + 真实密码
- 字符集 `ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678`

POST 字段: username, password(加密), captcha, _eventId=submit, cllt=userNameLogin, dllt=generalLogin, lt(空), execution。

### 2. 企业微信扫码 — 纯 HTTP 可行

authserver 自带二维码, contextPath = `/authserver`:
- GET `/qrCode/getToken?ts=` (params 带 uuid) → 返回 uuid 串
- GET `/qrCode/getCode?uuid=` → 二维码图片
- GET `/qrCode/getStatus.htl?ts=` (params 带 uuid) → 轮询状态

状态实测跳变: `0`(等扫) → `2`(已扫待确认) → `1`(已确认)。`3` = 失效。

状态到 1 → 提交 qrLoginForm。

实测取到成绩 66 行, 与账密一致。

qrLoginForm 字段: lt(空), uuid, cllt=qrLogin, dllt=generalLogin, _eventId=submit, execution。

**关键坑**: submit 必须 POST 到带 service 的地址:
`/authserver/login?display=qrLogin&service=http%3A%2F%2Fjwgl.gsau.edu.cn%2F`
qrcode.js 开头就是在有 service 时给表单 action 拼这段。裸 POST `/authserver/login` 会让 ticket 停在 `jwgl/` 首页, jsxsd 子系统会话建不起来(成绩取不到); 带 service 后 final 落到 `jsxsd/framework/xsMain.jsp`, 会话才建成。

### 3. 微信扫码 — 纯 HTTP 可行

`/authserver/combinedLogin.do?type=weixin` 302 到标准微信 OAuth qrconnect, 参数(authserver 每次动态生成 state):
- appid = wx155886b664973e68
- redirect_uri = https://authserver.gsau.edu.cn/authserver/callback
- response_type = code, scope = snsapi_login
- state = 32位hex(回调校验)

出码/轮询链路:
1. GET qrconnect 页面抓 `uuid`
2. GET `open.weixin.qq.com/connect/qrcode/{uuid}` → 二维码图
3. 长轮询 GET `lp.open.weixin.qq.com/connect/l/qrconnect?uuid=` → 解 `wx_errcode`

errcode 实测跳变: `408`(未扫) → `404`(已扫待确认) → `405`(已确认, 同时回 `wx_code`)。`403`拒绝 `402`超时。

405 拿到 code → GET `/authserver/callback?code=&state=` 建 CAS 会话。

实测取到成绩 66 行。

**关键坑**: callback 后 final 落在 `personalInfo/personCenter/index.html`(用户中心), 只建了 CASTGC, 没回教务。要再 GET 一次带 service 的登录(`SVC`)换 jwgl 应用 ticket, final 才落 `jsxsd/framework/xsMain.jsp`, 成绩才出。原因: 微信 OAuth 回调走无 service 的 `/authserver/callback`, 只建 TGC。

## 验证码机制 — 动态风控

GET `/authserver/checkNeedCaptcha.htl?username=` 返回 `{"isNeed":true/false}`, 它决定这次登录要不要验证码。

**同账号有时弹有时不弹** = 动态风控(反复调试触发)。isNeed=true 时留空 captcha 会 POST 失败。

方案定了: isNeed=true 就拉 `/authserver/getCaptcha.htl` 的图, 弹给用户手输。扫码方式不吃验证码。

## 排名(学工 xgfw) — 必须 WebView

纯 requests 打排名接口 `getStuAcademicRankings.do` 恒 403(四种 header 打法全 403, 字节一致, 排除 header 问题; dump 出的是金智应用层 403 页非 wengine 壳页)。

根因: 金智 jbxxapp 是 SPA, 接口认前端 JS(bh.js)动态生成的 `_WEU` cookie。纯 requests 只 GET HTML 没跑 JS, `_WEU` 建不起来。

**必须隐藏 WebView 跑 SPA 初始化 JS 刷 `_WEU`**(因 CAS TGC 已建, 这步免密)。用户已认可隐藏 WebView。现有 LoginActivity.kt 第二跳 loadUrl(XGFW_URL) 就是走 WebView 建这个会话。

## 实现边界小结

| 方式 | 教务(成绩/课表/考试) | 学工(排名) |
|---|---|---|
| 账密 | 纯 HTTP | 需 WebView 刷 _WEU |
| 企业微信码 | 纯 HTTP | 需 WebView 刷 _WEU |
| 微信码 | 纯 HTTP | 需 WebView 刷 _WEU |

任一方式登录后拿到的都是同一 CAS 会话, 排名那步都得走 WebView。

## 调研脚本(桌面)

- `C:\Users\Tika\Desktop\gsau_e2e.py` — 账密端到端(过检测→账密→成绩; 同 session→xgfw→排名)
- `C:\Users\Tika\Desktop\gsau_qr_probe.py` — 企业微信扫码端到端(出码→轮询→submit带service→成绩)
- `C:\Users\Tika\Desktop\gsau_wx_probe.py` — 微信扫码端到端(combinedLogin→qrconnect→轮询→callback→补service→成绩)
- `C:\Users\Tika\Desktop\gsau_xgfw_probe.py` — 排名 403 四种打法探测
