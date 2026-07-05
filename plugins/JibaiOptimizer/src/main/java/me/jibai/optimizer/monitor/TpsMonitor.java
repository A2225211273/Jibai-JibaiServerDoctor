package me.jibai.optimizer.monitor;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * TPS / MSPT 近似采集器（全核心兼容实现）。
 *
 * <p>Bukkit / Spigot 没有稳定通用的 TPS API（{@code Server#getTPS()} 与
 * {@code getAverageTickTime()} 属于 Paper/Spigot 扩展），为兼容 Bukkit / Spigot / Paper / Purpur，
 * 这里改用 {@link org.bukkit.scheduler.BukkitScheduler} 每 tick 采样：记录相邻两次 tick 的时间间隔，
 * 在滑动窗口内取平均，据此计算近似 TPS 与近似每 tick 耗时。不使用 NMS、反射或 CraftBukkit 内部类。</p>
 *
 * <p>正常情况下每 tick 间隔约 50ms（20 TPS）。间隔越大说明服务器越卡，TPS 越低。</p>
 *
 * @author 即白
 */
public class TpsMonitor {

    /** 采样窗口大小：最近 100 个 tick（约 5 秒）。 */
    private static final int SAMPLE_SIZE = 100;

    /** 理想每 tick 间隔（毫秒）。 */
    private static final double IDEAL_INTERVAL_MS = 50.0;

    private final JavaPlugin plugin;

    /** 环形缓冲，保存最近若干次 tick 的间隔（纳秒）。 */
    private final long[] intervalsNano = new long[SAMPLE_SIZE];
    private int index = 0;
    private int filled = 0;
    private long lastNano = 0L;

    private BukkitTask task;

    public TpsMonitor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 启动每 tick 采样任务（主线程）。 */
    public void start() {
        this.lastNano = System.nanoTime();
        this.index = 0;
        this.filled = 0;
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sample, 1L, 1L);
    }

    /** 停止采样任务。 */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** 每 tick 记录一次间隔。 */
    private void sample() {
        long now = System.nanoTime();
        long delta = now - lastNano;
        lastNano = now;
        intervalsNano[index] = delta;
        index = (index + 1) % SAMPLE_SIZE;
        if (filled < SAMPLE_SIZE) {
            filled++;
        }
    }

    /** 最近窗口的平均 tick 间隔（毫秒）。尚无样本时返回理想值 50ms。 */
    public double getAvgIntervalMs() {
        if (filled == 0) {
            return IDEAL_INTERVAL_MS;
        }
        long sum = 0L;
        for (int i = 0; i < filled; i++) {
            sum += intervalsNano[i];
        }
        return (sum / (double) filled) / 1_000_000.0;
    }

    /** 近似 TPS，上限 20。 */
    public double getTps() {
        double interval = getAvgIntervalMs();
        if (interval <= 0) {
            return 20.0;
        }
        return Math.min(20.0, 1000.0 / interval);
    }

    /**
     * 近似每 tick 耗时（毫秒）。
     *
     * <p>注意：不使用 NMS 无法测得服务端真实的单 tick 处理耗时，这里用相邻 tick 的平均间隔近似。
     * 正常约 50ms，数值越大表示服务器越卡。</p>
     */
    public double getMspt() {
        return getAvgIntervalMs();
    }
}
