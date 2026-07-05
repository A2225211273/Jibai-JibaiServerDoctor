package me.jibai.serverdoctor.inspect;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.health.RiskLevel;
import me.jibai.serverdoctor.hopper.HopperScanner;
import me.jibai.serverdoctor.redstone.RedstoneTracker;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

/**
 * 玩家附近检查服务。
 *
 * <p>以玩家为中心，检查周围若干区块范围内的实体、掉落物、经验球、漏斗、红石与活跃区块，
 * 综合判断该玩家附近是否可能造成卡顿。必须在主线程调用。</p>
 *
 * @author 即白
 */
public class PlayerInspectService {

    /** 检查半径（区块数），即以玩家所在区块为中心向外扩展的圈数。 */
    private static final int CHUNK_RADIUS = 4;

    private final ConfigManager config;
    private final RedstoneTracker redstoneTracker;
    private final HopperScanner hopperScanner;

    public PlayerInspectService(ConfigManager config, RedstoneTracker redstoneTracker,
                                HopperScanner hopperScanner) {
        this.config = config;
        this.redstoneTracker = redstoneTracker;
        this.hopperScanner = hopperScanner;
    }

    /** 玩家附近检查半径对应的方块数，用于展示。 */
    public int getBlockRadius() {
        return CHUNK_RADIUS * 16;
    }

    /**
     * 检查指定玩家附近。必须在主线程调用。
     */
    public InspectResult inspect(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        int centerChunkX = loc.getBlockX() >> 4;
        int centerChunkZ = loc.getBlockZ() >> 4;

        int entities = 0, items = 0, orbs = 0, monsters = 0, animals = 0, hoppers = 0, redstone = 0;
        int activeChunks = 0;

        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                activeChunks++;
                Chunk chunk = world.getChunkAt(cx, cz);
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
                hoppers += hopperScanner.countHoppers(chunk);
                redstone += redstoneTracker.getCount(world.getName(), cx, cz);
            }
        }

        RiskLevel level = evaluate(entities, items, hoppers, redstone);
        return new InspectResult(world.getName(), centerChunkX, centerChunkZ,
                entities, items, orbs, monsters, animals, hoppers, redstone, activeChunks, level);
    }

    /** 区域风险评级：范围更大，阈值相应放大。 */
    private RiskLevel evaluate(int entities, int items, int hoppers, int redstone) {
        int score = 0;
        if (entities >= 1200) {
            score += 3;
        } else if (entities >= 600) {
            score += 2;
        } else if (entities >= 250) {
            score += 1;
        }
        if (items >= 600) {
            score += 2;
        } else if (items >= 250) {
            score += 1;
        }
        if (hoppers >= config.getHopperMaxPerChunk() * 3) {
            score += 2;
        } else if (hoppers >= config.getHopperMaxPerChunk()) {
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
