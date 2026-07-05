package me.jibai.serverdoctor.logs;

import java.util.Collections;
import java.util.List;

/**
 * 日志错误摘要（不可变）。
 *
 * <p>由 {@link LogAnalyzer} 分析 logs/latest.log 后生成，包含：
 * ERROR / Exception 总数、WARN 总数，以及按出现次数聚合的若干条错误条目。</p>
 *
 * @author 即白
 */
public class LogErrorSummary {

    /**
     * 单条聚合后的错误条目。
     */
    public static class Entry {
        private final String pluginName;
        private final String message;
        private final int count;

        public Entry(String pluginName, String message, int count) {
            this.pluginName = pluginName;
            this.message = message;
            this.count = count;
        }

        public String getPluginName() {
            return pluginName;
        }

        public String getMessage() {
            return message;
        }

        public int getCount() {
            return count;
        }
    }

    private final int errorCount;
    private final int warnCount;
    private final List<Entry> entries;
    private final boolean logAvailable;

    public LogErrorSummary(int errorCount, int warnCount, List<Entry> entries, boolean logAvailable) {
        this.errorCount = errorCount;
        this.warnCount = warnCount;
        this.entries = entries == null ? Collections.emptyList() : entries;
        this.logAvailable = logAvailable;
    }

    /** 生成一个“日志不可用”的空摘要。 */
    public static LogErrorSummary unavailable() {
        return new LogErrorSummary(0, 0, Collections.emptyList(), false);
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getWarnCount() {
        return warnCount;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public boolean isLogAvailable() {
        return logAvailable;
    }

    public boolean hasProblems() {
        return errorCount > 0 || warnCount > 0;
    }
}
