package me.jibai.serverdoctor.redstone;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

/**
 * 红石事件监听器。
 *
 * <p>监听 {@link BlockRedstoneEvent}，把事件按世界 + 区块转交给 {@link RedstoneTracker} 统计。
 * 使用 {@link EventPriority#MONITOR} 且不修改事件，保证只观察不干预——不会改变红石行为、不破坏方块。</p>
 *
 * @author 即白
 */
public class RedstoneListener implements Listener {

    private final RedstoneTracker tracker;

    public RedstoneListener(RedstoneTracker tracker) {
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        // 区块坐标 = 方块坐标 >> 4
        int chunkX = block.getX() >> 4;
        int chunkZ = block.getZ() >> 4;
        tracker.record(block.getWorld().getName(), chunkX, chunkZ);
    }
}
