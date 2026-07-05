# JibaiServerDoctor

> 全核心 Minecraft 服务器体检与故障诊断插件
> 作者：即白　邮箱：jibai0517@gamil.com

JibaiServerDoctor 是一个面向服主的**服务器体检插件**。它不是垃圾清理插件，也不是强制优化插件，而是帮你快速搞清楚：服务器为什么卡、哪个世界压力最高、哪些区块可能有问题、哪些插件报错频繁、内存是否过高、当前服务器是否健康，并生成一份**新手也能看懂的体检报告**和**可复制给 AI 分析的诊断包**。

**核心原则：只做体检、报告与提醒，绝不删除数据、破坏方块、清理实体或卸载区块。**

---

## 一、插件简介

| 项目 | 说明 |
| --- | --- |
| 插件名称 | JibaiServerDoctor |
| 版本 | 1.0.0 |
| 类型 | 服务器体检与故障诊断 |
| 开发语言 | Java 17 |
| 构建工具 | Gradle Kotlin DSL |
| 主指令 | `/doctor`（别名 `/jdoctor`、`/serverdoctor`） |

## 二、支持的服务端核心

基础兼容（全程仅使用 Bukkit/Spigot API，不使用 Paper 专属 API、不使用 NMS、反射或 CraftBukkit 内部类）：

- Bukkit
- Spigot
- Paper（优先测试，已在 Paper 1.21.8 实测通过）
- Purpur

目标版本：**Minecraft 1.20.x 及以上**。

暂不支持：Folia、Velocity、BungeeCord、Forge、Fabric、NeoForge、Bedrock。

## 三、亮点功能

- **服务器健康快照**：定时采集在线玩家、世界实体 / 掉落物 / 经验球 / 怪物 / 动物、加载区块、JVM 内存、近似 tick 延迟，并保留历史。
- **0-100 健康评分**：按内存、实体、掉落物、区块、红石、漏斗、报错、tick 延迟综合打分，映射为“优秀 / 正常 / 需要关注 / 存在风险 / 严重异常”五档。
- **异常原因排名**：每条风险都给出**原因 + 处理建议**，新手服主也能照着做。
- **Markdown 体检报告**：一条指令生成结构化报告，涵盖环境、内存、实体、区块、红石、漏斗、报错、评分、问题排名。
- **诊断包导出**：把报告、错误摘要、服务器环境信息、配置副本和 `ai-prompt.txt` 打包到一个文件夹，方便发给开发者或 AI。
- **区块 / 玩家检查**：定位实体密集、漏斗过多、红石高频的具体位置。
- **红石高频检测**：按时间窗口统计各区块红石事件，超阈值提醒（只记录不破坏）。
- **漏斗数量扫描**：定时扫描已加载区块的漏斗数量，超阈值提醒（只记录不破坏）。
- **日志错误分析**：读取 `logs/latest.log` 末尾若干行，统计并聚合报错，尝试识别相关插件。
- **管理员告警**：健康、内存、红石、漏斗异常时提醒在线管理员，带冷却防刷屏。
- **不依赖 Paper TPS API**：用轻量任务测量真实 tick 间隔，全核心通用。

## 四、安装方法

1. 确认服务器核心是 Bukkit / Spigot / Paper / Purpur，版本 1.20 以上。
2. 备份服务器。
3. 把 `JibaiServerDoctor-1.0.0.jar` 放进服务器的 `plugins/` 文件夹。
4. 启动服务器，控制台会显示带颜色的启动横幅。
5. 首次启动会在 `plugins/JibaiServerDoctor/` 下生成 `config.yml` 和 `messages.yml`。

## 五、指令一览

| 指令 | 说明 | 权限 |
| --- | --- | --- |
| `/doctor status` | 查看当前服务器健康状态 | `jibai.doctor.status` |
| `/doctor report` | 生成 Markdown 体检报告 | `jibai.doctor.report` |
| `/doctor export` | 导出诊断包 | `jibai.doctor.export` |
| `/doctor inspect chunk` | 检查你当前所在区块（需玩家执行） | `jibai.doctor.inspect` |
| `/doctor inspect player <玩家名>` | 检查指定玩家附近区域 | `jibai.doctor.inspect` |
| `/doctor risks` | 查看当前风险列表 | `jibai.doctor.risks` |
| `/doctor errors` | 查看最近日志错误摘要 | `jibai.doctor.errors` |
| `/doctor reload` | 重载配置文件 | `jibai.doctor.reload` |
| `/doctor help` | 查看指令帮助 | 无 |

