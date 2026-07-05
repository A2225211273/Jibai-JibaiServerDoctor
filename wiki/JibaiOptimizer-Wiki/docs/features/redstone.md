# 红石高频检测

红石机器是服务器卡顿的常见来源之一。JibaiOptimizer 会统计区块内红石事件频率，并在超过阈值时提醒管理员。

## 默认行为

默认只警告，不破坏方块。

```yaml
redstone-monitor:
  action: warn
```

## 危险动作

`break` 是危险动作，必须显式开启双重开关：

```yaml
redstone-monitor:
  action: break
  break-blocks:
    enabled: true
```

并且只会破坏白名单里的红石元件。任何破坏都会写入日志并提醒管理员。
