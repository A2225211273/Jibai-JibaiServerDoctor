# 性能监控

JibaiOptimizer 会定期采集服务器运行数据，并提供 `/optimizer status` 查看当前状态。

## TPS

TPS 表示服务器每秒处理 tick 的能力。理想情况下接近 20。

## 近似 MSPT

为了兼容 Bukkit / Spigot / Paper / Purpur，插件不依赖 Paper 专属 API，而是使用调度器采样计算近似每 tick 间隔。

这个值适合观察趋势：数值越高，服务器越可能处于压力状态。

## 内存

插件会显示已用内存、最大内存和占用百分比，并在达到阈值时提醒管理员。
