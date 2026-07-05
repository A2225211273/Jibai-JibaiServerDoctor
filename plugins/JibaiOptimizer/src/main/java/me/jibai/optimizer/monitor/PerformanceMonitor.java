package me.jibai.optimizer.monitor;

import me.jibai.optimizer.JibaiOptimizerPlugin;
import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.util.FormatUtil;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.function.Consumer;

/**
 * 性能监控器（问题 4 修复：轻量化 + 分批扫描 + 缓存）。
 *
 * <p>为避免监控任务本身反向卡服，采集拆成两条独立任务：</p>
 * <ul>
 *   <li><b>轻量采集任务</b>（每 {@code monitor.interval-seconds} 秒）：只读取内存、近似 TPS/MSPT、
 *       在线人数、世界数，并把最近一次全量扫描缓存的实体/区块数量填入快照。这些操作极轻量，
 *       不遍历实体。采集完成后回调监听器（用于自动优化与内存告警）。</li>
 *   <li><b>分批扫描任务</b>（每 tick 推进一次）：按 {@code monitor.full-scan-interval-seconds} 的间隔
 *       启动新一轮全量扫描，每 tick 最多遍历 {@code monitor.max-worlds-per-tick} 个世界的实体与区块，
 *       一整轮扫描完成后整体提交缓存。这样既不会在单 tick 内遍历所有世界，又能在启动后几 tick 内
 *       完成首轮扫描，让 {@code status} 迅速有数据。</li>
 * </ul>
 *
 * <p>{@code status} 指令通过 {@link #getLatest()} 只读取缓存快照，绝不触发即时全量扫描。</p>
 *
 * @author 即白
 */
public class PerformanceMonitor {

    private final JibaiOptimizerPlugin plugin;
    private final ConfigManager config;
    private final MemoryMonitor memoryMonitor;
    private final TpsMonitor tpsMonitor;

    private BukkitTask lightTask;
    private BukkitTask scanTask;
    private volatile ServerSnapshot latest;
    private Consumer<ServerSnapshot> listener;

    // -------------------- 全量扫描的缓存与分批状态 --------------------

    /** 上次完成全量实体/区块扫描的时间戳（毫秒）。0 表示从未扫描，首轮会尽快启动。 */
    private long lastFullScanMillis = 0L;
    /** 最近一次全量扫描得到的统计（在下次全量扫描完成前复用）。 */
    private volatile int cachedEntities = 0;
    private volatile int cachedItems = 0;
    private volatile int cachedOrbs = 0;
    private volatile int cachedChunks = 0;

    // 分批扫描的累加器与游标（跨多 tick 累加，扫完一整轮再整体替换缓存）
    private int scanCursor = 0;
    private int accEntities = 0;
    private int accItems = 0;
    private int accOrbs = 0;
    private int accChunks = 0;
    private boolean scanning = false;

    public PerformanceMonitor(JibaiOptimizerPlugin plugin, ConfigManager config,
                              MemoryMonitor memoryMonitor, TpsMonitor tpsMonitor) {
        this.plugin = plugin;
        this.config = config;
        this.memoryMonitor = memoryMonitor;
        this.tpsMonitor = tpsMonitor;
    }

    /**
     * 启动采集与分批扫描任务。
     *
     * @param listener 每次轻量采集完成后的回调（主线程），可为 null
     */
    public void start(Consumer<ServerSnapshot> listener) {
        this.listener = listener;
        this.lastFullScanMillis = 0L;
        this.scanning = false;

        long intervalTicks = Math.max(20L, config.getLong("monitor.interval-seconds", 10) * 20L);
        // 轻量采集：延迟 40 tick（约 2 秒）启动，确保世界与实体已加载完毕
        this.lightTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::collectLight, 40L, intervalTicks);
        // 分批扫描：每 tick 推进一次，内部按间隔与批量大小自控节奏，启动后很快完成首轮
        this.scanTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::scanTick, 40L, 1L);
    }

    /**
     * 停止全部任务。
     */
    public void stop() {
        if (lightTask != null) {
            lightTask.cancel();
            lightTask = null;
        }
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    /**
     * 轻量采集（主线程）：只读内存与近似 TPS/MSPT，实体/区块沿用缓存，绝不在此遍历实体。
     */
    public void collectLight() {
        long usedMb = memoryMonitor.getUsedMb();
        long maxMb = memoryMonitor.getMaxMb();
        long freeMb = memoryMonitor.getFreeMb();
        double memPercent = memoryMonitor.getPercent();

        ServerSnapshot snapshot = new ServerSnapshot(
                tpsMonitor.getTps(),
                tpsMonitor.getMspt(),
                plugin.getServer().getOnlinePlayers().size(),
                plugin.getServer().getWorlds().size(),
                usedMb,
                maxMb,
                freeMb,
                memPercent,
                cachedEntities,
                cachedItems,
                cachedOrbs,
                cachedChunks,
                System.currentTimeMillis()
        );

        this.latest = snapshot;

        if (listener != null) {
            try {
                listener.accept(snapshot);
            } catch (Exception ex) {
                plugin.getLogger().warning("性能采集回调执行出错: " + ex.getMessage());
            }
        }
    }

    /**
     * 分批扫描推进（每 tick 调用，主线程）：到达全量扫描间隔时开始新一轮，
     * 每 tick 最多遍历 {@code max-worlds-per-tick} 个世界，扫完一整轮后整体提交缓存。
     */
    private void scanTick() {
        long now = System.currentTimeMillis();
        long fullScanIntervalMs = Math.max(0L, config.getLong("monitor.full-scan-interval-seconds", 60)) * 1000L;

        if (!scanning) {
            if (now - lastFullScanMillis < fullScanIntervalMs) {
                return;
            }
            // 开始新一轮分批扫描
            scanning = true;
            scanCursor = 0;
            accEntities = 0;
            accItems = 0;
            accOrbs = 0;
            accChunks = 0;
        }

        List<World> worlds = plugin.getServer().getWorlds();
        int maxWorlds = Math.max(1, config.getInt("monitor.max-worlds-per-tick", 1));
        int processed = 0;
        while (scanCursor < worlds.size() && processed < maxWorlds) {
            World world = worlds.get(scanCursor);
            accChunks += world.getLoadedChunks().length;
            for (Entity entity : world.getEntities()) {
                accEntities++;
                if (entity instanceof Item) {
                    accItems++;
                } else if (entity instanceof ExperienceOrb) {
                    accOrbs++;
                }
            }
            scanCursor++;
            processed++;
        }

        // 一整轮扫描完成：提交缓存
        if (scanCursor >= worlds.size()) {
            cachedEntities = accEntities;
            cachedItems = accItems;
            cachedOrbs = accOrbs;
            cachedChunks = accChunks;
            scanning = false;
            lastFullScanMillis = now;
        }
    }

    /**
     * 获取最近一次缓存快照。若尚未采集（插件刚启动），做一次轻量采集初始化，
     * 但不触发全量实体扫描（问题 4 要求）。
     */
    public ServerSnapshot getLatest() {
        if (latest == null) {
            collectLight();
        }
        return latest;
    }

    /** 便于日志输出的内存描述。 */
    public String describeMemory() {
        return memoryMonitor.getUsedMb() + "MB / " + memoryMonitor.getMaxMb()
                + "MB (" + FormatUtil.round1(memoryMonitor.getPercent()) + "%)";
    }
}
