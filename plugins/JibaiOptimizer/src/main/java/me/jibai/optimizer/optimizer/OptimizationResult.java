package me.jibai.optimizer.optimizer;

/**
 * 一次优化操作的结果统计。
 */
public class OptimizationResult {

    private final int cleanedItems;
    private final int cleanedOrbs;
    private final int mergedGroups;

    public OptimizationResult(int cleanedItems, int cleanedOrbs, int mergedGroups) {
        this.cleanedItems = cleanedItems;
        this.cleanedOrbs = cleanedOrbs;
        this.mergedGroups = mergedGroups;
    }

    public static OptimizationResult empty() {
        return new OptimizationResult(0, 0, 0);
    }

    public int getCleanedItems() {
        return cleanedItems;
    }

    public int getCleanedOrbs() {
        return cleanedOrbs;
    }

    public int getMergedGroups() {
        return mergedGroups;
    }
}
