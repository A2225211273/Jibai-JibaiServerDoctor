package me.jibai.optimizer.optimizer;

import me.jibai.optimizer.alert.AlertService;
import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.config.MessageManager;
import me.jibai.optimizer.entity.ExpOrbOptimizer;
import me.jibai.optimizer.entity.ItemOptimizer;
import me.jibai.optimizer.monitor.MemoryMonitor;
import me.jibai.optimizer.monitor.ServerSnapshot;
import me.jibai.optimizer.util.FormatUtil;

/**
 * 自动优化器。
 *
 * <p>根据 {@link ServerSnapshot} 判断服务器压力（TPS / MSPT / 内存），在超过阈值且不在冷却中时，
 * 执行配置允许的低风险优化动作。同时负责内存告警的分发。</p>
 */
public class AutoOptimizer {

    private final ConfigManager config;
    private final MessageManager messages;
    private final AlertService alert;
    private final ItemOptimizer itemOptimizer;
    private final ExpOrbOptimizer expOrbOptimizer;
    private final MemoryMonitor memoryMonitor;
    private final OptimizationCooldown cooldown = new OptimizationCooldown();

    public AutoOptimizer(ConfigManager config, MessageManager messages, AlertService alert,
                         ItemOptimizer itemOptimizer, ExpOrbOptimizer expOrbOptimizer,
                         MemoryMonitor memoryMonitor) {
        this.config = config;
        this.messages = messages;
        this.alert = alert;
        this.itemOptimizer = itemOptimizer;
        this.expOrbOptimizer = expOrbOptimizer;
        this.memoryMonitor = memoryMonitor;
    }

    /**
     * 每次采集后调用：处理内存告警并在满足条件时触发自动优化。必须在主线程执行。
     */
    public void check(ServerSnapshot snapshot) {
        checkMemoryAlert(snapshot);

        if (!config.getBool("auto-optimizer.enabled", true)) {
            return;
        }
        if (!isUnderPressure(snapshot)) {
            return;
        }
        long cooldownSeconds = config.getLong("auto-optimizer.cooldown-seconds", 120);
        if (!cooldown.isReady(cooldownSeconds)) {
            // 冷却中：静默跳过，避免刷屏
            return;
        }
        cooldown.markRun();

        boolean notify = config.getBool("auto-optimizer.actions.notify-admins", true);
        if (notify) {
            alert.alert(messages.withPrefix("optimizer.started"));
        }

        OptimizationResult result = runAutoActions();

        if (notify) {
            alert.alert(messages.withPrefix("optimizer.finished", MessageManager.ph(
                    "items", String.valueOf(result.getCleanedItems()),
                    "orbs", String.valueOf(result.getCleanedOrbs()),
                    "merged", String.valueOf(result.getMergedGroups()))));
        }
    }

    /**
     * 手动优化（/optimizer clean）。执行全部低风险清理，不受自动冷却限制。
     */
    public OptimizationResult runManual() {
        int items = itemOptimizer.cleanupOldItems();
        int orbs = expOrbOptimizer.cleanupOverLimit();
        int merged = itemOptimizer.mergeNearbyItems();
        return new OptimizationResult(items, orbs, merged);
    }

    /**
     * 按 auto-optimizer.actions.* 开关执行自动优化动作。
     */
    private OptimizationResult runAutoActions() {
        int items = 0;
        int orbs = 0;
        int merged = 0;

        if (config.getBool("auto-optimizer.actions.cleanup-dropped-items", true)) {
            items = itemOptimizer.cleanupOldItems();
        }
        if (config.getBool("auto-optimizer.actions.cleanup-exp-orbs", true)) {
            orbs = expOrbOptimizer.cleanupOverLimit();
        }
        if (config.getBool("auto-optimizer.actions.merge-nearby-items", true)) {
            // 自动优化只做轻量合并，避免在服务器已卡顿时做大规模全服合并
            merged = itemOptimizer.mergeNearbyItemsLight();
        }
        if (config.getBool("auto-optimizer.actions.limit-spawn", true)) {
            // 实际刷怪限制由 SpawnLimiterListener 常驻处理，这里发一次提示
            alert.alertThrottled("spawn-limited", 120, messages.withPrefix("spawn.limited"));
        }
        // 内存高时尝试强制 GC（内部自查开关与冷却，默认关闭）
        memoryMonitor.tryForceGc();

        return new OptimizationResult(items, orbs, merged);
    }

    private boolean isUnderPressure(ServerSnapshot snapshot) {
        double tpsBelow = config.getDouble("auto-optimizer.trigger.tps-below", 18.5);
        double msptAbove = config.getDouble("auto-optimizer.trigger.mspt-above", 45);
        double memAbove = config.getDouble("auto-optimizer.trigger.memory-percent-above", 85);
        return snapshot.getTps() < tpsBelow
                || snapshot.getMspt() > msptAbove
                || snapshot.getMemoryPercent() > memAbove;
    }

    /**
     * 内存告警：根据 warning/critical 阈值限流提醒管理员。
     */
    private void checkMemoryAlert(ServerSnapshot snapshot) {
        double percent = snapshot.getMemoryPercent();
        double warn = config.getDouble("memory.warning-percent", 85);
        double crit = config.getDouble("memory.critical-percent", 92);

        var ph = MessageManager.ph(
                "percent", FormatUtil.round1(percent),
                "used", String.valueOf(snapshot.getUsedMemoryMb()),
                "max", String.valueOf(snapshot.getMaxMemoryMb()));

        if (percent >= crit) {
            alert.alertMessageThrottled("memory-critical", 60, "memory.critical", ph);
        } else if (percent >= warn) {
            alert.alertMessageThrottled("memory-warning", 120, "memory.warning", ph);
        }
    }
}
