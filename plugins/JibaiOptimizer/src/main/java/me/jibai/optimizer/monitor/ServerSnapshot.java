package me.jibai.optimizer.monitor;

/**
 * 服务器状态快照，不可变数据对象。由 {@link PerformanceMonitor} 定时生成。
 */
public class ServerSnapshot {

    private final double tps;
    private final double mspt;
    private final int onlinePlayers;
    private final int worldCount;
    private final long usedMemoryMb;
    private final long maxMemoryMb;
    private final long freeMemoryMb;
    private final double memoryPercent;
    private final int totalEntities;
    private final int totalItems;
    private final int totalExpOrbs;
    private final int loadedChunks;
    private final long timestamp;

    public ServerSnapshot(double tps, double mspt, int onlinePlayers, int worldCount,
                          long usedMemoryMb, long maxMemoryMb, long freeMemoryMb, double memoryPercent,
                          int totalEntities, int totalItems, int totalExpOrbs, int loadedChunks,
                          long timestamp) {
        this.tps = tps;
        this.mspt = mspt;
        this.onlinePlayers = onlinePlayers;
        this.worldCount = worldCount;
        this.usedMemoryMb = usedMemoryMb;
        this.maxMemoryMb = maxMemoryMb;
        this.freeMemoryMb = freeMemoryMb;
        this.memoryPercent = memoryPercent;
        this.totalEntities = totalEntities;
        this.totalItems = totalItems;
        this.totalExpOrbs = totalExpOrbs;
        this.loadedChunks = loadedChunks;
        this.timestamp = timestamp;
    }

    public double getTps() {
        return tps;
    }

    public double getMspt() {
        return mspt;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public int getWorldCount() {
        return worldCount;
    }

    public long getUsedMemoryMb() {
        return usedMemoryMb;
    }

    public long getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public long getFreeMemoryMb() {
        return freeMemoryMb;
    }

    public double getMemoryPercent() {
        return memoryPercent;
    }

    public int getTotalEntities() {
        return totalEntities;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalExpOrbs() {
        return totalExpOrbs;
    }

    public int getLoadedChunks() {
        return loadedChunks;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
