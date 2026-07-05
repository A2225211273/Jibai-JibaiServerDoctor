package me.jibai.serverdoctor.report;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.health.DiagnosisData;
import me.jibai.serverdoctor.logs.LogErrorSummary;
import me.jibai.serverdoctor.util.FileUtil;
import me.jibai.serverdoctor.util.SafeTextFilter;
import me.jibai.serverdoctor.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * 诊断包导出服务。
 *
 * <p>把本次体检整理成一个文件夹
 * {@code plugins/JibaiServerDoctor/exports/export-yyyy-MM-dd-HH-mm-ss/}，包含：</p>
 * <ul>
 *   <li>report.md —— 最新体检报告</li>
 *   <li>error-summary.txt —— 最近错误摘要（已脱敏）</li>
 *   <li>server-info.txt —— 服务器环境信息</li>
 *   <li>config.yml —— 插件配置副本（不含任何敏感信息）</li>
 *   <li>ai-prompt.txt —— 给 AI 分析用的提示词（可选）</li>
 * </ul>
 *
 * <p><b>安全约束：</b>不自动打包、不上传，也不复制 server.properties 等可能含敏感信息的文件；
 * 错误摘要写出前均经过脱敏处理。</p>
 *
 * @author 即白
 */
public class ExportService {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final MarkdownReportWriter reportWriter;
    private final AiPromptWriter aiPromptWriter;

    public ExportService(JavaPlugin plugin, ConfigManager config,
                         MarkdownReportWriter reportWriter, AiPromptWriter aiPromptWriter) {
        this.plugin = plugin;
        this.config = config;
        this.reportWriter = reportWriter;
        this.aiPromptWriter = aiPromptWriter;
    }

    /** 诊断包根目录。 */
    public File getExportsDir() {
        return new File(plugin.getDataFolder(), config.getExportsDirectory());
    }

    /**
     * 导出诊断包。
     *
     * @param data 体检数据
     * @return 生成的诊断包文件夹
     * @throws IOException 写入失败时抛出
     */
    public File export(DiagnosisData data) throws IOException {
        File exportDir = new File(getExportsDir(), "export-" + TimeUtil.fileTimestamp());
        FileUtil.ensureDir(exportDir);

        // 1. 体检报告
        FileUtil.writeString(new File(exportDir, "report.md"), reportWriter.build(data));

        // 2. 错误摘要（脱敏）
        FileUtil.writeString(new File(exportDir, "error-summary.txt"), buildErrorSummary(data));

        // 3. 服务器环境信息
        FileUtil.writeString(new File(exportDir, "server-info.txt"), buildServerInfo());

        // 4. 插件配置副本
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileUtil.copyFile(configFile, new File(exportDir, "config.yml"));

        // 5. AI 提示词（可选）
        if (config.isExportIncludeAiPrompt()) {
            FileUtil.writeString(new File(exportDir, "ai-prompt.txt"), aiPromptWriter.build(data));
        }

        return exportDir;
    }

    private String buildErrorSummary(DiagnosisData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("JibaiServerDoctor 错误摘要\n");
        sb.append("生成时间：").append(TimeUtil.displayNow()).append("\n\n");

        LogErrorSummary log = data.getLogSummary();
        if (log == null || !log.isLogAvailable()) {
            sb.append("未能读取日志，或日志分析已在配置中关闭。\n");
            return sb.toString();
        }
        if (!log.hasProblems()) {
            sb.append("最近日志中未发现明显报错。\n");
            return sb.toString();
        }
        sb.append("ERROR/Exception：").append(log.getErrorCount())
                .append(" 条，WARN：").append(log.getWarnCount()).append(" 条\n\n");
        for (LogErrorSummary.Entry e : log.getEntries()) {
            String name = e.getPluginName() == null || e.getPluginName().isEmpty()
                    ? "未知" : e.getPluginName();
            // 摘要内容再次脱敏，双保险
            String message = config.isExportFilterSensitiveData()
                    ? SafeTextFilter.filter(e.getMessage()) : e.getMessage();
            sb.append("x").append(e.getCount()).append(" [").append(name).append("] ")
                    .append(message).append("\n");
        }
        return sb.toString();
    }

    private String buildServerInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("JibaiServerDoctor 服务器环境信息\n");
        sb.append("生成时间：").append(TimeUtil.displayNow()).append("\n\n");
        sb.append("服务端核心：").append(Bukkit.getName()).append("\n");
        sb.append("服务端版本：").append(Bukkit.getVersion()).append("\n");
        sb.append("Bukkit 版本：").append(Bukkit.getBukkitVersion()).append("\n");
        sb.append("在线玩家：").append(Bukkit.getOnlinePlayers().size())
                .append(" / ").append(Bukkit.getMaxPlayers()).append("\n");
        sb.append("Java 版本：").append(System.getProperty("java.version", "未知")).append("\n");
        sb.append("Java 供应商：").append(System.getProperty("java.vendor", "未知")).append("\n");
        sb.append("操作系统：").append(System.getProperty("os.name", "未知"))
                .append(" ").append(System.getProperty("os.version", "")).append("\n");
        sb.append("系统架构：").append(System.getProperty("os.arch", "未知")).append("\n");
        sb.append("CPU 核心数：").append(Runtime.getRuntime().availableProcessors()).append("\n");

        Runtime rt = Runtime.getRuntime();
        sb.append("JVM 最大内存：").append(rt.maxMemory() / (1024 * 1024)).append(" MB\n");
        sb.append("JVM 已分配内存：").append(rt.totalMemory() / (1024 * 1024)).append(" MB\n");

        sb.append("\n已安装插件：\n");
        for (org.bukkit.plugin.Plugin p : Bukkit.getPluginManager().getPlugins()) {
            sb.append("- ").append(p.getName()).append(" v").append(p.getDescription().getVersion())
                    .append(p.isEnabled() ? "" : "（未启用）").append("\n");
        }
        return sb.toString();
    }
}