所有子指令支持 Tab 补全；除 `inspect chunk` 需玩家执行外，其余控制台均可执行。

## 六、权限节点

| 权限 | 说明 | 默认 |
| --- | --- | --- |
| `jibai.doctor.admin` | 管理员总权限（包含以下全部） | op |
| `jibai.doctor.status` | 查看健康状态 | op |
| `jibai.doctor.report` | 生成报告 | op |
| `jibai.doctor.export` | 导出诊断包 | op |
| `jibai.doctor.inspect` | 使用检查功能 | op |
| `jibai.doctor.risks` | 查看风险列表 | op |
| `jibai.doctor.errors` | 查看错误摘要 | op |
| `jibai.doctor.reload` | 重载配置 | op |
| `jibai.doctor.notify` | 接收风险告警提醒 | op |

## 七、配置说明（config.yml）

主要配置项（完整注释见文件内）：

- `settings.enabled`：插件总开关。
- `snapshot.interval-seconds`：快照采集间隔（默认 60 秒，不低于 30）。
- `health-score.warning-score / danger-score`：健康评分告警阈值。
- `thresholds.*`：内存、实体、掉落物、区块、tick 延迟的警告 / 危险阈值。
- `redstone.*`：红石监控开关、时间窗口、单区块阈值、是否通知管理员。
- `hopper.*`：漏斗扫描开关、扫描间隔（默认 300 秒）、单区块阈值。
- `log-analyzer.*`：日志分析开关、最大读取行数（默认 5000）、关键字。
- `reports.* / exports.*`：报告与诊断包目录及内容开关。
- `alerts.*`：告警开关、冷却时间、接收权限。

提示语文件 `messages.yml` 中所有文本均可自定义，支持 `<green>` 颜色标签和 `&a` 颜色码混用。

## 八、报告说明

执行 `/doctor report` 后，报告生成于：

```
plugins/JibaiServerDoctor/reports/report-yyyy-MM-dd-HH-mm-ss.md
```

包含：服务器基础信息、健康评分、内存统计、实体与区块统计（含各世界明细）、红石风险、漏斗风险、插件报错统计、问题排名与处理建议。

## 九、诊断包说明

执行 `/doctor export` 后，诊断包生成于：

```
plugins/JibaiServerDoctor/exports/export-yyyy-MM-dd-HH-mm-ss/
```

包含：

- `report.md`：本次体检报告
- `error-summary.txt`：最近错误摘要（已脱敏）
- `server-info.txt`：服务器环境信息（核心、版本、Java、插件列表等）
- `config.yml`：插件配置副本
- `ai-prompt.txt`：可直接复制给 AI 的分析提示词

**安全约束**：不自动打包、不上传，不复制 `server.properties` 等敏感文件；写出前对 IP、密码、Token 等做脱敏处理。

## 十、从源码构建

```bash
./gradlew build
```

产物位于 `build/libs/JibaiServerDoctor-1.0.0.jar`。编译使用 Spigot API 以确保全核心兼容。

## 十一、测试方法

1. 将 jar 放入测试服 `plugins/`，启动服务器，确认横幅与加载成功。
2. 依次执行 `status`、`report`、`export`、`inspect chunk`、`inspect player <玩家名>`、`risks`、`errors`。
3. 修改配置后执行 `reload`。
4. 检查 `logs/latest.log` 无本插件报错。

## 十二、常见问题

**Q：为什么 tick 延迟显示不是精确的 TPS？**
A：为兼容 Bukkit/Spigot 全核心，本插件不依赖 Paper 的 TPS API，而是用轻量任务测量真实 tick 间隔得到近似延迟（理想 50ms）。

**Q：插件会自动清理实体或破坏红石 / 漏斗吗？**
A：不会。本插件只做体检、报告与提醒，任何清理和优化都需你自行决定。

**Q：`/doctor errors` 显示读不到日志？**
A：请确认服务器 `logs/latest.log` 存在，且 `config.yml` 中 `log-analyzer.enabled` 为 true。

---

作者：即白　邮箱：jibai0517@gamil.com
