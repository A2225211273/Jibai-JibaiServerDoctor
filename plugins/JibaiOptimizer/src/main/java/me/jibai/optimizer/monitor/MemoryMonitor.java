package me.jibai.optimizer.monitor;

import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.util.FormatUtil;

/**
 * JVM 内存监控器。使用 Java {@link Runtime} 获取内存数据，并可选执行强制 GC（默认关闭）。
 */
public class MemoryMonitor {

    private final ConfigManager config;
    private long lastGcTime = 0L;

    public MemoryMonitor(ConfigManager config) {
        this.config = config;
    }

    /** 已用内存（MB）= 已分配堆 - 空闲堆。 */
    public long getUsedMb() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return FormatUtil.toMb(used);
    }

    /** 最大可用内存（MB），即 -Xmx。 */
    public long getMaxMb() {
        return FormatUtil.toMb(Runtime.getRuntime().maxMemory());
    }

    /** 相对最大堆的空闲内存（MB）。 */
    public long getFreeMb() {
        return Math.max(0L, getMaxMb() - getUsedMb());
    }

    /** 内存使用百分比（0-100）。 */
    public double getPercent() {
        return FormatUtil.percent(getUsedMb(), getMaxMb());
    }

    /**
     * 在满足配置条件时尝试执行一次强制 GC。
     *
     * <p>强制 GC 默认关闭（{@code memory.force-gc.enabled=false}），且带有冷却时间，
     * 避免因频繁 GC 造成卡顿。</p>
     *
     * @return 是否真正执行了 GC
     */
    public boolean tryForceGc() {
        if (!config.getBool("memory.force-gc.enabled", false)) {
            return false;
        }
        double threshold = config.getDouble("memory.force-gc.threshold-percent", 92);
        if (getPercent() < threshold) {
            return false;
        }
        long cooldownMs = config.getLong("memory.force-gc.cooldown-seconds", 600) * 1000L;
        long now = System.currentTimeMillis();
        if (now - lastGcTime < cooldownMs) {
            return false;
        }
        lastGcTime = now;
        System.gc();
        return true;
    }
}
