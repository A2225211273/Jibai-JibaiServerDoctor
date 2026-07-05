package me.jibai.optimizer.redstone;

import me.jibai.optimizer.JibaiOptimizerPlugin;
import me.jibai.optimizer.alert.AlertService;
import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.config.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 红石限制服务。基于 {@link RedstoneTracker} 的统计判断区块是否红石高频，并按配置动作处理：
 * warn（仅提醒）/ cancel（尝试取消红石变化）/ break（破坏红石元件）。
 *
 * <p><b>安全约束（问题 6 修复）：</b>break 是危险动作，默认关闭。即使 {@code action=break}，
 * 也必须同时满足 {@code break-blocks.enabled=true} 才会真正破坏，且只允许破坏材质白名单中的方块
 * （默认仅红石线、中继器、比较器、侦测器）。任何破坏都会记录日志并提醒管理员，绝不破坏白名单之外的方块。</p>
 *
 * <p>同时负责定期清理统计数据，避免 Map 无限增长。</p>
 *
 * @author 即白
 */
public class RedstoneLimitService {

    /** break-blocks.materials 缺省时的安全白名单。 */
    private static final List<String> DEFAULT_BREAK_WHITELIST = Arrays.asList(
            "REDSTONE_WIRE", "REPEATER", "COMPARATOR", "OBSERVER");

    private final JibaiOptimizerPlugin plugin;
    private final ConfigManager config;
    private final AlertService alert;
    private final RedstoneTracker tracker;

    private BukkitTask cleanupTask;

    public RedstoneLimitService(JibaiOptimizerPlugin plugin, ConfigManager config, AlertService alert) {
        this.plugin = plugin;
        this.config = config;
        this.alert = alert;
        long window = config.getLong("redstone-monitor.check-window-seconds", 10);
        this.tracker = new RedstoneTracker(window);
    }

    public RedstoneTracker getTracker() {
        return tracker;
    }

    /**
     * 启动定期清理任务（每分钟清理一次过期窗口）。
     */
    public void start() {
        tracker.setWindowSeconds(config.getLong("redstone-monitor.check-window-seconds", 10));
        this.cleanupTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, tracker::cleanup, 20L * 60, 20L * 60);
    }

    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        tracker.clear();
    }

    /**
     * 处理一次红石事件（在主线程、事件线程调用）。
     */
    public void handle(BlockRedstoneEvent event) {
        if (!config.getBool("redstone-monitor.enabled", true)) {
            return;
        }
        Chunk chunk = event.getBlock().getChunk();
        int count = tracker.record(chunk);
        int max = config.getInt("redstone-monitor.max-events-per-chunk", 300);
        if (count <= max) {
            return;
        }

        String action = config.getString("redstone-monitor.action", "warn");
        String label = me.jibai.optimizer.util.TimeUtil.chunkLabel(chunk);

        // 告警（限流，冷却时间取窗口长度）
        long windowSeconds = config.getLong("redstone-monitor.check-window-seconds", 10);
        alert.alertMessageThrottled("redstone-" + label, windowSeconds, "redstone.warning",
                MessageManager.ph("chunk", label, "count", String.valueOf(count)));

        // 执行动作
        if ("cancel".equalsIgnoreCase(action)) {
            // 尝试取消红石变化：把新电流恢复为旧电流（不破坏方块，安全）
            event.setNewCurrent(event.getOldCurrent());
        } else if ("break".equalsIgnoreCase(action)) {
            // break 为危险动作：必须显式开启 break-blocks.enabled，且只破坏白名单材质
            tryBreakBlock(event.getBlock(), label);
        }
        // warn：仅告警，不改变红石行为
    }

    /**
     * 在满足安全条件时破坏触发红石变化的方块。
     *
     * <p>双重开关：{@code redstone-monitor.action=break} 且 {@code redstone-monitor.break-blocks.enabled=true}；
     * 且方块材质必须在 {@code break-blocks.materials} 白名单内。任何其他情况一律不破坏。</p>
     */
    private void tryBreakBlock(Block block, String label) {
        if (!config.getBool("redstone-monitor.break-blocks.enabled", false)) {
            // 未显式开启破坏开关：绝不破坏，仅前面已告警
            return;
        }
        Material type = block.getType();
        if (!isBreakAllowed(type)) {
            // 材质不在白名单：拒绝破坏，避免误伤玩家建筑
            return;
        }
        block.setType(Material.AIR);

        // 破坏行为必须记录日志并提醒管理员
        plugin.getLogger().warning("[红石保护] 已破坏高频红石方块 " + type + " @ " + label
                + "（action=break 且材质在白名单内）");
        alert.alertMessage("redstone.broken", MessageManager.ph(
                "chunk", label, "material", type.name()));
    }

    /**
     * 判断材质是否允许被 break。读取 {@code break-blocks.materials}，缺省用安全白名单。
     * 材质名无效时忽略，绝不因配置错误而破坏计划外方块。
     */
    private boolean isBreakAllowed(Material type) {
        List<String> configured = config.raw().getStringList("redstone-monitor.break-blocks.materials");
        List<String> names = (configured == null || configured.isEmpty()) ? DEFAULT_BREAK_WHITELIST : configured;
        Set<Material> whitelist = new HashSet<>();
        for (String name : names) {
            if (name == null) {
                continue;
            }
            Material m = Material.matchMaterial(name.trim().toUpperCase());
            if (m != null) {
                whitelist.add(m);
            }
        }
        return whitelist.contains(type);
    }
}
