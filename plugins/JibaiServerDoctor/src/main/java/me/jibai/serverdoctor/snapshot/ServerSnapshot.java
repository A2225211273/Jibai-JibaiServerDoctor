package me.jibai.serverdoctor.snapshot;

import java.util.Collections;
import java.util.List;

/**
 * 服务器整体健康快照（不可变）。
 *
 * <p>汇总某一时刻的全服信息：在线玩家、世界列表、内存、实体 / 掉落物 / 经验球 / 区块总数，
 * 以及近似 tick 延迟。由 {@link SnapshotService} 在主线程采集生成。</p>
 *
 * @author 即白
 */
public class ServerSnapshot {

    private final long timestamp;
    private final int onlinePlayers;
    private final int worldCount;
    private final List<WorldSnapshot> worlds;

    private final long usedMemoryMb;
    private final long maxMemoryMb;
    private final int memoryPercent;

    private final int totalEntities;
    private final int totalDroppedItems;
    private final int totalExpOrbs;
    private final int totalMonsters;
    private final int totalAnimals;
    private final int totalLoadedChunks;

    private final double approxTickDelayMs;

    public ServerSnapshot(long timestamp, int onlinePlayers, List<WorldSnapshot> worlds,
                          long usedMemoryMb, long maxMemoryMb, int memoryPercent,
                          double approxTickDelayMs) {
        this.timestamp = timestamp;
        this.onlinePlayers = onlinePlayers;
        this.worlds = worlds == null ? Collections.emptyList() : worlds;
        this.worldCount = this.worlds.size();
        this.usedMemoryMb = usedMemoryMb;
        this.maxMemoryMb = maxMemoryMb;
        this.memoryPercent = memoryPercent;
        this.approxTickDelayMs = approxTickDelayMs;

        int entities = 0, items = 0, orbs = 0, monsters = 0, animals = 0, chunks = 0;
        for (WorldSnapshot w : this.worlds) {
            entities += w.getEntities();
            items += w.getDroppedItems();
            orbs += w.getExpOrbs();
            monsters += w.getMonsters();
            animals += w.getAnimals();
            chunks += w.getLoadedChunks();
        }
        this.totalEntities = entities;
        this.totalDroppedItems = items;
        this.totalExpOrbs = orbs;
        this.totalMonsters = monsters;
        this.totalAnimals = animals;
        this.totalLoadedChunks = chunks;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public int getWorldCount() {
        return worldCount;
    }

    public List<WorldSnapshot> getWorlds() {
        return worlds;
    }

    public long getUsedMemoryMb() {
        return usedMemoryMb;
    }

    public long getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public int getMemoryPercent() {
        return memoryPercent;
    }

    public int getTotalEntities() {
        return totalEntities;
    }

    public int getTotalDroppedItems() {
        return totalDroppedItems;
    }

    public int getTotalExpOrbs() {
        return totalExpOrbs;
    }

    public int getTotalMonsters() {
        return totalMonsters;
    }

    public int getTotalAnimals() {
        return totalAnimals;
    }

    public int getTotalLoadedChunks() {
        return totalLoadedChunks;
    }

    public double getApproxTickDelayMs() {
        return approxTickDelayMs;
    }
}
