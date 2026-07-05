package me.jibai.serverdoctor.report;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.health.DiagnosisData;
import me.jibai.serverdoctor.util.FileUtil;
import me.jibai.serverdoctor.util.TimeUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * 体检报告服务。
 *
 * <p>负责把 {@link MarkdownReportWriter} 生成的报告写入
 * {@code plugins/JibaiServerDoctor/reports/report-yyyy-MM-dd-HH-mm-ss.md}，
 * 并返回生成的文件，供指令层反馈路径。文件写入属于 IO 操作，建议在异步线程调用。</p>
 *
 * @author 即白
 */
public class ReportService {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final MarkdownReportWriter writer;

    public ReportService(JavaPlugin plugin, ConfigManager config, MarkdownReportWriter writer) {
        this.plugin = plugin;
        this.config = config;
        this.writer = writer;
    }

    /** 报告输出目录。 */
    public File getReportsDir() {
        return new File(plugin.getDataFolder(), config.getReportsDirectory());
    }

    /**
     * 生成报告并写入文件。
     *
     * @param data 体检数据
     * @return 生成的报告文件
     * @throws IOException 写入失败时抛出
     */
    public File generate(DiagnosisData data) throws IOException {
        String content = writer.build(data);
        File dir = getReportsDir();
        FileUtil.ensureDir(dir);
        File file = new File(dir, "report-" + TimeUtil.fileTimestamp() + ".md");
        FileUtil.writeString(file, content);
        return file;
    }

    /**
     * 找到最近生成的一份报告文件，没有则返回 null。用于诊断包复用最新报告。
     */
    public File findLatestReport() {
        File dir = getReportsDir();
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        File[] files = dir.listFiles((d, name) -> name.startsWith("report-") && name.endsWith(".md"));
        if (files == null || files.length == 0) {
            return null;
        }
        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) {
                latest = f;
            }
        }
        return latest;
    }
}
