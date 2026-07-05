package me.jibai.optimizer.util;

import org.bukkit.Chunk;

/**
 * 位置 / 区块相关工具。
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * 秒转 tick（1 秒 = 20 tick）。
     */
    public static long secondsToTicks(long seconds) {
        return seconds * 20L;
    }

    /**
     * 返回区块的可读标识，如 {@code world[10, -3]}。
     */
    public static String chunkLabel(Chunk chunk) {
        return chunk.getWorld().getName() + "[" + chunk.getX() + ", " + chunk.getZ() + "]";
    }

    /**
     * 由世界名 + 区块坐标生成唯一 key，用于 Map 统计。
     */
    public static String chunkKey(String worldName, int x, int z) {
        return worldName + ":" + x + ":" + z;
    }

    /**
     * 由 {@link Chunk} 生成唯一 key。
     */
    public static String chunkKey(Chunk chunk) {
        return chunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
}
