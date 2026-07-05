package me.jibai.serverdoctor.inspect;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.health.RiskLevel;
import me.jibai.serverdoctor.hopper.HopperScanner;
import me.jibai.serverdoctor.redstone.RedstoneTracker;
import org.bukkit.Chunk;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;

/**
 * 区块检查服务。
 *
 * <p>检查一个具体区块：统计实体、掉落物、经验球、怪物、动物、漏斗数量，
 * 读取该区块的红石事件频率，并综合给出风险评级。必须在主线程调用。</p>
 *
 * @author 即白
 */
public class ChunkInspectService {

    private final ConfigManager config;
    private final RedstoneTracker redstoneTracker;
    private final HopperScanner hopperScanner;

    public ChunkInspectService(ConfigManager config, RedstoneTracker redstoneTracker,
                               HopperScanner hopperScanner) {
        this.config = config;
        this.redstoneTracker = redstoneTracker;
        this.hopperScanner = hopperScanner;
    }

    /**
     * 检查指定区块。必须在主线程调用（访问区块实体）。
     */
    public InspectResult inspect(Chunk chunk) {
        int entities = 0, items = 0, orbs = 0, monsters = 0, animals = 0;
        for (Entity entity : chunk.getEntities()) {
            entities++;
            EntityType type = entity.getType();
            if (type == EntityType.DROPPED_ITEM || entity instanceof Item) {
                items++;
            } else if (type == EntityType.EXPERIENCE_ORB || entity instanceof ExperienceOrb) {
                orbs++;
            } else if (entity instanceof Monster) {
                monsters++;
            } else if (entity instanceof Animals) {
                animals++;
            }
        }

        int hoppers = hopperScanner.countHoppers(chunk);
        int redstone = redstoneTracker.getCount(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

        RiskLevel level = evaluate(entities, items, hoppers, redstone);
        return new InspectResult(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(),
                entities, items, orbs, monsters, animals, hoppers, redstone, 1, level);
    }

    /** 单区块风险评级：综合实体、掉落物、漏斗、红石。 */
    private RiskLevel evaluate(int entities, int items, int hoppers, int redstone) {
        int score = 0;
        // 单区块实体密度阈值相对整服更小
        if (entities >= 300) {
            score += 3;
        } else if (entities >= 150) {
            score += 2;
        } else if (entities >= 60) {
            score += 1;
        }
        if (items >= 200) {
            score += 2;
        } else if (items >= 80) {
            score += 1;
        }
        if (hoppers >= config.getHopperMaxPerChunk()) {
            score += 2;
        } else if (hoppers >= config.getHopperMaxPerChunk() / 2) {
            score += 1;
        }
        if (redstone >= config.getRedstoneMaxEventsPerChunk()) {
            score += 3;
        } else if (redstone >= config.getRedstoneMaxEventsPerChunk() / 2) {
            score += 1;
        }

        if (score >= 6) {
            return RiskLevel.CRITICAL;
        } else if (score >= 4) {
            return RiskLevel.HIGH;
        } else if (score >= 2) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
