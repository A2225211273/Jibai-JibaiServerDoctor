package me.jibai.serverdoctor.inspect;

import me.jibai.serverdoctor.health.RiskLevel;

/**
 * 区块 / 玩家附近检查的结果（不可变）。
 *
 * <p>汇总一处区域的实体分类统计、漏斗数量、红石事件频率、活跃区块数，以及综合风险评级，
 * 供指令层渲染成聊天消息。</p>
 *
 * @author 即白
 */
public class InspectResult {

    private final String worldName;
    private final int centerX;
    private final int centerZ;
    private final int entities;
    private final int droppedItems;
    private final int expOrbs;
    private final int monsters;
    private final int animals;
    private final int hoppers;
    private final int redstoneCount;
    private final int activeChunks;
    private final RiskLevel level;

    public InspectResult(String worldName, int centerX, int centerZ, int entities, int droppedItems,
                         int expOrbs, int monsters, int animals, int hoppers, int redstoneCount,
                         int activeChunks, RiskLevel level) {
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.entities = entities;
        this.droppedItems = droppedItems;
        this.expOrbs = expOrbs;
        this.monsters = monsters;
        this.animals = animals;
        this.hoppers = hoppers;
        this.redstoneCount = redstoneCount;
        this.activeChunks = activeChunks;
        this.level = level;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public int getEntities() {
        return entities;
    }

    public int getDroppedItems() {
        return droppedItems;
    }

    public int getExpOrbs() {
        return expOrbs;
    }

    public int getMonsters() {
        return monsters;
    }

    public int getAnimals() {
        return animals;
    }

    public int getHoppers() {
        return hoppers;
    }

    public int getRedstoneCount() {
        return redstoneCount;
    }

    public int getActiveChunks() {
        return activeChunks;
    }

    public RiskLevel getLevel() {
        return level;
    }
}
