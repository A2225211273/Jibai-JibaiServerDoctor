package me.jibai.optimizer.chunk;

import me.jibai.optimizer.alert.AlertService;
import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 区块监控器。统计各世界已加载区块数量，加载区块总数超过阈值时提醒管理员。
 * 默认不强制卸载区块（{@code chunk-monitor.force-unload.enabled=false}）。
 */
public class ChunkMonitor {

    private final ConfigManager config;
    private final AlertService alert;

    public ChunkMonitor(ConfigManager config, AlertService alert) {
        this.config = config;
        this.alert = alert;
    }

    /**
     * 实时统计各世界已加载区块数量。
     *
     * @return 世界名 -> 加载区块数（保持世界顺序）
     */
    public Map<String, Integer> collectStats() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            result.put(world.getName(), world.getLoadedChunks().length);
        }
        return result;
    }

    public int getTotalLoadedChunks() {
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            total += world.getLoadedChunks().length;
        }
        return total;
    }

    /**
     * 检查加载区块总数是否超过告警阈值，超过则限流告警。
     *
     * @param totalLoadedChunks 当前加载区块总数
     */
    public void checkWarning(int totalLoadedChunks) {
        if (!config.getBool("chunk-monitor.enabled", true)) {
            return;
        }
        int threshold = config.getInt("chunk-monitor.warning-loaded-chunks", 8000);
        if (totalLoadedChunks > threshold) {
            alert.alertMessageThrottled("chunk-warning", 120, "chunks.warning",
                    MessageManager.ph(
                            "chunks", String.valueOf(totalLoadedChunks),
                            "threshold", String.valueOf(threshold)));
        }
    }
}
