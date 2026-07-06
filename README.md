# JibaiServerDoctor

面向 Bukkit / Spigot / Paper / Purpur 的 Minecraft 服务器体检与故障诊断插件。

> 作者：即白  
> 邮箱：jibai0517@gamil.com  
> 版本：1.0.0  
> 在线 Wiki：[[https://jibai-server-doctor-wiki.vercel.app](https://jibai-server-doctor-wiki.vercel.app](http://www.jibai.shop/))

## 项目介绍

**JibaiServerDoctor（服务器医生）** 是一个专门帮助服主诊断服务器问题的插件。

很多服务器卡顿时，服主会先安装清理插件或优化插件，但如果不知道问题来源，往往很难真正解决。JibaiServerDoctor 的定位是：**先诊断，再处理**。

它会像一次服务器体检一样，帮助你判断：

- 服务器为什么卡；
- 哪个世界压力最高；
- 哪些区块可能存在风险；
- 实体、掉落物、红石、漏斗是否异常；
- 日志里是否存在频繁报错；
- 当前服务器整体健康程度如何；
- 出问题时应该把哪些信息发给开发者或 AI 分析。

JibaiServerDoctor 不会自动破坏方块、删除实体或修改玩家数据。它的核心价值是生成清晰的体检结果、风险列表、Markdown 报告和可交给 AI 分析的诊断包。

## 核心功能

### 全核心兼容

插件基于 Bukkit / Spigot 通用 API 编写，不依赖 Paper 专属接口、不使用 NMS、不使用反射。

支持：

- Bukkit
- Spigot
- Paper
- Purpur

目标版本：**Minecraft 1.20.x 及以上**。

### 健康评分

插件会根据服务器运行状态生成 0-100 的健康评分。

| 分数 | 等级 | 含义 |
| --- | --- | --- |
| 90-100 | 优秀 | 状态很好 |
| 70-89 | 正常 | 整体稳定 |
| 50-69 | 需要关注 | 已出现风险苗头 |
| 30-49 | 存在风险 | 建议尽快排查 |
| 0-29 | 严重异常 | 需要立即处理 |

### 风险分析

插件会分析以下风险来源：

- JVM 内存占用；
- 世界实体总量；
- 掉落物堆积；
- 加载区块数量；
- 红石高频触发；
- 漏斗密集区块；
- 日志 ERROR / WARN / Exception；
- tick 延迟趋势。

每条风险都会尽量附带原因和处理建议，方便新手服主理解问题。

### 一键生成体检报告

使用 `/doctor report` 可以生成 Markdown 体检报告，内容包含服务器环境、内存状态、实体与区块统计、红石与漏斗风险、日志错误摘要、健康评分、风险排名和优化建议。

### 导出诊断包

使用 `/doctor export` 可以导出诊断包，包含体检报告、错误摘要、环境信息和 `ai-prompt.txt`。

`ai-prompt.txt` 可以直接复制给 AI 或开发者，用于进一步分析服务器问题。

### 区块与玩家定位

- `/doctor inspect chunk`：检查玩家当前所在区块的实体、掉落物、漏斗、红石情况。
- `/doctor inspect player <玩家名>`：检查指定玩家附近区域是否存在异常。

### 日志错误分析

插件会读取 `logs/latest.log` 最近若干行，统计 ERROR、WARN、Exception，并聚合同类报错，帮助服主判断是否有插件频繁报错。

### 安全优先

JibaiServerDoctor 的定位是诊断，不是自动清理。

默认行为：

- 不删除实体；
- 不破坏方块；
- 不卸载区块；
- 不修改玩家数据；
- 不上传服务器文件；
- 导出内容会对 IP、密码、Token 等敏感信息进行脱敏处理。

## 支持与不支持

| 类型 | 支持情况 |
| --- | --- |
| Paper | 支持，优先测试 |
| Purpur | 支持 |
| Spigot | 支持 |
| Bukkit | 支持 |
| Folia | 暂不支持 |
| Velocity / BungeeCord | 暂不支持 |
| Forge / Fabric | 暂不支持 |
| 基岩版 | 暂不支持 |

## 快速开始

1. 构建或获取 `JibaiServerDoctor-1.0.0.jar`。
2. 将 jar 放入服务器的 `plugins/` 目录。
3. 重启服务器。
4. 执行 `/doctor status` 查看当前健康状态。
5. 执行 `/doctor report` 生成体检报告。
6. 出现复杂问题时，执行 `/doctor export` 导出诊断包。

## 指令

主指令：`/doctor`

别名：

- `/jdoctor`
- `/serverdoctor`

| 指令 | 说明 |
| --- | --- |
| `/doctor status` | 查看服务器当前健康状态 |
| `/doctor report` | 生成 Markdown 体检报告 |
| `/doctor export` | 导出诊断包和 AI 提示词 |
| `/doctor inspect chunk` | 检查当前区块 |
| `/doctor inspect player <玩家名>` | 检查指定玩家附近区域 |
| `/doctor risks` | 查看当前风险列表 |
| `/doctor errors` | 查看最近日志错误摘要 |
| `/doctor reload` | 重载配置文件 |
| `/doctor help` | 查看帮助 |

## 权限

| 权限 | 说明 |
| --- | --- |
| `jibai.doctor.admin` | 管理员总权限 |
| `jibai.doctor.status` | 查看健康状态 |
| `jibai.doctor.report` | 生成报告 |
| `jibai.doctor.export` | 导出诊断包 |
| `jibai.doctor.inspect` | 使用检查功能 |
| `jibai.doctor.risks` | 查看风险列表 |
| `jibai.doctor.errors` | 查看错误摘要 |
| `jibai.doctor.reload` | 重载配置 |
| `jibai.doctor.notify` | 接收风险告警 |

默认全部为 OP 权限。

## 项目结构

```text
plugins/
  JibaiServerDoctor/
    src/main/java/
    src/main/resources/
    README.md
    WIKI.md
```

## 文档

- 插件源码与说明：[plugins/JibaiServerDoctor](plugins/JibaiServerDoctor)
- Wiki 文档：[plugins/JibaiServerDoctor/WIKI.md](plugins/JibaiServerDoctor/WIKI.md)
- 在线 Wiki：[https://jibai-server-doctor-wiki.vercel.app](https://jibai-server-doctor-wiki.vercel.app)

## 免责声明

JibaiServerDoctor 是诊断插件，不承诺自动修复所有卡顿问题。服务器性能可能受到硬件、核心版本、插件冲突、地图规模、玩家行为、红石机器和 Java 参数等多种因素影响。

插件提供的是体检、风险提示、报告和诊断信息，具体处理动作仍建议由服主或专业维护人员确认后执行。

## 作者

JibaiServerDoctor 由 **即白** 开发。

- 作者：即白
- 邮箱：jibai0517@gamil.com
