package me.jibai.optimizer.chunk;

import me.jibai.optimizer.JibaiOptimizerPlugin;
import me.jibai.optimizer.alert.AlertService;
import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.config.MessageManager;
import me.jibai.optimizer.util.TimeUtil;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 漏斗监控器。分批扫描已加载区块中的漏斗数量，超过阈值时提醒管理员。
 *
 * <p>为避免一次性遍历所有已加载区块的方块实体造成卡顿，扫描按批进行：每个扫描 tick
 * 只处理有限数量的区块（{@code hopper-monitor.max-chunks-per-scan-tick}），一轮扫描完成后
 * 缓存结果。{@code /optimizer hoppers} 指令只读取最近一次缓存结果，不触发即时全量扫描。</p>
 *
 * <p>默认只警告（{@code hopper-monitor.action=warn}），不破坏漏斗。</p>
 *
 * @author 即白
 */
public class HopperMonitor {

    private final JibaiOptimizerPlugin plugin;
    private final ConfigManager config;
    private final AlertService alert;

    /** 分批扫描任务：每 tick 处理有限数量区块。 */
    private BukkitTask scanTask;

    /** 当前正在扫描的区块队列（按批消费）。 */
    private final List<Chunk> pending = new ArrayList<>();

    /** 本轮扫描累计发现的超限区块。 */
    private List<ChunkHopperCount> currentRoundOver = new ArrayList<>();

    /** 最近一次完成扫描的缓存结果与时间戳。 */
    private volatile List<ChunkHopperCount> cachedOver = new ArrayList<>();
    private volatile long cacheTimeMillis = 0L;

    /** 距离上次一轮扫描开始的计时（tick 累加）。 */
    private long ticksSinceRoundStart = 0L;

    public HopperMonitor(JibaiOptimizerPlugin plugin, ConfigManager config, AlertService alert) {
        this.plugin = plugin;
        this.config = config;
        this.alert = alert;
    }

    /** 单个区块的漏斗统计。 */
    public static class ChunkHopperCount {
        public final String label;
        public final int count;

        public ChunkHopperCount(String label, int count) {
            this.label = label;
            this.count = count;
        }
    }

    /**
     * 启动分批扫描任务。任务每 tick 触发一次，但只在需要时处理有限数量区块。
     */
    public void start() {
        if (!config.getBool("hopper-monitor.enabled", true)) {
            return;
        }
        this.ticksSinceRoundStart = 0L;
        this.scanTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
    }

    /**
     * 停止扫描任务并清理状态。
     */
    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        pending.clear();
        currentRoundOver = new ArrayList<>();
    }

    /**
     * 每 tick 调用：控制扫描节奏并分批处理区块（必须在主线程执行）。
     */
    private void tick() {
        long intervalTicks = Math.max(20L, config.getLong("hopper-monitor.scan-interval-seconds", 300) * 20L);
        ticksSinceRoundStart++;

        // 队列为空：判断是否到达下一轮扫描时间，到了就装载所有已加载区块作为本轮扫描目标
        if (pending.isEmpty()) {
            if (ticksSinceRoundStart < intervalTicks) {
                return;
            }
            beginNewRound();
            return;
        }

        // 队列非空：本轮扫描进行中，按批处理
        int perTick = Math.max(1, config.getInt("hopper-monitor.max-chunks-per-scan-tick", 100));
        int max = config.getInt("hopper-monitor.max-hoppers-per-chunk", 64);
        int processed = 0;
        while (!pending.isEmpty() && processed < perTick) {
            Chunk chunk = pending.remove(pending.size() - 1);
            processed++;
            if (!chunk.isLoaded()) {
                continue;
            }
            int count = countHoppers(chunk);
            if (count > max) {
                currentRoundOver.add(new ChunkHopperCount(TimeUtil.chunkLabel(chunk), count));
            }
        }

        // 本轮处理完毕：更新缓存并告警
        if (pending.isEmpty()) {
            finishRound();
        }
    }

    /** 开始新一轮扫描：装载当前所有已加载区块进待处理队列。 */
    private void beginNewRound() {
        ticksSinceRoundStart = 0L;
        currentRoundOver = new ArrayList<>();
        pending.clear();
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                pending.add(chunk);
            }
        }
    }

    /** 结束本轮扫描：写入缓存并对超限区块告警。 */
    private void finishRound() {
        this.cachedOver = currentRoundOver;
        this.cacheTimeMillis = System.currentTimeMillis();
        this.currentRoundOver = new ArrayList<>();

        for (ChunkHopperCount item : cachedOver) {
            alert.alertMessageThrottled("hopper-" + item.label, 300, "hopper.warning",
                    MessageManager.ph("chunk", item.label, "count", String.valueOf(item.count)));
        }
    }

    /** 统计单个区块内的漏斗数量。 */
    private int countHoppers(Chunk chunk) {
        int count = 0;
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Hopper) {
                count++;
            }
        }
        return count;
    }

    /**
     * 返回最近一次扫描缓存的超限区块列表（供 /optimizer hoppers 使用，不触发扫描）。
     * 若缓存已过期（超过 {@code hopper-monitor.cache-expire-seconds}），仍返回旧数据但由调用方决定提示。
     */
    public List<ChunkHopperCount> getCachedOver() {
        return cachedOver;
    }

    /** 缓存是否为空（从未完成过一轮扫描）。 */
    public boolean hasCache() {
        return cacheTimeMillis > 0L;
    }

    /** 缓存是否已过期。 */
    public boolean isCacheExpired() {
        long expireSeconds = config.getLong("hopper-monitor.cache-expire-seconds", 600);
        if (expireSeconds <= 0) {
            return false;
        }
        return System.currentTimeMillis() - cacheTimeMillis > expireSeconds * 1000L;
    }
}
