# 即白 Minecraft 插件展示库

这里用于展示和管理即白开发的 Minecraft 插件项目、Wiki 文档、发布说明和部署资料。

> 作者：即白  
> 邮箱：jibai0517@gamil.com

## 插件列表

| 插件 | 类型 | 状态 | 目录 | 在线 Wiki |
| --- | --- | --- | --- | --- |
| JibaiOptimizer | 服务器优化与性能监控 | 已部署 Wiki，插件待上线前复测 | [plugins/JibaiOptimizer](plugins/JibaiOptimizer) | https://jibaioptimizer-wiki.vercel.app |

## 仓库结构

```text
plugins/
  JibaiOptimizer/          # 插件 Gradle 源码项目
wiki/
  JibaiOptimizer-Wiki/     # VitePress 网页 Wiki 项目
```

## 安全说明

本仓库不应上传服务器敏感信息，例如：

- 私服真实白名单、OP 列表、IP 白名单
- 数据库密码、Token、API Key
- Vercel `.env.local`
- 本地构建缓存、`node_modules`、Gradle build 输出

## JibaiOptimizer 简介

JibaiOptimizer 是一款面向 Bukkit / Spigot / Paper / Purpur 的 Minecraft 服务器优化与性能监控插件。它用于监控 TPS、近似 MSPT、内存、实体、区块、掉落物、经验球、红石和漏斗等性能相关因素，并在服务器压力过高时执行低风险优化。

在线 Wiki：

https://jibaioptimizer-wiki.vercel.app
