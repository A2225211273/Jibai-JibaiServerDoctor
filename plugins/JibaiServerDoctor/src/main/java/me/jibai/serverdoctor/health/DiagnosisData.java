package me.jibai.serverdoctor.health;

import me.jibai.serverdoctor.hopper.HopperRisk;
import me.jibai.serverdoctor.logs.LogErrorSummary;
import me.jibai.serverdoctor.snapshot.ChunkRiskSnapshot;
import me.jibai.serverdoctor.snapshot.ServerSnapshot;

import java.util.Collections;
import java.util.List;

/**
 * 一次完整体检的数据包（不可变）。
 *
 * <p>把快照、评分、等级、风险列表、红石 / 漏斗风险、日志摘要打包在一起，
 * 供状态展示、Markdown 报告、诊断包和 AI 提示词复用，避免各处重复计算。</p>
 *
 * @author 即白
 */
public class DiagnosisData {

    private final ServerSnapshot snapshot;
    private final int score;
    private final HealthLevel level;
    private final List<Risk> risks;
    private final List<ChunkRiskSnapshot> redstoneRisks;
    private final List<HopperRisk> hopperRisks;
    private final LogErrorSummary logSummary;

    public DiagnosisData(ServerSnapshot snapshot, int score, HealthLevel level, List<Risk> risks,
                         List<ChunkRiskSnapshot> redstoneRisks, List<HopperRisk> hopperRisks,
                         LogErrorSummary logSummary) {
        this.snapshot = snapshot;
        this.score = score;
        this.level = level;
        this.risks = risks == null ? Collections.emptyList() : risks;
        this.redstoneRisks = redstoneRisks == null ? Collections.emptyList() : redstoneRisks;
        this.hopperRisks = hopperRisks == null ? Collections.emptyList() : hopperRisks;
        this.logSummary = logSummary;
    }

    public ServerSnapshot getSnapshot() {
        return snapshot;
    }

    public int getScore() {
        return score;
    }

    public HealthLevel getLevel() {
        return level;
    }

    public List<Risk> getRisks() {
        return risks;
    }

    public List<ChunkRiskSnapshot> getRedstoneRisks() {
        return redstoneRisks;
    }

    public List<HopperRisk> getHopperRisks() {
        return hopperRisks;
    }

    public LogErrorSummary getLogSummary() {
        return logSummary;
    }
}
