package me.jibai.serverdoctor.health;

import me.jibai.serverdoctor.hopper.HopperRisk;
import me.jibai.serverdoctor.hopper.HopperScanner;
import me.jibai.serverdoctor.logs.LogAnalyzer;
import me.jibai.serverdoctor.logs.LogErrorSummary;
import me.jibai.serverdoctor.redstone.RedstoneTracker;
import me.jibai.serverdoctor.snapshot.ChunkRiskSnapshot;
import me.jibai.serverdoctor.snapshot.ServerSnapshot;
import me.jibai.serverdoctor.snapshot.SnapshotService;

import java.util.List;

/**
 * 体检汇总服务。
 *
 * <p>把快照采集、红石 / 漏斗风险、日志分析、评分与风险分析组装成一份完整的
 * {@link DiagnosisData}。这是 status / report / export 等功能的统一入口，
 * 保证各处使用的是同一套计算逻辑。</p>
 *
 * @author 即白
 */
public class DiagnosisService {

    private final SnapshotService snapshotService;
    private final RedstoneTracker redstoneTracker;
    private final HopperScanner hopperScanner;
    private final LogAnalyzer logAnalyzer;
    private final HealthScoreService scoreService;
    private final RiskAnalyzer riskAnalyzer;

    public DiagnosisService(SnapshotService snapshotService, RedstoneTracker redstoneTracker,
                            HopperScanner hopperScanner, LogAnalyzer logAnalyzer,
                            HealthScoreService scoreService, RiskAnalyzer riskAnalyzer) {
        this.snapshotService = snapshotService;
        this.redstoneTracker = redstoneTracker;
        this.hopperScanner = hopperScanner;
        this.logAnalyzer = logAnalyzer;
        this.scoreService = scoreService;
        this.riskAnalyzer = riskAnalyzer;
    }

    /**
     * 生成完整体检数据。必须在主线程调用（会即时采集快照）。
     *
     * @param includeLogAnalysis 是否分析日志（日志读取相对较重，status 可关闭以更轻量）
     */
    public DiagnosisData diagnose(boolean includeLogAnalysis) {
        ServerSnapshot snapshot = snapshotService.getLatestOrCapture();
        List<ChunkRiskSnapshot> redstoneRisks = redstoneTracker.getCurrentRisks();
        List<HopperRisk> hopperRisks = hopperScanner.getLatestRisks();
        LogErrorSummary logSummary = includeLogAnalysis
                ? logAnalyzer.analyze() : LogErrorSummary.unavailable();

        int score = scoreService.score(snapshot, redstoneRisks, hopperRisks, logSummary);
        HealthLevel level = scoreService.level(score);
        List<Risk> risks = riskAnalyzer.analyze(snapshot, redstoneRisks, hopperRisks, logSummary);

        return new DiagnosisData(snapshot, score, level, risks, redstoneRisks, hopperRisks, logSummary);
    }
}
