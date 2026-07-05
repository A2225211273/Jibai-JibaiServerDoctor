# JibaiOptimizer

一个兼容 **Bukkit / Spigot / Paper / Purpur** 的 Minecraft 服务器优化与性能监控插件。

作者：即白 · 邮箱：jibai0517@gamil.com · 版本：1.0.0

> **重要说明**：本插件**无法保证服务器永远不卡**。卡顿的根本原因可能是硬件不足、插件冲突、超大红石机器、实体过多、Java 参数不合理等，这些不是任何插件能单独解决的。JibaiOptimizer 的定位是**降低资源占用、及时发现异常、自动处理低风险问题、提供清晰的性能数据**，帮你把风险降下来、把问题看清楚。
>
> 所有**危险功能默认关闭**：不强制 GC、不破坏红石、不破坏漏斗、不强制卸载区块、不删除玩家重要实体和物品。破坏类动作（如红石 break）需要你显式开启多重开关才会生效，详见下文。

---

## 一、插件简介

JibaiOptimizer 通过监控服务器运行状态、减少无意义资源消耗、限制高频卡服行为、自动清理低价值对象、向管理员发出性能告警，帮助服务器尽量保持流畅。

它是一个**综合优化插件**，而不是单纯的“清垃圾插件”。主要能力：

- TPS / MSPT 近似监控（由调度器自行采样计算，全核心通用，不依赖 Paper API）
- JVM 内存占用监控与告警
- 实体数量统计与控制
- 区块加载压力监控
- 掉落物、经验球轻量清理
- 红石高频检测
- 漏斗密集检测
- 卡顿时限制自然刷怪
- 压力过高时自动执行低风险优化
- 管理员指令查看服务器状态

> ⚠️ 任何插件都无法保证服务器永远不卡。卡顿可能来自硬件、插件冲突、红石机器、实体过多、Java 参数不合理等。本插件的目标是**降低资源占用、及时发现异常、自动处理低风险问题、并提供清晰的性能数据**。

---

## 二、支持版本

| 项目 | 说明 |
|------|------|
| 服务端 | **Bukkit / Spigot / Paper / Purpur**（不支持 Folia、BungeeCord、Velocity、Forge、Fabric） |
| 优先测试 | Paper 1.20.x+ / 1.21.x+ |
| Minecraft 版本 | 1.20.x ~ 1.21.x+ |
| Java 运行环境 | Java 17 及以上 |
| 描述文件 | `plugin.yml`（不使用 paper-plugin.yml） |

插件使用 **Spigot 1.20.1 API** 编译，全程仅使用 Bukkit/Spigot 通用 API，**不使用任何 Paper 专属 API、NMS、反射或 CraftBukkit 内部类**，因此可在 Bukkit / Spigot / Paper / Purpur 上通用运行。

> **关于 TPS / MSPT**：Bukkit / Spigot 没有稳定通用的 TPS API（`getTPS()` / `getAverageTickTime()` 是 Paper/Spigot 扩展），为兼容全核心，本插件用调度器每 tick 采样、根据 tick 间隔计算**近似 TPS 与近似每 tick 耗时**。正常约 20 TPS / 50ms，数值仅供参考，不追求与核心内部值完全一致。

---

## 三、安装方法

1. 从 `build/libs/` 获取 `JibaiOptimizer-1.0.0.jar`（或自行打包，见下文）。
2. 将 jar 放入服务器（Bukkit / Spigot / Paper / Purpur）的 `plugins/` 文件夹。
3. 启动或重启服务器。
4. 首次启动会在 `plugins/JibaiOptimizer/` 下生成 `config.yml` 与 `messages.yml`。
5. 控制台出现 `JibaiOptimizer 已启用` 即表示加载成功。

---

## 四、指令说明

主指令：`/optimizer`，别名：`/joptimizer`、`/jopt`。支持 Tab 补全，控制台可执行全部指令。

| 指令 | 说明 | 权限 |
|------|------|------|
| `/optimizer status` | 查看服务器性能状态（TPS、MSPT、内存、实体、区块等） | `jibai.optimizer.status` |
| `/optimizer memory` | 查看内存占用详情 | `jibai.optimizer.memory` |
| `/optimizer entities` | 查看各世界实体统计 | `jibai.optimizer.entities` |
| `/optimizer chunks` | 查看各世界加载区块统计 | `jibai.optimizer.chunks` |
| `/optimizer clean` | 手动执行一次低风险优化 | `jibai.optimizer.clean` |
| `/optimizer redstone` | 查看红石高频统计 | `jibai.optimizer.status` |
| `/optimizer hoppers` | 扫描并查看漏斗密集区块 | `jibai.optimizer.status` |
| `/optimizer reload` | 重载配置与消息，并重启定时任务 | `jibai.optimizer.reload` |

