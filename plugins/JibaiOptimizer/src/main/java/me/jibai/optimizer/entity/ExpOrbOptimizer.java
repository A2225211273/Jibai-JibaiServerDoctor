package me.jibai.optimizer.entity;

import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 经验球优化器。按区块统计经验球数量，超过阈值时清理多余部分。
 * 遍历实体，必须在主线程执行。
 */
public class ExpOrbOptimizer {

    private final ConfigManager config;

    public ExpOrbOptimizer(ConfigManager config) {
        this.config = config;
    }

    /**
     * 清理超过每区块上限的经验球。
     *
     * @return 被清理的经验球数量
     */
    public int cleanupOverLimit() {
        if (!config.getBool("exp-orb-optimizer.enabled", true)) {
            return 0;
        }
        if (!config.getBool("exp-orb-optimizer.cleanup-when-over-limit", true)) {
            return 0;
        }
        int max = config.getInt("exp-orb-optimizer.max-orbs-per-chunk", 30);
        if (max < 0) {
            return 0;
        }

        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            // 按区块分组
            Map<String, List<ExperienceOrb>> byChunk = new HashMap<>();
            for (ExperienceOrb orb : world.getEntitiesByClass(ExperienceOrb.class)) {
                String key = TimeUtil.chunkKey(orb.getLocation().getChunk());
                byChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(orb);
            }
            // 超过上限的区块，移除多余的经验球（保留 max 个）
            for (List<ExperienceOrb> orbs : byChunk.values()) {
                if (orbs.size() <= max) {
                    continue;
                }
                for (int i = max; i < orbs.size(); i++) {
                    ExperienceOrb orb = orbs.get(i);
                    if (!orb.isDead()) {
                        orb.remove();
                        removed++;
                    }
                }
            }
        }
        return removed;
    }
}
