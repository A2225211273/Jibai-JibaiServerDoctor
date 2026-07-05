---
layout: home

hero:
  name: JibaiOptimizer
  text: Minecraft 服务器优化与性能监控插件
  tagline: 面向 Bukkit / Spigot / Paper / Purpur 的全核心优化工具，帮助服主观察服务器状态、降低资源浪费，并在压力过高时执行低风险优化。
  actions:
    - theme: brand
      text: 开始安装
      link: /guide/install
    - theme: alt
      text: 查看指令
      link: /guide/commands

features:
  - title: 全核心兼容
    details: 基于 Bukkit / Spigot 通用 API，支持 Bukkit、Spigot、Paper、Purpur，不绑定单一核心。
  - title: 综合性能监控
    details: 监控 TPS、近似 MSPT、内存、实体、区块、掉落物、经验球、红石和漏斗等关键指标。
  - title: 低风险优化
    details: 默认不强制 GC、不破坏漏斗、不强制卸载区块、不删除玩家重要实体，危险动作需要显式开启。
---

# JibaiOptimizer 是什么？

JibaiOptimizer 是由 **即白** 开发的 Minecraft 服务器优化与性能监控插件。它不是单纯的“清垃圾插件”，而是一个帮助服主观察服务器状态、发现异常来源、降低资源浪费的综合优化工具。

当服务器压力过高时，插件可以自动执行低风险优化，例如清理旧掉落物、清理超量经验球、轻量合并掉落物，并向管理员发送提醒。

## 基础信息

| 项目 | 内容 |
| --- | --- |
| 作者 | 即白 |
| 邮箱 | jibai0517@gamil.com |
| 支持核心 | Bukkit / Spigot / Paper / Purpur |
| Minecraft | 1.20+ |
| Java | 17+ |
| 不支持 | Folia / Forge / Fabric / BungeeCord / Velocity |

## 使用前说明

任何插件都不能保证服务器永远不卡。卡顿可能来自硬件不足、插件冲突、红石机器、实体异常、地图区块、Java 参数等多种原因。

JibaiOptimizer 的定位是降低可控资源浪费、及时发现异常、自动处理部分低风险问题。它适合作为服务器优化体系的一部分，而不是所有性能问题的唯一答案。
