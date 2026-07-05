package me.jibai.serverdoctor.logs;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.util.FileUtil;
import me.jibai.serverdoctor.util.SafeTextFilter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志错误分析器。
 *
 * <p>读取 {@code logs/latest.log} 的末尾若干行（默认最多 5000 行，避免读取超大日志导致卡顿），
 * 统计 ERROR / Exception / WARN 数量，并把相似的错误行按“插件名 + 精简消息”聚合计数，
 * 生成一份 {@link LogErrorSummary}。所有内容在写出前都会经过 {@link SafeTextFilter} 脱敏。</p>
 *
 * <p>本分析只做只读统计，不复制整份日志。</p>
 *
 * @author 即白
 */
public class LogAnalyzer {

    private final JavaPlugin plugin;
    private final ConfigManager config;

    /** 匹配日志行中的插件名，形如 [ExamplePlugin] 或 [Server thread/ERROR]: [ExamplePlugin]。 */
    private static final Pattern PLUGIN_PATTERN = Pattern.compile("\\[([A-Za-z0-9_]+)\\]");

    /**
     * 用于剥离行首连续的 [时间戳] [线程/级别] 前缀，保留真正的消息主体。
     * 一行日志常有多个方括号前缀（如 [04:54:10] [Server thread/WARN]:），故用 + 重复匹配。
     */
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(\\[[^]]*]\\s*)+:?\\s*");

    public LogAnalyzer(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * 分析最近日志，生成错误摘要。可在异步线程调用（纯文件读取，不触碰游戏对象）。
     */
    public LogErrorSummary analyze() {
        if (!config.isLogAnalyzerEnabled()) {
            return LogErrorSummary.unavailable();
        }
        File logFile = resolveLatestLog();
        if (logFile == null || !logFile.exists()) {
            return LogErrorSummary.unavailable();
        }

        List<String> lines = FileUtil.readLastLines(logFile, config.getLogMaxLines());
        if (lines.isEmpty()) {
            return LogErrorSummary.unavailable();
        }

        int errorCount = 0;
        int warnCount = 0;
        // 聚合：key = 插件名 + 精简消息，value = [插件名, 精简消息, 次数]
        Map<String, int[]> counter = new LinkedHashMap<>();
        Map<String, String[]> meta = new LinkedHashMap<>();

        for (String rawLine : lines) {
            String line = rawLine;
            boolean isError = containsIgnoreCase(line, "ERROR")
                    || line.contains("Exception")
                    || line.contains("Caused by");
            boolean isWarn = containsIgnoreCase(line, "WARN");

            if (isError) {
                errorCount++;
            } else if (isWarn) {
                warnCount++;
            } else {
                continue;
            }

            String plugin = guessPluginName(line);
            String shortMsg = shorten(SafeTextFilter.filter(line));
            String key = plugin + "|" + shortMsg;

            counter.computeIfAbsent(key, k -> new int[]{0})[0]++;
            meta.putIfAbsent(key, new String[]{plugin, shortMsg});
        }

        // 转成条目并按次数降序
        List<LogErrorSummary.Entry> entries = new ArrayList<>();
        for (Map.Entry<String, int[]> e : counter.entrySet()) {
            String[] m = meta.get(e.getKey());
            entries.add(new LogErrorSummary.Entry(m[0], m[1], e.getValue()[0]));
        }
        entries.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));

        return new LogErrorSummary(errorCount, warnCount, entries, true);
    }

    /**
     * 定位 logs/latest.log。
     *
     * <p>优先用 {@link org.bukkit.Server#getWorldContainer()} 取服务器根目录（最可靠）；
     * 若不可用则回退到“插件数据目录上两级”。注意 {@code getDataFolder()} 可能是相对路径，
     * 直接对相对路径连续调用 {@code getParentFile()} 会得到 null，因此先转成绝对路径。</p>
     */
    private File resolveLatestLog() {
        try {
            // 首选：世界容器目录即服务器根目录
            File serverRoot = plugin.getServer().getWorldContainer();
            if (serverRoot == null || !serverRoot.exists()) {
                // 回退：插件数据目录上两级（先转绝对路径，避免相对路径 getParentFile 返回 null）
                File dataFolder = plugin.getDataFolder().getAbsoluteFile(); // plugins/JibaiServerDoctor
                File pluginsDir = dataFolder.getParentFile();               // plugins
                serverRoot = pluginsDir != null ? pluginsDir.getParentFile() : null;
            }
            if (serverRoot == null) {
                return null;
            }
            return new File(new File(serverRoot, "logs"), "latest.log");
        } catch (Exception ex) {
            return null;
        }
    }

    private String guessPluginName(String line) {
        Matcher matcher = PLUGIN_PATTERN.matcher(line);
        String candidate = "";
        while (matcher.find()) {
            String group = matcher.group(1);
            // 跳过明显是线程 / 级别标签的内容
            if (group.equalsIgnoreCase("ERROR") || group.equalsIgnoreCase("WARN")
                    || group.equalsIgnoreCase("INFO") || group.contains("thread")
                    || group.contains("Thread") || group.equalsIgnoreCase("STDERR")
                    || group.equalsIgnoreCase("STDOUT")) {
                continue;
            }
            candidate = group;
            break;
        }
        return candidate;
    }

    /** 截断过长的消息，避免摘要过长；同时去掉行首前缀。 */
    private String shorten(String line) {
        String stripped = PREFIX_PATTERN.matcher(line).replaceFirst("");
        stripped = stripped.trim();
        int max = 160;
        if (stripped.length() > max) {
            stripped = stripped.substring(0, max) + "...";
        }
        return stripped;
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toUpperCase().contains(needle.toUpperCase());
    }
}
