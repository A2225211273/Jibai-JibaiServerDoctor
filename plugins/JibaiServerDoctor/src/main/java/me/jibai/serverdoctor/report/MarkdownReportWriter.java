package me.jibai.serverdoctor.report;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.health.DiagnosisData;
import me.jibai.serverdoctor.health.Risk;
import me.jibai.serverdoctor.logs.LogErrorSummary;
import me.jibai.serverdoctor.snapshot.ChunkRiskSnapshot;
import me.jibai.serverdoctor.snapshot.ServerSnapshot;
import me.jibai.serverdoctor.snapshot.WorldSnapshot;
import me.jibai.serverdoctor.util.FormatUtil;
import me.jibai.serverdoctor.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Markdown 体检报告生成器。
 *
 * <p>把一份 {@link DiagnosisData} 渲染成服主能看懂的 Markdown 文本，涵盖服务器基础信息、
 * 内存 / 实体 / 区块统计、红石 / 漏斗风险、插件报错、健康评分、问题排名与处理建议。</p>
 *
 * @author 即白
 */
public class MarkdownReportWriter {

    private final JavaPlugin plugin;
    private final ConfigManager config;

    public MarkdownReportWriter(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** 生成完整的 Markdown 报告文本。 */
    public String build(DiagnosisData data) {
        StringBuilder sb = new StringBuilder();
        ServerSnapshot s = data.getSnapshot();

        sb.append("# JibaiServerDoctor 服务器体检报告\n\n");
        sb.append("> 由 JibaiServerDoctor 生成　作者：即白　邮箱：jibai0517@gamil.com\n\n");
        sb.append("- 生成时间：").append(TimeUtil.displayNow()).append("\n\n");

        // 一、服务器基础信息
        sb.append("## 一、服务器基础信息\n\n");
        sb.append("| 项目 | 内容 |\n");
        sb.append("| --- | --- |\n");
        sb.append("| 服务端核心 | ").append(escape(Bukkit.getName())).append(" |\n");
        sb.append("| 服务端版本 | ").append(escape(Bukkit.getVersion())).append(" |\n");
        sb.append("| Bukkit 版本 | ").append(escape(Bukkit.getBukkitVersion())).append(" |\n");
        sb.append("| Java 版本 | ").append(escape(System.getProperty("java.version", "未知"))).append(" |\n");
        sb.append("| 操作系统 | ").append(escape(System.getProperty("os.name", "未知"))).append(" |\n");
        sb.append("| 在线玩家 | ").append(s.getOnlinePlayers()).append(" |\n");
        sb.append("| 世界数量 | ").append(s.getWorldCount()).append(" |\n\n");

        // 二、健康评分
        sb.append("## 二、健康评分\n\n");
        sb.append("**").append(data.getScore()).append(" / 100　【")
                .append(data.getLevel().getDisplayName()).append("】**\n\n");

        // 三、内存统计
        sb.append("## 三、内存统计\n\n");
        sb.append("- 已用内存：").append(s.getUsedMemoryMb()).append(" MB\n");
        sb.append("- 最大内存：").append(s.getMaxMemoryMb()).append(" MB\n");
        sb.append("- 内存占用：").append(s.getMemoryPercent()).append(" %\n");
        sb.append("- 近似 tick 延迟：").append(Math.round(s.getApproxTickDelayMs()))
                .append(" ms（理想 50ms）\n\n");

        // 四、实体与区块统计
        sb.append("## 四、实体与区块统计\n\n");
        sb.append("- 实体总数：").append(FormatUtil.number(s.getTotalEntities())).append("\n");
        sb.append("- 掉落物：").append(FormatUtil.number(s.getTotalDroppedItems())).append("\n");
        sb.append("- 经验球：").append(FormatUtil.number(s.getTotalExpOrbs())).append("\n");
        sb.append("- 怪物：").append(FormatUtil.number(s.getTotalMonsters())).append("\n");
        sb.append("- 动物：").append(FormatUtil.number(s.getTotalAnimals())).append("\n");
        sb.append("- 加载区块：").append(FormatUtil.number(s.getTotalLoadedChunks())).append("\n\n");

        sb.append("### 各世界明细\n\n");
        sb.append("| 世界 | 实体 | 掉落物 | 经验球 | 怪物 | 动物 | 加载区块 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- |\n");
        for (WorldSnapshot w : s.getWorlds()) {
            sb.append("| ").append(escape(w.getWorldName()))
                    .append(" | ").append(w.getEntities())
                    .append(" | ").append(w.getDroppedItems())
                    .append(" | ").append(w.getExpOrbs())
                    .append(" | ").append(w.getMonsters())
                    .append(" | ").append(w.getAnimals())
                    .append(" | ").append(w.getLoadedChunks())
                    .append(" |\n");
        }
        sb.append("\n");

        // 五、红石风险
        sb.append("## 五、红石风险\n\n");
        List<ChunkRiskSnapshot> redstone = data.getRedstoneRisks();
        if (redstone.isEmpty()) {
            sb.append("未发现红石高频区块。\n\n");
        } else {
            sb.append("| 区块 | ").append(config.getRedstoneWindowSeconds()).append(" 秒内事件次数 |\n");
            sb.append("| --- | --- |\n");
            for (ChunkRiskSnapshot r : redstone) {
                sb.append("| ").append(escape(r.coordinateLabel()))
                        .append(" | ").append(r.getValue()).append(" |\n");
            }
            sb.append("\n");
        }

        // 六、漏斗风险
        sb.append("## 六、漏斗风险\n\n");
        if (data.getHopperRisks().isEmpty()) {
            sb.append("未发现漏斗超标区块。\n\n");
        } else {
            sb.append("| 区块 | 漏斗数量 |\n");
            sb.append("| --- | --- |\n");
            data.getHopperRisks().forEach(r -> sb.append("| ").append(escape(r.coordinateLabel()))
                    .append(" | ").append(r.getHopperCount()).append(" |\n"));
            sb.append("\n");
        }

        // 七、插件报错统计
        if (config.isReportIncludePluginErrors()) {
            sb.append("## 七、插件报错统计\n\n");
            LogErrorSummary log = data.getLogSummary();
            if (log == null || !log.isLogAvailable()) {
                sb.append("未能读取日志或日志分析已关闭。\n\n");
            } else if (!log.hasProblems()) {
                sb.append("最近日志中未发现明显报错。\n\n");
            } else {
                sb.append("- ERROR / Exception：").append(log.getErrorCount()).append(" 条\n");
                sb.append("- WARN：").append(log.getWarnCount()).append(" 条\n\n");
                sb.append("| 次数 | 插件 | 摘要 |\n");
                sb.append("| --- | --- | --- |\n");
                int shown = 0;
                for (LogErrorSummary.Entry e : log.getEntries()) {
                    if (shown++ >= 15) {
                        break;
                    }
                    String pluginName = e.getPluginName() == null || e.getPluginName().isEmpty()
                            ? "未知" : e.getPluginName();
                    sb.append("| ").append(e.getCount())
                            .append(" | ").append(escape(pluginName))
                            .append(" | ").append(escape(e.getMessage()))
                            .append(" |\n");
                }
                sb.append("\n");
            }
        }

        // 八、问题排名与处理建议
        sb.append("## 八、问题排名与处理建议\n\n");
        List<Risk> risks = data.getRisks();
        if (risks.isEmpty()) {
            sb.append("当前没有发现明显风险，服务器状态良好。\n\n");
        } else {
            int index = 1;
            for (Risk risk : risks) {
                sb.append("**").append(index++).append(". [")
                        .append(risk.getLevel().getDisplayName()).append("] ")
                        .append(escape(risk.getMessage())).append("**\n\n");
                if (config.isReportIncludeRiskSuggestions()) {
                    sb.append("> 建议：").append(escape(risk.getSuggestion())).append("\n\n");
                }
            }
        }

        sb.append("---\n\n");
        sb.append("*本报告仅用于诊断参考。JibaiServerDoctor 只做体检、报告与提醒，"
                + "不会删除数据、破坏方块或清理实体。*\n");

        return sb.toString();
    }

    /** 转义 Markdown 表格中的竖线与换行，避免破坏表格结构。 */
    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "\\|").replace("\n", " ").replace("\r", " ");
    }
}
