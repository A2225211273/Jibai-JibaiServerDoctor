# 即白 Minecraft 插件展示库

这里用于展示和管理即白开发的 Minecraft 插件项目、Wiki 文档和发布说明。

> 作者：即白  
> 邮箱：jibai0517@gamil.com

## 插件列表

| 插件 | 类型 | 状态 | 目录 |
| --- | --- | --- | --- |
| JibaiServerDoctor | 服务器体检与诊断 | 新开发插件，待上线前复测 | [plugins/JibaiServerDoctor](plugins/JibaiServerDoctor) |

## 仓库结构

```text
plugins/
  JibaiServerDoctor/       # 服务器医生插件源码与文档
```

## JibaiServerDoctor 简介

JibaiServerDoctor（服务器医生）是一款面向 Bukkit / Spigot / Paper / Purpur 的 Minecraft 服务器体检与诊断插件。

它的目标不是直接“清理”，而是帮助服主看懂服务器为什么卡、哪些世界或区块压力高、实体/红石/漏斗/日志是否存在异常，并生成新手也能看懂的体检报告和可交给 AI 或开发者分析的诊断信息。

## 安全说明

本仓库不应上传服务器敏感信息，例如：

- 私服真实 IP、白名单、OP 列表
- 数据库密码、Token、API Key
- 测试服本地运行目录
- Gradle build 输出、本地缓存
