package me.jibai.optimizer.redstone;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

/**
 * 红石高频检测监听器。监听 {@link BlockRedstoneEvent}，把统计与动作处理委托给
 * {@link RedstoneLimitService}。
 */
public class RedstoneMonitorListener implements Listener {

    private final RedstoneLimitService limitService;

    public RedstoneMonitorListener(RedstoneLimitService limitService) {
        this.limitService = limitService;
    }

    // 使用 HIGH 优先级，因为 cancel/break 动作可能修改红石状态
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        limitService.handle(event);
    }
}
