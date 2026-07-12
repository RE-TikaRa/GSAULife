# 甘农生活数据处理说明

- 文档版本：1.0
- 生效日期：2026 年 7 月 13 日
- 最后更新：2026 年 7 月 13 日
- 适用应用：甘农生活（`com.tika.gsaulife`）

## 1. 文档目的

本说明从技术角度描述甘农生活的数据来源、处理流程、存储结构、网络边界和删除行为。隐私权利和联系信息见[隐私政策](Privacy-Policy.md)。

当前实现的核心原则是：

> 应用仅在用户主动使用相关功能时，向甘肃农业大学官方系统发送完成请求所需的数据。校园卡信息、学校会话、教务数据和本地设置不会发送到开发者控制的服务。手动检查更新会访问 Cloudflare Worker 与 GitHub，但不附带这些校园数据。相关数据保存在设备本地，并由用户管理和删除。

## 2. 数据边界

应用包含三个主要数据边界：

```text
Android 应用私有存储
├── 校园卡字段与付款码缓存
├── 教务和学工会话
├── 成绩、课表、考试、排名缓存
├── 教学周、外观和刷新设置
└── Android WebView Cookie 存储

甘肃农业大学官方服务
├── 校园卡系统
├── 教务系统
├── 学工系统
└── 统一认证页面

公开更新服务
├── gh.re-tikara.fun
└── GitHub Releases
```

应用没有开发者用户数据库、同步账号、遥测平台、广告 SDK、崩溃上报 SDK、Cloudflare KV 或远程配置服务。

## 3. 校园卡处理流程

### 3.1 添加校园卡

```text
用户复制校园卡付款页面链接
              │
              ▼
       应用解析查询参数
        openid + id
              │
              ▼
HTTPS 请求 yktapp.gsau.edu.cn
              │
              ▼
学校页面返回付款字符串、姓名、卡号、余额
              │
              ▼
保存必要字段到 gsaulife_card
```

应用不会保存用户粘贴的完整原始链接文本。解析成功后，本地账户记录保存 `openid`、卡片编号、学校返回的姓名、卡号、余额、本机备注、付款字符串和获取时间。

### 3.2 二维码生成

```text
本地付款字符串
      │
      ▼
ZXing 在内存中生成 Bitmap
      │
      ├── 应用付款码首页
      ├── 全屏付款页
      └── Android 桌面组件
```

- 二维码图像不保存为相册或应用文件。
- 二维码图像不会上传到开发者服务、GitHub 或 Cloudflare。
- 付款字符串会作为校园卡本地缓存保存，以便应用和组件短时展示。
- 当前付款码展示有效期为 60 秒；超过时间后界面和组件会隐藏旧码。
- 应用在刷新、服务结束、设备启动、删除校园卡或清除应用数据时，可能替换或清除本地付款码缓存。

### 3.3 多卡与备注

多张校园卡保存在同一 SharedPreferences 文件的 JSON 数组中。切换当前卡只修改本地索引，不通知开发者服务。备注名只存在于本机，不发送给学校系统。

## 4. 教务和学工处理流程

### 4.1 登录

```text
用户选择教务或学工功能
          │
          ▼
Android WebView 打开学校 HTTPS 页面
          │
          ▼
用户在学校页面完成认证
          │
          ▼
学校设置 Cookie
          │
          ├── WebView Cookie 存储
          └── academic_school_sessions
```

登录表单由学校页面提供。应用不读取或保存用户输入的学校密码；应用在目标学校页面登录成功后，读取学校为对应域名设置的 Cookie，用于后续原生数据查询。

教务和学工使用独立会话。设置页的“重新登录”会先清除 WebView Cookie，再打开学校登录页面。

### 4.2 教务查询

```text
academic_school_sessions 中的教务 Cookie
                    │
                    ▼
       HTTPS 请求 jwgl.gsau.edu.cn
                    │
                    ▼
        HTML 页面结构校验与解析
                    │
       ┌────────────┼────────────┐
       ▼            ▼            ▼
     成绩          课表         考试安排
       │            │            │
       └────────────┴────────────┘
                    │
                    ▼
       academic_cache 本地缓存
```

应用会校验教务页面的数据表和当前学期标记。登录页、维护页或结构不符合预期的页面不会作为合法空数据覆盖已有缓存。

### 4.3 学工查询

```text
academic_school_sessions 中的学工 Cookie
                    │
                    ▼
       HTTPS 请求 xgfw.gsau.edu.cn
                    │
                    ▼
        JSON 成功码与字段范围校验
                    │
                    ▼
       专业排名与班级排名缓存
```

学工响应缺少关键字段、排名超出总人数或业务状态失败时，应用不会把异常响应写入缓存。

### 4.4 离线查看

