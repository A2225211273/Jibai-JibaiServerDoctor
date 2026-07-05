package me.jibai.serverdoctor.redstone;

import me.jibai.serverdoctor.alert.AlertService;
import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.config.MessageManager;
import me.jibai.serverdoctor.snapshot.ChunkRiskSnapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 红石事件追踪器。
 *
 * <p>按区块维护一个时间窗口内的红石事件时间戳队列，实时统计各区块在窗口内的事件次数。
 * 超过阈值即记为风险点，并（可选）通知管理员。<b>只记录和提醒，绝不破坏任何方块。</b></p>
 *
 * <p>{@link RedstoneListener} 在主线程回调 {@link #record}，这里用并发容器保存计数，
 * 以便 /doctor risks、报告等在需要时安全读取。</p>
 *
 * @author 即白
 */
public class RedstoneTracker {

    /** 区块键 -> 最近事件时间戳队列（毫秒）。 */
    private final Map<String, Deque<Long>> chunkEvents = new ConcurrentHashMap<>();

    private final ConfigManager config;
    private final MessageManager messages;
    private final AlertService alertService;

    public RedstoneTracker(ConfigManager config, MessageManager messages, AlertService alertService) {
        this.config = config;
        this.messages = messages;
        this.alertService = alertService;
    }

    /**
     * 记录一次红石事件。由监听器在主线程调用。
     */
    public void record(String worldName, int chunkX, int chunkZ) {
        if (!config.isRedstoneEnabled()) {
            return;
        }
        String key = key(worldName, chunkX, chunkZ);
        long now = System.currentTimeMillis();
        long windowMillis = config.getRedstoneWindowSeconds() * 1000L;

        Deque<Long> queue = chunkEvents.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            queue.addLast(now);
            // 清理窗口外的旧事件
            while (!queue.isEmpty() && now - queue.peekFirst() > windowMillis) {
                queue.pollFirst();
            }
            int count = queue.size();
            if (count >= config.getRedstoneMaxEventsPerChunk() && config.isRedstoneNotifyAdmins()) {
                alertService.alert("redstone:" + key, "alert.redstone-high",
                        MessageManager.ph("chunk", worldName + ", " + chunkX + ", " + chunkZ,
                                "count", String.valueOf(count)));
            }
        }
    }

    /**
     * 获取当前所有超过阈值的红石风险区块（快照）。
     */
    public List<ChunkRiskSnapshot> getCurrentRisks() {
        List<ChunkRiskSnapshot> risks = new ArrayList<>();
        long now = System.currentTimeMillis();
        long windowMillis = config.getRedstoneWindowSeconds() * 1000L;
        int threshold = config.getRedstoneMaxEventsPerChunk();

        for (Map.Entry<String, Deque<Long>> entry : chunkEvents.entrySet()) {
            Deque<Long> queue = entry.getValue();
            int count;
            synchronized (queue) {
                while (!queue.isEmpty() && now - queue.peekFirst() > windowMillis) {
                    queue.pollFirst();
                }
                count = queue.size();
            }
            if (count >= threshold) {
                risks.add(parseKey(entry.getKey(), count));
            }
        }
        risks.sort(Comparator.comparingInt(ChunkRiskSnapshot::getValue).reversed());
        return risks;
    }

    /**
     * 查询指定区块当前窗口内的红石事件次数（用于区块检查）。
     */
    public int getCount(String worldName, int chunkX, int chunkZ) {
        Deque<Long> queue = chunkEvents.get(key(worldName, chunkX, chunkZ));
        if (queue == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long windowMillis = config.getRedstoneWindowSeconds() * 1000L;
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > windowMillis) {
                queue.pollFirst();
            }
            return queue.size();
        }
    }

    /** 清空统计（重载时调用）。 */
    public void reset() {
        chunkEvents.clear();
    }

    private String key(String worldName, int chunkX, int chunkZ) {
        return worldName + ";" + chunkX + ";" + chunkZ;
    }

    private ChunkRiskSnapshot parseKey(String key, int count) {
        String[] parts = key.split(";");
        String world = parts.length > 0 ? parts[0] : "unknown";
        int x = parts.length > 1 ? safeInt(parts[1]) : 0;
        int z = parts.length > 2 ? safeInt(parts[2]) : 0;
        return new ChunkRiskSnapshot(world, x, z, count, "redstone");
    }

    private int safeInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
