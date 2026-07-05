package me.jibai.serverdoctor.snapshot;

/**
 * 区块风险快照（不可变）。
 *
 * <p>用于描述某个区块的红石 / 漏斗风险数据，供风险分析和告警使用。
 * 区块坐标用世界名 + 区块 X/Z 表示。</p>
 *
 * @author 即白
 */
public class ChunkRiskSnapshot {

    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final int value;
    private final String type;

    /**
     * @param worldName 世界名
     * @param chunkX    区块 X 坐标
     * @param chunkZ    区块 Z 坐标
     * @param value     风险数值（红石事件次数或漏斗数量）
     * @param type      风险类型，如 "redstone" 或 "hopper"
     */
    public ChunkRiskSnapshot(String worldName, int chunkX, int chunkZ, int value, String type) {
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.value = value;
        this.type = type;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    /** 便于展示的坐标串，如 world, 120, -44。 */
    public String coordinateLabel() {
        return worldName + ", " + chunkX + ", " + chunkZ;
    }
}