成功查询的数据带有获取时间，并保存在 `academic_cache`。当网络失败或学校会话过期时，应用可以显示已有缓存及其时间标记。

离线缓存只反映上次成功查询结果。学校后续修改的数据不会自动出现在旧缓存中。

## 5. 教学周处理

教学周设置保存在 `academic_settings`：

```text
用户选择开学日期
        或
输入某日对应的当前周
        │
        ▼
换算为与时区无关的日期序号
        │
        ▼
根据设备当前日期计算教学周
```

日期序号和教学周只在本地使用，不发送给学校或开发者。

## 6. 更新检查流程

```text
用户点击“检查更新”
          │
          ▼
GET gh.re-tikara.fun/api/repos/RE-TikaRa/GSAULife/releases/latest
          │
          ▼
Cloudflare Worker 请求 GitHub API
          │
          ▼
返回公开的版本号与发布页地址
          │
          ▼
应用在本地比较版本号
```

应用不会在更新请求中加入校园卡字段、Cookie、成绩、课表、排名、主题、通知状态或设备标识符。当前应用版本号也只用于本地比较，不作为查询参数发送。

Cloudflare 和 GitHub 仍会接触网络连接自然产生的 IP 地址、时间、TLS 和 HTTP 元数据。Worker 只允许预先列出的公开 Release API 路径，不提供通用的带认证 GitHub API 代理。

## 7. 本地存储结构

当前应用代码使用 Android SharedPreferences 保存教务缓存，并使用 WebView Cookie 存储保留学校网页会话；应用没有建立 SQLite 数据库保存成绩、课表、考试或排名缓存。

| 存储名称 | 内容 | 格式 |
|---|---|---|
| `gsaulife_card` | 校园卡字段、当前卡索引、付款码缓存 | SharedPreferences + JSON |
| `gsaulife_card_refresh` | 按需刷新或持续刷新模式 | SharedPreferences |
| `academic_school_sessions` | 教务 Cookie、学工 Cookie、会话版本 | SharedPreferences + JSON |
| `academic_cache` | 成绩、成绩明细、课表、考试、排名及获取时间 | SharedPreferences + JSON |
| `academic_settings` | 开学日期序号与教学周校准 | SharedPreferences + JSON |
| `appearance` | 跟随系统、浅色或深色 | SharedPreferences |
| `legal_agreement` | 已同意的法律文档版本 | SharedPreferences |
| Android WebView Cookie | 学校登录页面设置的 Cookie | WebView 管理的应用私有存储 |

SharedPreferences 由 Android 应用沙箱隔离，但当前版本没有对这些文件增加独立的应用层加密。设备 Root、系统漏洞、恶意调试工具或能够访问应用私有目录的程序可能读取相关内容。

## 8. 内存中的临时数据

以下数据会在功能运行期间进入内存：

- 网络请求和响应内容。
- HTML、JSON 和解析后的模型。
- 二维码 Bitmap。
- 当前页面列表、课程网格和弹窗内容。
- GitHub Release 响应。

页面关闭或进程结束后，相关对象由 Android 与运行时回收。应用不会把内存内容转存到开发者服务器。

## 9. 删除行为

| 用户操作 | 删除内容 |
|---|---|
| 删除单张校园卡 | 对应校园卡字段、备注和付款码缓存 |
| 退出教务身份 | 教务 Cookie、学工 Cookie、WebView Cookie、成绩、成绩明细、课表、考试、排名和教学周设置 |
| 清除应用数据 | SharedPreferences、WebView 数据和应用私有目录中的全部内容 |
| 卸载应用 | Android 管理的应用私有数据 |
| 取消通知或后台权限 | 不删除业务数据，只停止对应系统能力 |

应用没有远程账户，因此没有服务器端“注销”和远程删除接口。开发者也无法访问设备上的数据并代为删除。

## 10. 备份行为

应用通过清单和 `data_extraction_rules.xml` 禁用 Android 云备份与设备迁移，并排除标准备份机制支持的全部数据域。应用代码也没有实现自有云同步。

第三方 Root 备份、厂商私有迁移、整机镜像或调试工具不属于应用能够控制的范围。

## 11. 数据共享与销售

应用不会出售、出租或交换用户数据，不会把校园卡或教务数据分享给广告商、数据经纪人、分析平台或开发者社区。

学校系统、GitHub、Cloudflare 和网络基础设施仅在对应请求发生时处理其所需数据或网络元数据，具体边界见[第三方服务说明](Third-Party.md)。

## 12. 变更记录

数据格式和处理流程随源代码公开。发生实质变化时，本说明会更新版本和日期；历史内容可通过 Git 提交记录查看。

相关文件：[隐私政策](Privacy-Policy.md) · [服务协议](Terms-of-Service.md) · [第三方服务说明](Third-Party.md) · [安全说明](Security.md)
