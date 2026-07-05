package me.jibai.serverdoctor.hopper;

/**
 * 漏斗风险记录（不可变）。
 *
 * <p>表示某个区块的漏斗数量超过阈值，仅用于记录和提醒，插件绝不破坏漏斗。</p>
 *
 * @author 即白
 */
public class HopperRisk {

    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final int hopperCount;

    public HopperRisk(String worldName, int chunkX, int chunkZ, int hopperCount) {
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.hopperCount = hopperCount;
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

    public int getHopperCount() {
        return hopperCount;
    }

    /** 便于展示的坐标串，如 world, 12, -8。 */
    public String coordinateLabel() {
        return worldName + ", " + chunkX + ", " + chunkZ;
    }
}
