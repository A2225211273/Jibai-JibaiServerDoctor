package me.jibai.optimizer.redstone;

import me.jibai.optimizer.util.TimeUtil;
import org.bukkit.Chunk;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 红石事件统计器。按区块在滑动时间窗口内统计红石事件次数，并定期清理过期数据，
 * 避免统计 Map 无限增长。
 */
public class RedstoneTracker {

    /** 单个区块的统计窗口。 */
    private static class EventWindow {
        final String label;
        long windowStart;
        int count;

        EventWindow(String label, long windowStart) {
            this.label = label;
            this.windowStart = windowStart;
        }
    }

    private final Map<String, EventWindow> windows = new LinkedHashMap<>();
    private long windowMs;

    public RedstoneTracker(long windowSeconds) {
        this.windowMs = windowSeconds * 1000L;
    }

    /** 重载后更新窗口长度。 */
    public void setWindowSeconds(long windowSeconds) {
        this.windowMs = windowSeconds * 1000L;
    }

    /**
     * 记录一次某区块的红石事件，返回该区块在当前窗口内的累计次数。
     */
    public int record(Chunk chunk) {
        String key = TimeUtil.chunkKey(chunk);
        long now = System.currentTimeMillis();
        EventWindow window = windows.get(key);
        if (window == null || now - window.windowStart >= windowMs) {
            window = new EventWindow(TimeUtil.chunkLabel(chunk), now);
            windows.put(key, window);
        }
        window.count++;
        return window.count;
    }

    /**
     * 返回当前未过期窗口的统计快照：可读区块标识 -> 次数。
     */
    public Map<String, Integer> snapshot() {
        long now = System.currentTimeMillis();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (EventWindow window : windows.values()) {
            if (now - window.windowStart < windowMs) {
                result.put(window.label, window.count);
            }
        }
        return result;
    }

    /**
     * 清理过期窗口数据（超过窗口长度两倍视为过期）。
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, EventWindow>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            EventWindow window = it.next().getValue();
            if (now - window.windowStart >= windowMs * 2) {
                it.remove();
            }
        }
    }

    /** 清空全部统计（重载时使用）。 */
    public void clear() {
        windows.clear();
    }
}
