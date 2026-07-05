package me.jibai.optimizer.entity;

import me.jibai.optimizer.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实体数量控制服务。
 *
 * <p>负责统计各世界的实体分类数量、判断实体是否为受保护类型，并向命令提供实时统计数据。
 * 遍历实体的方法必须在主线程执行。</p>
 */
public class EntityControlService {

    private final ConfigManager config;

    public EntityControlService(ConfigManager config) {
        this.config = config;
    }

    /** 单个世界的实体统计。 */
    public static class WorldEntityStats {
        public final String worldName;
        public int entities;
        public int monsters;
        public int animals;
        public int items;

        public WorldEntityStats(String worldName) {
            this.worldName = worldName;
        }
    }

    /**
     * 实时统计所有世界的实体分类数量。
     *
     * @return 世界名 -> 统计（保持世界顺序）
     */
    public Map<String, WorldEntityStats> collectStats() {
        Map<String, WorldEntityStats> result = new LinkedHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            WorldEntityStats stats = new WorldEntityStats(world.getName());
            for (Entity entity : world.getEntities()) {
                stats.entities++;
                if (entity instanceof Monster) {
                    stats.monsters++;
                } else if (entity instanceof Animals) {
                    stats.animals++;
                } else if (entity instanceof Item) {
                    stats.items++;
                }
            }
            result.put(world.getName(), stats);
        }
        return result;
    }

    /**
     * 判断实体是否受保护，受保护实体默认不会被清理。
     *
     * <p>保护规则由 config 的 {@code entity-control.protect.*} 控制：
     * 命名实体、驯服动物、村民、物品展示框、盔甲架。</p>
     */
    public boolean isProtected(Entity entity) {
        if (config.getBool("entity-control.protect.named-entities", true) && entity.getCustomName() != null) {
            return true;
        }
        if (config.getBool("entity-control.protect.tamed-animals", true)
                && entity instanceof Tameable && ((Tameable) entity).isTamed()) {
            return true;
        }
        if (config.getBool("entity-control.protect.villagers", true) && entity instanceof Villager) {
            return true;
        }
        if (config.getBool("entity-control.protect.item-frames", true) && entity instanceof ItemFrame) {
            return true;
        }
        if (config.getBool("entity-control.protect.armor-stands", true) && entity instanceof ArmorStand) {
            return true;
        }
        return false;
    }
}
