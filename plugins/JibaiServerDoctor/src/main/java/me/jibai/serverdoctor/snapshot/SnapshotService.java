package me.jibai.serverdoctor.snapshot;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.util.FormatUtil;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * 服务器快照采集服务。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>用一个每 tick 运行的轻量任务测量相邻两次执行的真实间隔，从而计算近似 tick 延迟
 *       （理想为 50ms，超过说明服务器在追赶）。<b>不依赖 Paper 的 TPS API</b>，保证全核心兼容。</li>
 *   <li>按配置间隔在主线程遍历所有世界，统计实体 / 掉落物 / 经验球 / 怪物 / 动物 / 区块，
 *       连同 JVM 内存生成一份 {@link ServerSnapshot}，并保留最近若干份历史。</li>
 * </ul>
 *
 * <p>所有对世界 / 实体的访问均在主线程进行，符合 Bukkit 线程模型。</p>
 *
 * @author 即白
 */
public class SnapshotService {

    private final JavaPlugin plugin;
    private final ConfigManager config;

    /** 最近的历史快照，队首最旧、队尾最新。 */
    private final Deque<ServerSnapshot> history = new ArrayDeque<>();

    private BukkitTask tickTask;
    private BukkitTask snapshotTask;

    /** 上一次 tick 任务执行的系统时间（毫秒）。 */
    private long lastTickMillis = -1;
    /** 最近一次测得的 tick 间隔（毫秒），初始为理想值 50。 */
    private volatile double lastTickDelayMs = 50.0;

    /** 采集完成回调（在主线程调用）。 */
    private Consumer<ServerSnapshot> onSnapshot;

    public SnapshotService(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * 启动采集任务。
     *
     * @param onSnapshot 每次生成快照后的回调（主线程），可为 null
     */
    public void start(Consumer<ServerSnapshot> onSnapshot) {
        this.onSnapshot = onSnapshot;

        // tick 延迟测量任务：每 tick 执行一次，记录真实间隔
        this.lastTickMillis = System.currentTimeMillis();
        this.tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            if (lastTickMillis > 0) {
                double delta = now - lastTickMillis;
                // 用滑动平均平滑抖动，权重偏向历史，避免单次卡顿造成剧烈跳变
                lastTickDelayMs = lastTickDelayMs * 0.8 + delta * 0.2;
            }
            lastTickMillis = now;
        }, 1L, 1L);

        // 完整快照任务
        long intervalTicks = config.getSnapshotIntervalSeconds() * 20L;
        this.snapshotTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::collectAndStore, intervalTicks, intervalTicks);

        if (config.isCollectOnStartup()) {
            // 启动后延迟 5 秒采集首份快照，给服务器留出加载世界的时间
            plugin.getServer().getScheduler().runTaskLater(plugin, this::collectAndStore, 100L);
        }
    }

    /** 停止所有采集任务。 */
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (snapshotTask != null) {
            snapshotTask.cancel();
            snapshotTask = null;
        }
        lastTickMillis = -1;
    }

    /** 采集并存入历史，同时触发回调。必须在主线程调用。 */
    public void collectAndStore() {
        ServerSnapshot snapshot = capture();
        history.addLast(snapshot);
        while (history.size() > config.getSnapshotKeepHistory()) {
            history.pollFirst();
        }
        if (onSnapshot != null) {
            try {
                onSnapshot.accept(snapshot);
            } catch (Exception ex) {
                plugin.getLogger().warning("快照回调处理异常：" + ex.getMessage());
            }
        }
    }

    /**
     * 立即采集一份快照（不存入历史）。必须在主线程调用。
     * 供 /doctor status 等需要即时数据的场景使用。
     */
    public ServerSnapshot capture() {
        Runtime runtime = Runtime.getRuntime();
        long maxBytes = runtime.maxMemory();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        long usedMb = FormatUtil.toMegabytes(usedBytes);
        long maxMb = FormatUtil.toMegabytes(maxBytes);
        int memPercent = FormatUtil.percent(usedBytes, maxBytes);

        List<WorldSnapshot> worlds = new ArrayList<>();
        for (World world : plugin.getServer().getWorlds()) {
            worlds.add(captureWorld(world));
        }

        int players = plugin.getServer().getOnlinePlayers().size();
        return new ServerSnapshot(System.currentTimeMillis(), players, worlds,
                usedMb, maxMb, memPercent, lastTickDelayMs);
    }

    /** 采集单个世界的实体分类统计。必须在主线程调用。 */
    private WorldSnapshot captureWorld(World world) {
        int entities = 0, items = 0, orbs = 0, monsters = 0, animals = 0;
        for (Entity entity : world.getEntities()) {
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
        int loadedChunks = world.getLoadedChunks().length;
        return new WorldSnapshot(world.getName(), entities, items, orbs, monsters, animals, loadedChunks);
    }

    /** 获取最新一份历史快照，没有则即时采集一份。必须在主线程调用。 */
    public ServerSnapshot getLatestOrCapture() {
        ServerSnapshot latest = history.peekLast();
        return latest != null ? latest : capture();
    }

    /** 只读历史列表（从旧到新）。 */
    public List<ServerSnapshot> getHistory() {
        return new ArrayList<>(history);
    }

    /** 当前近似 tick 延迟（毫秒）。 */
    public double getApproxTickDelayMs() {
        return lastTickDelayMs;
    }
}