无参数执行 `/optimizer` 会显示指令帮助。

---

## 五、权限说明

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `jibai.optimizer.admin` | 管理员总权限（包含以下全部子权限） | op |
| `jibai.optimizer.status` | 查看状态 / 红石 / 漏斗统计 | op |
| `jibai.optimizer.memory` | 查看内存 | op |
| `jibai.optimizer.entities` | 查看实体统计 | op |
| `jibai.optimizer.chunks` | 查看区块统计 | op |
| `jibai.optimizer.clean` | 手动执行优化 | op |
| `jibai.optimizer.reload` | 重载配置 | op |
| `jibai.optimizer.notify` | 接收性能告警 | op |

拥有 `jibai.optimizer.notify` 或 `jibai.optimizer.admin` 的在线玩家会收到内存、区块、红石、漏斗等告警。

---

## 六、config.yml 配置说明

修改配置后使用 `/optimizer reload` 生效。所有配置项均带默认值，误删也不会导致插件报错。

### settings —— 总开关
| 键 | 默认 | 说明 |
|----|------|------|
| `settings.enabled` | true | 插件总开关，关闭后所有监控与优化任务停止 |
| `settings.language` | zh_CN | 语言标识 |

### monitor —— 性能采集
| 键 | 默认 | 说明 |
|----|------|------|
| `monitor.interval-seconds` | 10 | 轻量采集间隔（秒），只读内存与近似 TPS/MSPT，不宜过小 |
| `monitor.full-scan-interval-seconds` | 60 | 实体/区块全量扫描间隔（秒），比轻量采集降频，避免频繁全量遍历 |
| `monitor.max-worlds-per-tick` | 1 | 全量扫描时单 tick 最多遍历的世界数量（分批，避免单 tick 卡顿） |
| `monitor.max-chunks-per-tick` | 3000 | 全量扫描时单 tick 最多处理的区块数量（预留上限） |
| `monitor.history-size` | 60 | 历史快照保留数量（预留） |

### auto-optimizer —— 自动优化
当 TPS 过低、MSPT 过高或内存过高时，自动执行配置允许的优化动作，**带冷却时间避免频繁执行**。
| 键 | 默认 | 说明 |
|----|------|------|
| `auto-optimizer.enabled` | true | 自动优化开关 |
| `auto-optimizer.cooldown-seconds` | 120 | 两次自动优化的最小间隔 |
| `auto-optimizer.trigger.tps-below` | 18.5 | TPS 低于此值触发 |
| `auto-optimizer.trigger.mspt-above` | 45 | MSPT 高于此值触发 |
| `auto-optimizer.trigger.memory-percent-above` | 85 | 内存百分比高于此值触发 |
| `auto-optimizer.actions.cleanup-dropped-items` | true | 清理旧掉落物 |
| `auto-optimizer.actions.cleanup-exp-orbs` | true | 清理超量经验球 |
| `auto-optimizer.actions.merge-nearby-items` | true | 合并附近相同掉落物 |
| `auto-optimizer.actions.limit-spawn` | true | 提示已限制刷怪 |
| `auto-optimizer.actions.notify-admins` | true | 通知管理员 |

### memory —— 内存监控
| 键 | 默认 | 说明 |
|----|------|------|
| `memory.warning-percent` | 85 | 内存告警阈值 |
| `memory.critical-percent` | 92 | 内存严重告警阈值 |
| `memory.force-gc.enabled` | **false** | 强制 GC（默认关闭，可能造成卡顿） |
| `memory.force-gc.threshold-percent` | 92 | 触发 GC 的内存阈值 |
| `memory.force-gc.cooldown-seconds` | 600 | GC 冷却时间 |

### entity-control —— 实体控制

> ⚠️ **说明（重要）：** `entity-control` 目前只提供**实体分类统计**（供 `/optimizer entities` 查看），
> 以及供其它模块调用的**受保护实体判定**。下方 `max-*-per-chunk` 阈值目前为
> **仅统计 / 预留项**，插件**不会**据此主动清理或删除实体。真正生效的实体控制是
> `spawn-limiter`（限制自然刷怪）与 `exp-orb-optimizer`（清理超量经验球）。
> 本插件默认**不清理**动物、村民、盔甲架、物品展示框、驯服动物和有名字的实体。

