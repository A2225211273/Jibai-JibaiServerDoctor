package me.jibai.optimizer.entity;

import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 掉落物优化器。
 *
 * <p>功能：清理过旧掉落物、保护玩家附近掉落物、保护带自定义名称 / 特殊 meta 的物品、
 * 按区块合并附近相同物品。所有操作均遍历已加载实体，必须在主线程执行。</p>
 *
 * <p><b>安全保证：</b></p>
 * <ul>
 *   <li>合并<strong>绝不吞物品</strong>：严格遵守 {@link ItemStack#getMaxStackSize()}，
 *       超出单堆上限的部分会保留在其它掉落物实体上，合并前后总数量完全一致。</li>
 *   <li>合并按<strong>区块分组</strong>处理，不做全世界 O(n²) 两两比较，避免物品多时卡服。</li>
 *   <li>每次运行有处理上限（{@code max-items-per-run} / {@code max-chunks-per-run}），
 *       自动优化触发时只做轻量合并，不做全服大规模合并。</li>
 *   <li>带自定义名、附魔、Lore 等特殊 meta 的物品，以及玩家附近的掉落物，始终受保护，不参与合并/清理。</li>
 * </ul>
 *
 * @author 即白
 */
public class ItemOptimizer {

    private final ConfigManager config;

    public ItemOptimizer(ConfigManager config) {
        this.config = config;
    }

    /**
     * 清理过旧且不受保护的掉落物。
     *
     * @return 被清理的掉落物数量
     */
    public int cleanupOldItems() {
        if (!config.getBool("item-optimizer.enabled", true)) {
            return 0;
        }
        long ageTicks = config.getLong("item-optimizer.cleanup-age-seconds", 180) * 20L;
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.getTicksLived() < ageTicks) {
                    continue;
                }
                if (isProtected(item)) {
                    continue;
                }
                item.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * 合并附近相同类型的掉落物（完整合并，受每次运行上限约束）。
     *
     * @return 发生合并的组数（每组表示至少两个掉落物被合并）
     */
    public int mergeNearbyItems() {
        int maxItems = config.getInt("item-optimizer.merge-nearby-items.max-items-per-run", 3000);
        int maxChunks = config.getInt("item-optimizer.merge-nearby-items.max-chunks-per-run", 200);
        return mergeNearbyItems(maxItems, maxChunks);
    }

    /**
     * 自动优化触发时的轻量合并：只处理少量区块和物品，避免在卡顿时雪上加霜。
     *
     * @return 发生合并的组数
     */
    public int mergeNearbyItemsLight() {
        int maxItems = config.getInt("item-optimizer.merge-nearby-items.auto-max-items-per-run", 800);
        int maxChunks = config.getInt("item-optimizer.merge-nearby-items.auto-max-chunks-per-run", 40);
        return mergeNearbyItems(maxItems, maxChunks);
    }

    /**
     * 按区块分组合并掉落物。
     *
     * <p>算法：先按区块把掉落物分组（避免全世界两两比较的 O(n²)）；每个区块内仅对同一区块的相同物品，
     * 在合并半径内进行合并。合并时严格遵守最大堆叠上限，超出部分保留在原有的其它掉落物实体上，
     * 保证总数量不变、绝不删除物品。</p>
     *
     * @param maxItems  本次最多处理的掉落物数量
     * @param maxChunks 本次最多处理的区块数量
     * @return 发生合并的组数
     */
    private int mergeNearbyItems(int maxItems, int maxChunks) {
        if (!config.getBool("item-optimizer.merge-nearby-items.enabled", true)) {
            return 0;
        }
        double radius = config.getDouble("item-optimizer.merge-nearby-items.radius", 3);
        double radiusSq = radius * radius;

        int mergedGroups = 0;
        int processedItems = 0;
        int processedChunks = 0;

        outer:
        for (World world : Bukkit.getWorlds()) {
            // 1. 按区块分组，避免跨全世界两两比较
            Map<String, List<Item>> byChunk = new LinkedHashMap<>();
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.isDead() || isProtected(item)) {
                    continue;
                }
                String key = TimeUtil.chunkKey(item.getLocation().getChunk());
                byChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }

            // 2. 逐区块合并
            for (List<Item> items : byChunk.values()) {
                if (processedChunks >= maxChunks) {
                    break outer;
                }
                processedChunks++;

                for (int i = 0; i < items.size(); i++) {
                    if (processedItems >= maxItems) {
                        break outer;
                    }
                    Item base = items.get(i);
                    processedItems++;
                    if (base.isDead()) {
                        continue;
                    }
                    ItemStack baseStack = base.getItemStack();
                    int maxStack = Math.max(1, baseStack.getMaxStackSize());
                    // 已满堆则无需再合并到它
                    boolean mergedAny = false;

                    for (int j = i + 1; j < items.size(); j++) {
                        Item other = items.get(j);
                        if (other.isDead()) {
                            continue;
                        }
                        ItemStack otherStack = other.getItemStack();
                        if (!baseStack.isSimilar(otherStack)) {
                            continue;
                        }
                        if (base.getLocation().distanceSquared(other.getLocation()) > radiusSq) {
                            continue;
                        }
                        int space = maxStack - baseStack.getAmount();
                        if (space <= 0) {
                            // base 已满堆，停止往它合并（其余留给后续 base）
                            break;
                        }
                        int move = Math.min(space, otherStack.getAmount());
                        // 把 move 个从 other 转移到 base，绝不丢弃
                        baseStack.setAmount(baseStack.getAmount() + move);
                        int remaining = otherStack.getAmount() - move;
                        if (remaining <= 0) {
                            other.remove();
                        } else {
                            otherStack.setAmount(remaining);
                            other.setItemStack(otherStack);
                        }
                        mergedAny = true;
                    }

                    if (mergedAny) {
                        base.setItemStack(baseStack);
                        mergedGroups++;
                    }
                }
            }
        }
        return mergedGroups;
    }

    /**
     * 判断掉落物是否受保护：带自定义名 / 附魔 / Lore 等特殊 meta，或玩家附近。
     */
    private boolean isProtected(Item item) {
        ItemStack stack = item.getItemStack();
        ItemMeta meta = stack.getItemMeta();
        // 带自定义显示名的物品受保护
        if (meta != null && meta.hasDisplayName()) {
            return true;
        }
        // 带附魔或 Lore 等特殊 meta 的物品受保护，避免合并破坏差异化物品
        if (meta != null && (meta.hasEnchants() || meta.hasLore() || stack.getEnchantments().size() > 0)) {
            return true;
        }
        // 实体本身设置了自定义名（Bukkit API，兼容全核心）
        if (item.getCustomName() != null) {
            return true;
        }
        // 玩家附近的掉落物受保护
        if (config.getBool("item-optimizer.protect-near-player.enabled", true)) {
            double radius = config.getDouble("item-optimizer.protect-near-player.radius", 8);
            double radiusSq = radius * radius;
            Location loc = item.getLocation();
            for (Player player : item.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(loc) <= radiusSq) {
                    return true;
                }
            }
        }
        return false;
    }
}
