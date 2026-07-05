package me.jibai.optimizer.optimizer;

/**
 * 自动优化冷却管理。记录上次执行时间，用于避免频繁触发优化。
 */
public class OptimizationCooldown {

    private long lastRun = 0L;

    /**
     * 距离上次执行是否已超过冷却时间。
     */
    public boolean isReady(long cooldownSeconds) {
        return System.currentTimeMillis() - lastRun >= cooldownSeconds * 1000L;
    }

    /**
     * 标记为刚刚执行。
     */
    public void markRun() {
        this.lastRun = System.currentTimeMillis();
    }

    /**
     * 剩余冷却秒数（不小于 0）。
     */
    public long remainingSeconds(long cooldownSeconds) {
        long elapsed = System.currentTimeMillis() - lastRun;
        long remaining = cooldownSeconds * 1000L - elapsed;
        return Math.max(0L, remaining / 1000L);
    }
}
