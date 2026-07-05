package me.jibai.serverdoctor.snapshot;

/**
 * 单个世界的快照数据（不可变）。
 *
 * <p>记录某一时刻某个世界的实体、掉落物、经验球、怪物、动物和加载区块数量。</p>
 *
 * @author 即白
 */
public class WorldSnapshot {

    private final String worldName;
    private final int entities;
    private final int droppedItems;
    private final int expOrbs;
    private final int monsters;
    private final int animals;
    private final int loadedChunks;

    public WorldSnapshot(String worldName, int entities, int droppedItems, int expOrbs,
                         int monsters, int animals, int loadedChunks) {
        this.worldName = worldName;
        this.entities = entities;
        this.droppedItems = droppedItems;
        this.expOrbs = expOrbs;
        this.monsters = monsters;
        this.animals = animals;
        this.loadedChunks = loadedChunks;
    }

    public String getWorldName() {
        return worldName;
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

    public int getLoadedChunks() {
        return loadedChunks;
    }
}
