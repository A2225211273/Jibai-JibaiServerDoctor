package me.jibai.serverdoctor.health;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.hopper.HopperRisk;
import me.jibai.serverdoctor.logs.LogErrorSummary;
import me.jibai.serverdoctor.snapshot.ChunkRiskSnapshot;
import me.jibai.serverdoctor.snapshot.ServerSnapshot;
import me.jibai.serverdoctor.snapshot.WorldSnapshot;
import me.jibai.serverdoctor.util.FormatUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 风险分析器。
 *
 * <p>根据一份快照，以及红石 / 漏斗 / 日志的辅助数据，生成一份面向服主的风险列表。
 * 每条风险都带有严重程度、原因说明和处理建议，并按严重程度从高到低排序，
 * 对应开发提示词的“异常原因排名”。</p>
 *
 * <p>本类是纯计算，不持有其它服务的引用，避免模块间循环依赖。</p>
 *
 * @author 即白
 */
public class RiskAnalyzer {

    private final ConfigManager config;

    public RiskAnalyzer(ConfigManager config) {
        this.config = config;
    }

    /**
     * 综合分析生成风险列表。
     *
     * @param snapshot      服务器快照（必需）
     * @param redstoneRisks 当前红石高频区块（可为 null）
     * @param hopperRisks   当前漏斗超标区块（可为 null）
     * @param logSummary    日志错误摘要（可为 null）
     * @return 已按严重程度排序的风险列表
     */
    public List<Risk> analyze(ServerSnapshot snapshot,
                              List<ChunkRiskSnapshot> redstoneRisks,
                              List<HopperRisk> hopperRisks,
                              LogErrorSummary logSummary) {
        List<Risk> risks = new ArrayList<>();
        if (snapshot == null) {
            return risks;
        }

        analyzeMemory(snapshot, risks);
        analyzeEntities(snapshot, risks);
        analyzeDroppedItems(snapshot, risks);
        analyzeChunks(snapshot, risks);
        analyzeTick(snapshot, risks);
        analyzeRedstone(redstoneRisks, risks);
        analyzeHoppers(hopperRisks, risks);
        analyzeLogs(logSummary, risks);

        // 严重程度高的排在前面
        risks.sort(Comparator.comparingInt((Risk r) -> r.getLevel().getWeight()).reversed());
        return risks;
    }

    private void analyzeMemory(ServerSnapshot s, List<Risk> risks) {
        int percent = s.getMemoryPercent();
        if (percent >= config.getMemoryDangerPercent()) {
            risks.add(new Risk(RiskLevel.CRITICAL,
                    "内存占用过高：" + percent + "%（" + s.getUsedMemoryMb() + "MB / " + s.getMaxMemoryMb() + "MB）",
                    "内存接近上限容易触发频繁 GC 导致卡顿。可考虑提高服务器分配内存（-Xmx），"
                            + "或排查是否有插件 / 生物农场占用过多内存。"));
        } else if (percent >= config.getMemoryWarningPercent()) {
            risks.add(new Risk(RiskLevel.HIGH,
                    "内存占用偏高：" + percent + "%（" + s.getUsedMemoryMb() + "MB / " + s.getMaxMemoryMb() + "MB）",
                    "建议持续观察内存趋势，若持续走高需考虑加内存或优化实体 / 区块数量。"));
        }
    }

    private void analyzeEntities(ServerSnapshot s, List<Risk> risks) {
        int total = s.getTotalEntities();
        if (total >= config.getTotalEntitiesDanger()) {
            risks.add(new Risk(RiskLevel.CRITICAL,
                    "全服实体数量过高：共 " + FormatUtil.number(total) + " 个实体",
                    "实体过多会显著增加每 tick 计算量。请用 /doctor inspect 定位实体密集的世界或区块，"
                            + "常见来源是刷怪塔、动物养殖场或掉落物堆积。"));
        } else if (total >= config.getTotalEntitiesWarning()) {
            risks.add(new Risk(RiskLevel.MEDIUM,
                    "全服实体数量偏高：共 " + FormatUtil.number(total) + " 个实体",
                    "可关注实体最多的世界，必要时限制刷怪或整理养殖场。"));
        }

        // 找出实体最多的世界，单独提示，便于定位
        WorldSnapshot worst = null;
        for (WorldSnapshot w : s.getWorlds()) {
            if (worst == null || w.getEntities() > worst.getEntities()) {
                worst = w;
            }
        }
        if (worst != null && worst.getEntities() >= config.getTotalEntitiesWarning() / 2) {
            risks.add(new Risk(RiskLevel.MEDIUM,
                    "世界 " + worst.getWorldName() + " 实体数量较多：" + FormatUtil.number(worst.getEntities()) + " 个",
                    "该世界是当前实体压力主要来源，可优先在此世界排查刷怪塔或掉落物。"));
        }
    }

