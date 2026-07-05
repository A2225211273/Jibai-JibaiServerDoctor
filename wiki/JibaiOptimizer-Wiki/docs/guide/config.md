# 配置说明

配置文件位于：

```text
plugins/JibaiOptimizer/config.yml
```

修改后执行：

```text
/optimizer reload
```

## settings

插件总开关和语言标识。

## monitor

控制性能采集间隔、全量扫描间隔和分批扫描参数。TPS / MSPT 为兼容全核心而使用调度器采样得到，是趋势值，不等于 Paper 内部精确指标。

## auto-optimizer

当 TPS 低于阈值、近似 MSPT 高于阈值或内存占用高于阈值时，自动执行低风险优化。

## memory

控制内存告警阈值。强制 GC 默认关闭，因为它可能导致短时间卡顿。

## entity-control

当前主要用于实体统计和受保护实体判断。`max-*-per-chunk` 属于统计和预留配置，不应理解为会主动清理所有实体。

## item-optimizer

控制掉落物清理、玩家附近保护、特殊物品保护和掉落物合并。

## exp-orb-optimizer

控制经验球超量清理。

## chunk-monitor

监控加载区块数量，超过阈值时提醒管理员。强制卸载默认关闭。

## redstone-monitor

检测红石高频区块。默认 `action: warn`，只提醒不破坏。

如需开启破坏动作，必须同时满足：

```yaml
redstone-monitor:
  action: break
  break-blocks:
    enabled: true
```

并且只会处理白名单材质。

## hopper-monitor

后台分批扫描漏斗密集区块，`/optimizer hoppers` 只读取缓存，不触发即时全量扫描。插件只提醒，不破坏漏斗。

## spawn-limiter

服务器卡顿时限制部分自然刷怪。

## logging

控制控制台和文件日志输出。
