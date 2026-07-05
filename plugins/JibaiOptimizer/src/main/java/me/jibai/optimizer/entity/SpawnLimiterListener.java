package me.jibai.optimizer.entity;

import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.monitor.TpsMonitor;
import org.bukkit.Chunk;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * 刷怪限制监听器。监听 {@link CreatureSpawnEvent}，在服务器卡顿或区块实体过多时限制自然刷怪。
 *
 * <p>仅限制自然生成（{@code SpawnReason.NATURAL}），不影响管理员用刷怪蛋、指令、繁殖等方式生成实体。</p>
 */
public class SpawnLimiterListener implements Listener {

    private final ConfigManager config;
    private final TpsMonitor tpsMonitor;

    public SpawnLimiterListener(ConfigManager config, TpsMonitor tpsMonitor) {
        this.config = config;
        this.tpsMonitor = tpsMonitor;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!config.getBool("spawn-limiter.enabled", true)) {
            return;
        }
        // 只限制自然刷怪，保留管理员/繁殖/刷怪蛋等生成方式
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        boolean onlyWhenLagging = config.getBool("spawn-limiter.only-when-lagging", true);
        double tpsThreshold = config.getDouble("spawn-limiter.tps-below", 18.0);
        boolean lagging = tpsMonitor.getTps() < tpsThreshold;

        // 若配置为“仅卡顿时限制”，而当前不卡顿，则不限制
        if (onlyWhenLagging && !lagging) {
            return;
        }

        Entity entity = event.getEntity();
        Chunk chunk = event.getLocation().getChunk();

        int monsters = 0;
        int animals = 0;
        for (Entity e : chunk.getEntities()) {
            if (e instanceof Monster) {
                monsters++;
            } else if (e instanceof Animals) {
                animals++;
            }
        }

        int maxMonsters = config.getInt("spawn-limiter.max-monsters-per-chunk", 40);
        int maxAnimals = config.getInt("spawn-limiter.max-animals-per-chunk", 50);

        if (entity instanceof Monster && monsters >= maxMonsters) {
            event.setCancelled(true);
        } else if (entity instanceof Animals && animals >= maxAnimals) {
            event.setCancelled(true);
        }
    }
}
