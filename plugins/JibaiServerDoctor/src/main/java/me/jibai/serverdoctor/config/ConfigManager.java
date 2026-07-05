package me.jibai.serverdoctor.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * config.yml 管理器。
 *
 * <p>负责释放默认配置、加载配置，并以类型安全的 getter 暴露各配置项，
 * 使其它模块无需直接接触 Bukkit {@link FileConfiguration} 的字符串路径。</p>
 *
 * @author 即白
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 加载（或重载）配置。首次调用会释放默认 config.yml。 */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // -------------------- settings --------------------

    public boolean isPluginEnabled() {
        return config.getBoolean("settings.enabled", true);
    }

    public String getLanguage() {
        return config.getString("settings.language", "zh_CN");
    }

    public boolean isCollectOnStartup() {
        return config.getBoolean("settings.collect-on-startup", true);
    }

    // -------------------- snapshot --------------------

    public int getSnapshotIntervalSeconds() {
        return Math.max(10, config.getInt("snapshot.interval-seconds", 60));
    }

    public int getSnapshotKeepHistory() {
        return Math.max(1, config.getInt("snapshot.keep-history", 60));
    }

    // -------------------- health-score --------------------

    public boolean isHealthScoreEnabled() {
        return config.getBoolean("health-score.enabled", true);
    }

    public int getWarningScore() {
        return config.getInt("health-score.warning-score", 70);
    }

    public int getDangerScore() {
        return config.getInt("health-score.danger-score", 50);
    }

    // -------------------- thresholds --------------------

    public int getMemoryWarningPercent() {
        return config.getInt("thresholds.memory-warning-percent", 85);
    }

    public int getMemoryDangerPercent() {
        return config.getInt("thresholds.memory-danger-percent", 92);
    }

    public int getTotalEntitiesWarning() {
        return config.getInt("thresholds.total-entities-warning", 4000);
    }

    public int getTotalEntitiesDanger() {
        return config.getInt("thresholds.total-entities-danger", 8000);
    }

    public int getDroppedItemsWarning() {
        return config.getInt("thresholds.dropped-items-warning", 1000);
    }

    public int getDroppedItemsDanger() {
        return config.getInt("thresholds.dropped-items-danger", 2500);
    }

    public int getLoadedChunksWarning() {
        return config.getInt("thresholds.loaded-chunks-warning", 6000);
    }

    public int getLoadedChunksDanger() {
        return config.getInt("thresholds.loaded-chunks-danger", 10000);
    }

    public int getTickDelayWarningMs() {
        return config.getInt("thresholds.tick-delay-warning-ms", 70);
    }

    public int getTickDelayDangerMs() {
        return config.getInt("thresholds.tick-delay-danger-ms", 120);
    }

    // -------------------- redstone --------------------

    public boolean isRedstoneEnabled() {
        return config.getBoolean("redstone.enabled", true);
    }

    public int getRedstoneWindowSeconds() {
        return Math.max(1, config.getInt("redstone.window-seconds", 10));
    }

    public int getRedstoneMaxEventsPerChunk() {
        return config.getInt("redstone.max-events-per-chunk", 300);
    }

    public boolean isRedstoneNotifyAdmins() {
        return config.getBoolean("redstone.notify-admins", true);
    }

    // -------------------- hopper --------------------

    public boolean isHopperEnabled() {
        return config.getBoolean("hopper.enabled", true);
    }

    public int getHopperScanIntervalSeconds() {
        return Math.max(30, config.getInt("hopper.scan-interval-seconds", 300));
    }

    public int getHopperMaxPerChunk() {
        return config.getInt("hopper.max-hoppers-per-chunk", 64);
    }

    public boolean isHopperNotifyAdmins() {
        return config.getBoolean("hopper.notify-admins", true);
    }

    // -------------------- log-analyzer --------------------

    public boolean isLogAnalyzerEnabled() {
        return config.getBoolean("log-analyzer.enabled", true);
    }

    public int getLogMaxLines() {
        return Math.max(100, config.getInt("log-analyzer.max-lines", 5000));
    }

    public List<String> getLogKeywords() {
        List<String> list = config.getStringList("log-analyzer.keywords");
        if (list == null || list.isEmpty()) {
            return Arrays.asList("ERROR", "WARN", "Exception", "Caused by");
        }
        return list;
    }

    // -------------------- reports --------------------

    public String getReportsDirectory() {
        return config.getString("reports.directory", "reports");
    }

    public boolean isReportIncludePluginErrors() {
        return config.getBoolean("reports.include-plugin-errors", true);
    }

    public boolean isReportIncludeRiskSuggestions() {
        return config.getBoolean("reports.include-risk-suggestions", true);
    }

    // -------------------- exports --------------------

    public String getExportsDirectory() {
        return config.getString("exports.directory", "exports");
    }

    public boolean isExportIncludeAiPrompt() {
        return config.getBoolean("exports.include-ai-prompt", true);
    }

    public boolean isExportFilterSensitiveData() {
        return config.getBoolean("exports.filter-sensitive-data", true);
    }

    // -------------------- alerts --------------------

    public boolean isAlertsEnabled() {
        return config.getBoolean("alerts.enabled", true);
    }

    public int getAlertCooldownSeconds() {
        return Math.max(0, config.getInt("alerts.cooldown-seconds", 120));
    }

    public String getAlertPermission() {
        return config.getString("alerts.permission", "jibai.doctor.notify");
    }

    // -------------------- logging --------------------

    public boolean isConsoleLogging() {
        return config.getBoolean("logging.console", true);
    }

    /** 暴露底层配置，供个别需要读取原始节点的场景使用。 */
    public FileConfiguration raw() {
        return config;
    }
}