| 键 | 默认 | 说明 |
|----|------|------|
| `entity-control.enabled` | true | 实体统计开关 |
| `entity-control.max-entities-per-chunk` | 80 | 单区块实体上限（**仅统计/预留**，不主动清理） |
| `entity-control.max-monsters-per-chunk` | 40 | 单区块怪物上限（**仅统计/预留**） |
| `entity-control.max-animals-per-chunk` | 50 | 单区块动物上限（**仅统计/预留**） |
| `entity-control.max-items-per-chunk` | 100 | 单区块掉落物上限（**仅统计/预留**） |
| `entity-control.protect.named-entities` | true | 保护命名实体（判定受保护实体时生效） |
| `entity-control.protect.tamed-animals` | true | 保护驯服动物 |
| `entity-control.protect.villagers` | true | 保护村民 |
| `entity-control.protect.item-frames` | true | 保护物品展示框 |
| `entity-control.protect.armor-stands` | true | 保护盔甲架 |

### item-optimizer —— 掉落物优化
| 键 | 默认 | 说明 |
|----|------|------|
| `item-optimizer.enabled` | true | 掉落物优化开关 |
| `item-optimizer.cleanup-age-seconds` | 180 | 掉落物存在超过此秒数才可被清理 |
| `item-optimizer.protect-near-player.enabled` | true | 保护玩家附近掉落物 |
| `item-optimizer.protect-near-player.radius` | 8 | 保护半径 |
| `item-optimizer.merge-nearby-items.enabled` | true | 合并附近相同掉落物 |
| `item-optimizer.merge-nearby-items.radius` | 3 | 合并半径 |
| `item-optimizer.merge-nearby-items.max-items-per-run` | 3000 | 手动合并单次最多处理的掉落物数量（防卡） |
| `item-optimizer.merge-nearby-items.max-chunks-per-run` | 200 | 手动合并单次最多处理的区块数量（防卡） |
| `item-optimizer.merge-nearby-items.auto-max-items-per-run` | 800 | 自动优化触发时的轻量合并掉落物上限 |
| `item-optimizer.merge-nearby-items.auto-max-chunks-per-run` | 40 | 自动优化触发时的轻量合并区块上限 |

> **合并绝不吞物品**：合并严格遵守物品最大堆叠数量，超出单堆上限的部分会保留在其它掉落物上，合并前后总数量完全一致。
> 带自定义名称、附魔、Lore 等特殊属性的物品，以及玩家附近的掉落物**始终受保护**，不会被清理或合并。
> 合并按**区块分组**处理（不做全世界两两比较），并有单次处理上限；自动优化触发时只做轻量合并，避免卡顿时雪上加霜。

### exp-orb-optimizer —— 经验球优化
| 键 | 默认 | 说明 |
|----|------|------|
| `exp-orb-optimizer.enabled` | true | 经验球优化开关 |
| `exp-orb-optimizer.max-orbs-per-chunk` | 30 | 单区块经验球上限 |
| `exp-orb-optimizer.cleanup-when-over-limit` | true | 超限时清理多余经验球 |

### chunk-monitor —— 区块监控
| 键 | 默认 | 说明 |
|----|------|------|
| `chunk-monitor.enabled` | true | 区块监控开关 |
| `chunk-monitor.warning-loaded-chunks` | 8000 | 加载区块总数超此值时告警 |
| `chunk-monitor.force-unload.enabled` | **false** | 强制卸载区块（默认关闭） |

### redstone-monitor —— 红石检测
| 键 | 默认 | 说明 |
|----|------|------|
| `redstone-monitor.enabled` | true | 红石检测开关 |
| `redstone-monitor.check-window-seconds` | 10 | 统计时间窗口 |
| `redstone-monitor.max-events-per-chunk` | 300 | 单区块窗口内红石事件阈值 |
| `redstone-monitor.action` | **warn** | 动作：warn（仅提醒） / cancel（取消红石变化，不破坏方块） / break（危险，见下） |
| `redstone-monitor.break-blocks.enabled` | **false** | break 动作的二级开关，默认关闭；即使 action=break，此项为 false 也绝不破坏方块 |
| `redstone-monitor.break-blocks.materials` | 红石线/中继器/比较器/侦测器 | break 允许破坏的材质白名单，白名单之外的方块一律不破坏 |

