package me.jibai.serverdoctor.hopper;

import me.jibai.serverdoctor.alert.AlertService;
import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.config.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 漏斗数量扫描器。
 *
 * <p>按配置间隔（默认 300 秒）在主线程遍历所有已加载区块，统计每个区块内的漏斗数量。
 * 超过阈值的区块记为风险点，并（可选）通知管理员。<b>只记录和提醒，绝不破坏漏斗。</b></p>
 *
 * <p>为避免每秒扫描带来的开销，扫描间隔在 {@link ConfigManager} 中已强制不低于 30 秒。</p>
 *
 * @author 即白
 */
public class HopperScanner {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    private final AlertService alertService;

    /** 最近一次扫描得到的漏斗风险列表。用并发容器以便指令线程安全读取。 */
    private final List<HopperRisk> latestRisks = new CopyOnWriteArrayList<>();

    private BukkitTask scanTask;

    public HopperScanner(JavaPlugin plugin, ConfigManager config,
                         MessageManager messages, AlertService alertService) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.alertService = alertService;
    }

    /** 启动定时扫描任务。 */
    public void start() {
        if (!config.isHopperEnabled()) {
            return;
        }
        long intervalTicks = config.getHopperScanIntervalSeconds() * 20L;
        this.scanTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::scan, intervalTicks, intervalTicks);
    }

    /** 停止扫描任务。 */
    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    /**
     * 立即执行一次扫描。必须在主线程调用（会访问区块方块实体）。
     * 供 /doctor inspect 等需要即时数据的场景复用。
     */
    public void scan() {
        List<HopperRisk> found = new ArrayList<>();
        int threshold = config.getHopperMaxPerChunk();

        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                int hoppers = countHoppers(chunk);
                if (hoppers >= threshold) {
                    HopperRisk risk = new HopperRisk(world.getName(), chunk.getX(), chunk.getZ(), hoppers);
                    found.add(risk);
                    if (config.isHopperNotifyAdmins()) {
                        alertService.alert("hopper:" + world.getName() + ";" + chunk.getX() + ";" + chunk.getZ(),
                                "alert.hopper-high",
                                MessageManager.ph("chunk", risk.coordinateLabel(),
                                        "count", String.valueOf(hoppers)));
                    }
                }
            }
        }
        found.sort(Comparator.comparingInt(HopperRisk::getHopperCount).reversed());

        latestRisks.clear();
        latestRisks.addAll(found);
    }

    /** 统计单个区块内的漏斗数量。必须在主线程调用。 */
    public int countHoppers(Chunk chunk) {
        int count = 0;
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Hopper) {
                count++;
            }
        }
        return count;
    }

    /** 获取最近一次扫描的漏斗风险列表（只读副本）。 */
    public List<HopperRisk> getLatestRisks() {
        return new ArrayList<>(latestRisks);
    }

    /** 清空结果（重载时调用）。 */
    public void reset() {
        latestRisks.clear();
    }
}
