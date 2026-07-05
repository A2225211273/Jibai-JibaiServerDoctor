package me.jibai.serverdoctor.report;

import me.jibai.serverdoctor.health.DiagnosisData;
import me.jibai.serverdoctor.health.Risk;
import me.jibai.serverdoctor.logs.LogErrorSummary;
import me.jibai.serverdoctor.snapshot.ChunkRiskSnapshot;
import me.jibai.serverdoctor.snapshot.ServerSnapshot;
import me.jibai.serverdoctor.util.FormatUtil;
import me.jibai.serverdoctor.util.TimeUtil;
import org.bukkit.Bukkit;

import java.util.List;

/**
 * AI 分析提示词生成器。
 *
 * <p>生成 {@code ai-prompt.txt} 的内容：把本次体检的关键数据整理成一段可直接复制给 AI 的文本，
 * 让 AI 帮助分析服务器卡顿原因。文本已经是脱敏后的统计数据，不含敏感信息。</p>
 *
 * @author 即白
 */
public class AiPromptWriter {

    /** 构造给 AI 的提示词文本。 */
    public String build(DiagnosisData data) {
        ServerSnapshot s = data.getSnapshot();
        StringBuilder sb = new StringBuilder();

        sb.append("你是一名 Minecraft 服务器性能诊断专家。以下是我的服务器体检数据，"
                + "请帮我分析可能的卡顿原因，并按优先级给出可执行的优化建议。\n\n");
        sb.append("== 体检时间 ==\n").append(TimeUtil.displayNow()).append("\n\n");

        sb.append("== 服务器环境 ==\n");
        sb.append("服务端核心：").append(Bukkit.getName()).append("\n");
        sb.append("服务端版本：").append(Bukkit.getVersion()).append("\n");
        sb.append("Bukkit 版本：").append(Bukkit.getBukkitVersion()).append("\n");
        sb.append("Java 版本：").append(System.getProperty("java.version", "未知")).append("\n");
        sb.append("在线玩家：").append(s.getOnlinePlayers()).append("\n");
        sb.append("世界数量：").append(s.getWorldCount()).append("\n\n");

        sb.append("== 健康评分 ==\n");
        sb.append(data.getScore()).append(" / 100（").append(data.getLevel().getDisplayName()).append("）\n\n");

        sb.append("== 关键指标 ==\n");
        sb.append("内存占用：").append(s.getUsedMemoryMb()).append("MB / ")
                .append(s.getMaxMemoryMb()).append("MB（").append(s.getMemoryPercent()).append("%）\n");
        sb.append("近似 tick 延迟：").append(Math.round(s.getApproxTickDelayMs())).append("ms（理想 50ms）\n");
        sb.append("实体总数：").append(FormatUtil.number(s.getTotalEntities())).append("\n");
        sb.append("掉落物：").append(FormatUtil.number(s.getTotalDroppedItems())).append("\n");
        sb.append("经验球：").append(FormatUtil.number(s.getTotalExpOrbs())).append("\n");
        sb.append("加载区块：").append(FormatUtil.number(s.getTotalLoadedChunks())).append("\n\n");

        // 红石 / 漏斗风险
        List<ChunkRiskSnapshot> redstone = data.getRedstoneRisks();
        if (!redstone.isEmpty()) {
            sb.append("== 红石高频区块 ==\n");
            for (ChunkRiskSnapshot r : redstone) {
                sb.append("区块 ").append(r.coordinateLabel()).append("：").append(r.getValue()).append(" 次\n");
            }
            sb.append("\n");
        }
        if (!data.getHopperRisks().isEmpty()) {
            sb.append("== 漏斗超标区块 ==\n");
            data.getHopperRisks().forEach(r -> sb.append("区块 ").append(r.coordinateLabel())
                    .append("：").append(r.getHopperCount()).append(" 个\n"));
            sb.append("\n");
        }

        // 插件报错
        LogErrorSummary log = data.getLogSummary();
        if (log != null && log.isLogAvailable() && log.hasProblems()) {
            sb.append("== 插件报错摘要 ==\n");
            sb.append("ERROR/Exception：").append(log.getErrorCount())
                    .append(" 条，WARN：").append(log.getWarnCount()).append(" 条\n");
            int shown = 0;
            for (LogErrorSummary.Entry e : log.getEntries()) {
                if (shown++ >= 10) {
                    break;
                }
                String name = e.getPluginName() == null || e.getPluginName().isEmpty()
                        ? "未知" : e.getPluginName();
                sb.append("[").append(name).append("] x").append(e.getCount())
                        .append("：").append(e.getMessage()).append("\n");
            }
            sb.append("\n");
        }

        // 已识别风险排名
        sb.append("== 已识别风险排名 ==\n");
        List<Risk> risks = data.getRisks();
        if (risks.isEmpty()) {
            sb.append("插件未发现明显风险。\n");
        } else {
            int index = 1;
            for (Risk risk : risks) {
                sb.append(index++).append(". [").append(risk.getLevel().getDisplayName()).append("] ")
                        .append(risk.getMessage()).append("\n");
            }
        }
        sb.append("\n请基于以上数据给出分析。\n");

        return sb.toString();
    }
}
