package me.jibai.serverdoctor.health;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.hopper.HopperRisk;
import me.jibai.serverdoctor.logs.LogErrorSummary;
import me.jibai.serverdoctor.snapshot.ChunkRiskSnapshot;
import me.jibai.serverdoctor.snapshot.ServerSnapshot;

import java.util.List;

/**
 * 健康评分服务。
 *
 * <p>从满分 100 出发，按各项风险因素扣分，最终得到 0-100 的健康评分，再映射到
 * {@link HealthLevel} 五档等级。扣分因素与开发提示词一致：内存、实体、掉落物、
 * 加载区块、tick 延迟、红石、漏斗、插件报错。</p>
 *
 * @author 即白
 */
public class HealthScoreService {

    private final ConfigManager config;

    public HealthScoreService(ConfigManager config) {
        this.config = config;
    }

    /**
     * 计算健康评分。
     *
     * @param snapshot      服务器快照（必需）
     * @param redstoneRisks 红石高频区块（可为 null）
     * @param hopperRisks   漏斗超标区块（可为 null）
     * @param logSummary    日志错误摘要（可为 null）
     * @return 0-100 的评分
     */
    public int score(ServerSnapshot snapshot,
                     List<ChunkRiskSnapshot> redstoneRisks,
                     List<HopperRisk> hopperRisks,
                     LogErrorSummary logSummary) {
        if (snapshot == null) {
            return 100;
        }
        int score = 100;

        // 内存
        int mem = snapshot.getMemoryPercent();
        if (mem >= config.getMemoryDangerPercent()) {
            score -= 25;
        } else if (mem >= config.getMemoryWarningPercent()) {
            score -= 12;
        }

        // 实体
        int entities = snapshot.getTotalEntities();
        if (entities >= config.getTotalEntitiesDanger()) {
            score -= 20;
        } else if (entities >= config.getTotalEntitiesWarning()) {
            score -= 10;
        }

        // 掉落物
        int items = snapshot.getTotalDroppedItems();
        if (items >= config.getDroppedItemsDanger()) {
            score -= 12;
        } else if (items >= config.getDroppedItemsWarning()) {
            score -= 6;
        }

        // 加载区块
        int chunks = snapshot.getTotalLoadedChunks();
        if (chunks >= config.getLoadedChunksDanger()) {
            score -= 15;
        } else if (chunks >= config.getLoadedChunksWarning()) {
            score -= 7;
        }

        // tick 延迟
        double tick = snapshot.getApproxTickDelayMs();
        if (tick >= config.getTickDelayDangerMs()) {
            score -= 25;
        } else if (tick >= config.getTickDelayWarningMs()) {
            score -= 12;
        }

        // 红石高频区块：每个扣 5 分，最多扣 15
        if (redstoneRisks != null && !redstoneRisks.isEmpty()) {
            score -= Math.min(15, redstoneRisks.size() * 5);
        }

        // 漏斗超标区块：每个扣 3 分，最多扣 9
        if (hopperRisks != null && !hopperRisks.isEmpty()) {
            score -= Math.min(9, hopperRisks.size() * 3);
        }

        // 插件报错：按错误条数适度扣分，最多扣 15
        if (logSummary != null && logSummary.isLogAvailable()) {
            int errorPenalty = Math.min(15, logSummary.getErrorCount());
            score -= errorPenalty;
        }

        // 夹取到 0-100
        return Math.max(0, Math.min(100, score));
    }

    /** 根据评分返回等级。 */
    public HealthLevel level(int score) {
        return HealthLevel.fromScore(score);
    }
}