> ⚠️ **break 是危险功能，默认关闭。** 只有同时满足 `action: break` 且 `break-blocks.enabled: true`，且方块材质在 `break-blocks.materials` 白名单内，才会破坏该方块。任何破坏都会记录控制台日志并提醒管理员。绝不破坏白名单之外的方块（不会误伤玩家建筑）。建议保持默认 `warn`。

### hopper-monitor —— 漏斗检测
| 键 | 默认 | 说明 |
|----|------|------|
| `hopper-monitor.enabled` | true | 漏斗检测开关 |
| `hopper-monitor.max-hoppers-per-chunk` | 64 | 单区块漏斗阈值 |
| `hopper-monitor.scan-interval-seconds` | 300 | 两轮扫描的间隔，不宜过短 |
| `hopper-monitor.max-chunks-per-scan-tick` | 100 | 每 tick 最多扫描的区块数（分批，避免卡顿） |
| `hopper-monitor.cache-expire-seconds` | 600 | 扫描结果缓存有效期 |

> 漏斗扫描**分批进行**：后台每 tick 只处理有限数量区块，一轮扫完后缓存结果。`/optimizer hoppers` 只读取最近一次缓存，**不会触发即时全量扫描**。漏斗检测**只提醒、不破坏漏斗**（已移除 break 动作）。

### spawn-limiter —— 刷怪限制
| 键 | 默认 | 说明 |
|----|------|------|
| `spawn-limiter.enabled` | true | 刷怪限制开关 |
| `spawn-limiter.only-when-lagging` | true | 仅在卡顿时限制 |
| `spawn-limiter.tps-below` | 18.0 | TPS 低于此值视为卡顿 |
| `spawn-limiter.max-monsters-per-chunk` | 40 | 单区块怪物上限 |
| `spawn-limiter.max-animals-per-chunk` | 50 | 单区块动物上限 |

### logging —— 日志
| 键 | 默认 | 说明 |
|----|------|------|
| `logging.console` | true | 告警输出到控制台 |
| `logging.file` | false | 告警写入 `plugins/JibaiOptimizer/optimizer.log` |

---

## 七、messages.yml 文本说明

所有提示文本均在 `messages.yml` 中，可自由修改。

