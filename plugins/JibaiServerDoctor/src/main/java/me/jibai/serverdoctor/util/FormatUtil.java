package me.jibai.serverdoctor.util;

/**
 * 数值格式化工具。
 *
 * @author 即白
 */
public final class FormatUtil {

    private static final long MB = 1024L * 1024L;

    private FormatUtil() {
    }

    /** 字节转 MB（取整）。 */
    public static long toMegabytes(long bytes) {
        return bytes / MB;
    }

    /**
     * 计算百分比（0-100，取整）。分母为 0 时返回 0，避免除零。
     */
    public static int percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((double) part / (double) total * 100.0);
    }

    /** 给大数字加千位分隔符，便于阅读，如 5860 -> 5,860。 */
    public static String number(long value) {
        return String.format("%,d", value);
    }
}