    private void analyzeDroppedItems(ServerSnapshot s, List<Risk> risks) {
        int items = s.getTotalDroppedItems();
        if (items >= config.getDroppedItemsDanger()) {
            risks.add(new Risk(RiskLevel.HIGH,
                    "掉落物数量过高：共 " + FormatUtil.number(items) + " 个",
                    "掉落物堆积通常来自刷怪塔或玩家丢弃。本插件不会自动清理，"
                            + "如需清理可使用你的清理插件，或排查产生源头。"));
        } else if (items >= config.getDroppedItemsWarning()) {
            risks.add(new Risk(RiskLevel.MEDIUM,
                    "掉落物数量偏高：共 " + FormatUtil.number(items) + " 个",
                    "建议关注掉落物增长趋势，避免持续堆积。"));
        }
    }

    private void analyzeChunks(ServerSnapshot s, List<Risk> risks) {
        int chunks = s.getTotalLoadedChunks();
        if (chunks >= config.getLoadedChunksDanger()) {
            risks.add(new Risk(RiskLevel.HIGH,
                    "加载区块过多：共 " + FormatUtil.number(chunks) + " 个",
                    "区块越多，每 tick 需要处理的范围越大。可检查是否有大量常加载区块（如传送门、"
                            + "常加载机器）或玩家分散在过多区域。"));
        } else if (chunks >= config.getLoadedChunksWarning()) {
            risks.add(new Risk(RiskLevel.MEDIUM,
                    "加载区块偏多：共 " + FormatUtil.number(chunks) + " 个",
                    "建议关注区块数量，必要时调整视距或排查常加载区块。"));
        }
    }

    private void analyzeTick(ServerSnapshot s, List<Risk> risks) {
        int tick = (int) Math.round(s.getApproxTickDelayMs());
        if (tick >= config.getTickDelayDangerMs()) {
            risks.add(new Risk(RiskLevel.CRITICAL,
                    "服务器 tick 延迟过高：约 " + tick + "ms（理想 50ms）",
                    "tick 延迟高说明服务器已明显卡顿。请结合本报告其它风险项综合排查，"
                            + "重点看实体、红石、内存。"));
        } else if (tick >= config.getTickDelayWarningMs()) {
            risks.add(new Risk(RiskLevel.HIGH,
                    "服务器 tick 延迟偏高：约 " + tick + "ms（理想 50ms）",
                    "服务器开始出现追赶迹象，建议尽快排查主要压力来源。"));
        }
    }

    private void analyzeRedstone(List<ChunkRiskSnapshot> redstoneRisks, List<Risk> risks) {
        if (redstoneRisks == null || redstoneRisks.isEmpty()) {
            return;
        }
        int window = config.getRedstoneWindowSeconds();
        for (ChunkRiskSnapshot r : redstoneRisks) {
            RiskLevel level = r.getValue() >= config.getRedstoneMaxEventsPerChunk() * 2
                    ? RiskLevel.CRITICAL : RiskLevel.HIGH;
            risks.add(new Risk(level,
                    "区块 " + r.coordinateLabel() + " 红石事件过高：" + window + " 秒内 " + r.getValue() + " 次",
                    "高频红石通常来自时钟电路或红石机器。本插件只记录不破坏，"
                            + "可前往该坐标检查并优化电路（如加延迟、改用观察者）。"));
        }
    }

    private void analyzeHoppers(List<HopperRisk> hopperRisks, List<Risk> risks) {
        if (hopperRisks == null || hopperRisks.isEmpty()) {
            return;
        }
        for (HopperRisk r : hopperRisks) {
            risks.add(new Risk(RiskLevel.MEDIUM,
                    "区块 " + r.coordinateLabel() + " 漏斗数量过高：" + r.getHopperCount() + " 个",
                    "密集漏斗会持续尝试转移物品，增加负担。本插件只记录不破坏，"
                            + "可考虑减少漏斗数量或改用其它物品运输方案。"));
        }
    }

    private void analyzeLogs(LogErrorSummary logSummary, List<Risk> risks) {
        if (logSummary == null || !logSummary.isLogAvailable()) {
            return;
        }
        for (LogErrorSummary.Entry entry : logSummary.getEntries()) {
            if (entry.getCount() >= 5) {
                String pluginPart = entry.getPluginName() == null || entry.getPluginName().isEmpty()
                        ? "" : "插件 " + entry.getPluginName() + " ";
                risks.add(new Risk(entry.getCount() >= 20 ? RiskLevel.HIGH : RiskLevel.MEDIUM,
                        pluginPart + "报错频繁：最近日志中出现 " + entry.getCount() + " 次",
                        "频繁报错可能拖慢服务器或造成功能异常。建议使用 /doctor errors 查看摘要，"
                                + "并检查对应插件版本是否与服务器兼容。"));
            }
        }
    }
}
