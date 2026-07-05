package me.jibai.serverdoctor.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间格式化工具。
 *
 * <p>提供文件名安全的时间戳（用于报告 / 诊断包命名）和可读时间（用于报告正文）。</p>
 *
 * @author 即白
 */
public final class TimeUtil {

    /** 用于文件名，形如 2026-07-06-14-30-15，不含冒号，避免 Windows 文件名非法。 */
    private static final String FILE_PATTERN = "yyyy-MM-dd-HH-mm-ss";

    /** 用于展示，形如 2026-07-06 14:30:15。 */
    private static final String DISPLAY_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private TimeUtil() {
    }

    /** 当前时间的文件名安全时间戳。 */
    public static String fileTimestamp() {
        return new SimpleDateFormat(FILE_PATTERN).format(new Date());
    }

    /** 当前时间的可读字符串。 */
    public static String displayNow() {
        return new SimpleDateFormat(DISPLAY_PATTERN).format(new Date());
    }

    /** 把毫秒时间戳格式化为可读字符串。 */
    public static String display(long millis) {
        return new SimpleDateFormat(DISPLAY_PATTERN).format(new Date(millis));
    }
}
