package me.jibai.optimizer.util;

/**
 * 数值格式化工具。
 */
public final class FormatUtil {

    private FormatUtil() {
    }

    /**
     * 保留两位小数。
     */
    public static String round2(double value) {
        return String.format("%.2f", value);
    }

    /**
     * 保留一位小数。
     */
    public static String round1(double value) {
        return String.format("%.1f", value);
    }

    /**
     * 字节转 MB（整数）。
     */
    public static long toMb(long bytes) {
        return bytes / (1024L * 1024L);
    }

    /**
     * 计算百分比（0-100），保留一位小数。分母为 0 时返回 0。
     */
    public static double percent(double used, double total) {
        if (total <= 0) {
            return 0.0;
        }
        return used / total * 100.0;
    }
}