- **颜色**：优先支持 [MiniMessage](https://docs.advntr.dev/minimessage/format.html) 标签（如 `<green>`、`<gold>`），同时兼容传统 `&a`、`&c`、`&e` 等颜色符号，可在同一行混用。
- **占位符**：用 `{}` 包裹，如 `{tps}`、`{mspt}`、`{used}`、`{max}`、`{percent}`、`{entities}`、`{chunks}`、`{items}`、`{orbs}`、`{chunk}`、`{count}` 等。
- `prefix` 为消息前缀，用于告警与操作反馈类消息。

修改后执行 `/optimizer reload` 即可生效。

---

## 八、功能行为说明

### TPS / MSPT 近似监控
Bukkit / Spigot 没有稳定通用的 TPS API，为兼容全核心，插件用调度器每 tick 采样、在滑动窗口内取平均 tick 间隔，据此计算**近似** TPS 与近似每 tick 耗时（不使用 NMS/反射，无法测得服务端真实单 tick 处理耗时，仅供趋势参考）。正常约 20 TPS / 50ms，数值越差说明越卡。

### 性能采集（轻量化）
采集分两级：**轻量采集**（每 `monitor.interval-seconds` 秒）只读内存与近似 TPS，不遍历实体；**全量扫描**（每 `monitor.full-scan-interval-seconds` 秒）按世界分批遍历实体与区块，每 tick 最多处理 `monitor.max-worlds-per-tick` 个世界，避免监控本身反向卡服。`status` 只读最近一次缓存，不触发即时全量扫描。

### 自动优化
当满足任一触发条件（TPS 低 / MSPT 高 / 内存高）且不在冷却中时，执行配置允许的动作并通知管理员。**冷却机制**保证不会频繁触发。自动优化只做**轻量合并**（受 `auto-max-*-per-run` 上限约束），不做全服大规模合并。

### 内存监控
使用 Java Runtime 获取已用 / 最大 / 空闲内存与百分比。超过 `warning-percent` / `critical-percent` 时限流告警。**默认不执行强制 GC**——强制 GC 可能造成卡顿，需显式开启且带冷却。

### 掉落物合并（绝不吞物品）
按区块分组合并（不做全世界 O(n²) 两两比较），严格遵守 `ItemStack` 最大堆叠上限，超出部分保留在其它掉落物实体上，**合并前后总数量完全一致**。带自定义名/附魔/Lore 的物品与玩家附近掉落物始终受保护。

### 红石检测
监听 `BlockRedstoneEvent`，按区块在滑动时间窗口内统计事件次数，超过阈值时触发动作。统计数据每分钟清理过期项，避免无限增长。**默认动作为 warn，不破坏红石。** `break` 为危险动作，必须同时设置 `action: break` 且 `break-blocks.enabled: true` 才生效，且只破坏白名单材质（默认仅红石线/中继器/比较器/侦测器），破坏时记录日志并提醒管理员。

### 漏斗检测（分批扫描）
后台**分批**扫描已加载区块（每 tick 最多处理 `max-chunks-per-scan-tick` 个区块），一轮扫完后缓存结果。`/optimizer hoppers` 只读最近一次缓存，**不触发即时全量扫描**。**只警告，不破坏漏斗。**

### 实体限制与刷怪限制
`entities` 指令实时统计各世界实体分类数量。`SpawnLimiterListener` 监听 `CreatureSpawnEvent`，**仅限制自然刷怪**（不影响刷怪蛋、指令、繁殖等），在卡顿或区块实体超限时取消部分自然生成。

> 所有实体、世界、玩家、区块相关操作均在**主线程**执行，不异步操作 Bukkit API。插件关闭时会取消所有定时任务。

---

## 九、打包方法

项目使用 **Gradle Kotlin DSL**，已内置 Gradle Wrapper。在项目根目录执行：

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

产物位于 `build/libs/JibaiOptimizer-1.0.0.jar`。

> 构建需要 JDK 17 或更高版本。项目以 Java 17 为发布目标（`options.release = 17`），因此可用 JDK 17/21/25 等任意较新 JDK 构建，产出的字节码均可在 Java 17 运行。

---

## 十、测试方法

1. 使用 Gradle 打包插件。
2. 将 jar 放入服务器（Bukkit / Spigot / Paper / Purpur）`plugins/` 文件夹并启动服务器。
3. 确认控制台输出 `JibaiOptimizer 已启用`。
4. 依次执行：
   - `/optimizer status`
   - `/optimizer memory`
   - `/optimizer entities`
   - `/optimizer chunks`
5. 往地上丢大量物品，等待超过 `cleanup-age-seconds` 后测试掉落物清理。
6. 生成大量经验球，测试经验球清理。
7. 制造高频红石（如快速时钟），测试红石告警。
8. 放置大量漏斗（超过阈值），执行 `/optimizer hoppers` 或等待扫描测试漏斗告警。
9. 临时调低 `auto-optimizer.trigger` 阈值，测试自动优化触发。
10. 执行 `/optimizer clean`，测试手动优化。
11. 修改 `messages.yml` 后执行 `/optimizer reload`，确认文本生效。
12. 检查控制台与 `logs/latest.log` 是否有报错。

---

## 十一、常见问题

**Q：装了插件为什么还是卡？**
A：本插件降低资源占用并提供数据，但无法解决硬件不足、插件冲突、超大红石机器等根本问题。请结合 `/optimizer status` 与告警信息定位瓶颈。

**Q：为什么我的物品/村民/盔甲架没有被清理？**
A：这是有意的保护。带自定义名的物品、玩家附近的掉落物、命名实体、驯服动物、村民、物品展示框、盔甲架默认均受保护。

**Q：支持 Folia 吗？**
A：不支持。插件使用普通 Bukkit 调度器。

**Q：自动优化会不会误删玩家的东西？**
A：不会删除方块或建筑。清理仅针对超时的普通掉落物和超量经验球，且遵循上述保护规则。红石、漏斗、区块默认只告警不破坏。

**Q：强制 GC 有用吗？**
A：强制 GC 可能引发明显卡顿，默认关闭。仅在明确了解风险时才建议开启。

**Q：修改配置后不生效？**
A：请执行 `/optimizer reload`，或重启服务器。

---

## 十二、项目结构

```text
src/main/java/me/jibai/optimizer/
  JibaiOptimizerPlugin.java        主类：装配模块、生命周期管理
  command/                         指令与 Tab 补全
  config/                          配置与消息管理
  monitor/                         性能 / 内存 / TPS 监控
  optimizer/                       自动优化与冷却
  entity/                          掉落物 / 经验球 / 实体 / 刷怪
  chunk/                           区块 / 漏斗监控
  redstone/                        红石检测
  alert/                           告警与管理员通知
  util/                            颜色 / 格式 / 权限等工具
src/main/resources/
  plugin.yml  config.yml  messages.yml
```
