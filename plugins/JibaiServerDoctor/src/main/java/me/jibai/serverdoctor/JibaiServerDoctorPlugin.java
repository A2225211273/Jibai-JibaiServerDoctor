package me.jibai.serverdoctor;

import me.jibai.serverdoctor.alert.AlertService;
import me.jibai.serverdoctor.command.DoctorCommand;
import me.jibai.serverdoctor.command.DoctorTabCompleter;
import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.config.MessageManager;
import me.jibai.serverdoctor.health.DiagnosisData;
import me.jibai.serverdoctor.health.DiagnosisService;
import me.jibai.serverdoctor.health.HealthScoreService;
import me.jibai.serverdoctor.health.RiskAnalyzer;
import me.jibai.serverdoctor.hopper.HopperScanner;
import me.jibai.serverdoctor.inspect.ChunkInspectService;
import me.jibai.serverdoctor.inspect.PlayerInspectService;
import me.jibai.serverdoctor.logs.LogAnalyzer;
import me.jibai.serverdoctor.redstone.RedstoneListener;
import me.jibai.serverdoctor.redstone.RedstoneTracker;
import me.jibai.serverdoctor.report.AiPromptWriter;
import me.jibai.serverdoctor.report.ExportService;
import me.jibai.serverdoctor.report.MarkdownReportWriter;
import me.jibai.serverdoctor.report.ReportService;
import me.jibai.serverdoctor.snapshot.ServerSnapshot;
import me.jibai.serverdoctor.snapshot.SnapshotService;
import me.jibai.serverdoctor.util.ColorUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * JibaiServerDoctor 插件主类。
 *
 * <p>全核心服务器体检与故障诊断插件。职责：加载配置与消息、组装各功能模块、
 * 注册指令与监听器、启动/停止定时任务、输出启动横幅。具体业务逻辑分散在各模块，
 * 主类只负责装配与生命周期管理。</p>
 *
 * <p>兼容目标：Bukkit / Spigot / Paper / Purpur（1.20+），全程仅使用 Bukkit/Spigot API，
 * 不使用 NMS / 反射 / CraftBukkit 内部类。</p>
 *
 * @author 即白
 */
public class JibaiServerDoctorPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;

    private SnapshotService snapshotService;
    private RedstoneTracker redstoneTracker;
    private HopperScanner hopperScanner;
    private LogAnalyzer logAnalyzer;
    private AlertService alertService;

    private HealthScoreService healthScoreService;
    private RiskAnalyzer riskAnalyzer;
    private DiagnosisService diagnosisService;

    private ReportService reportService;
    private ExportService exportService;

    private ChunkInspectService chunkInspectService;
    private PlayerInspectService playerInspectService;

    @Override
    public void onEnable() {
        // 1. 配置与消息
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        configManager.load();
        messageManager.load();

        // 2. 组装模块
        this.alertService = new AlertService(this, configManager, messageManager);
        this.snapshotService = new SnapshotService(this, configManager);
        this.redstoneTracker = new RedstoneTracker(configManager, messageManager, alertService);
        this.hopperScanner = new HopperScanner(this, configManager, messageManager, alertService);
        this.logAnalyzer = new LogAnalyzer(this, configManager);

        this.healthScoreService = new HealthScoreService(configManager);
        this.riskAnalyzer = new RiskAnalyzer(configManager);
        this.diagnosisService = new DiagnosisService(snapshotService, redstoneTracker, hopperScanner,
                logAnalyzer, healthScoreService, riskAnalyzer);

        MarkdownReportWriter reportWriter = new MarkdownReportWriter(this, configManager);
        AiPromptWriter aiPromptWriter = new AiPromptWriter();
        this.reportService = new ReportService(this, configManager, reportWriter);
        this.exportService = new ExportService(this, configManager, reportWriter, aiPromptWriter);

        this.chunkInspectService = new ChunkInspectService(configManager, redstoneTracker, hopperScanner);
        this.playerInspectService = new PlayerInspectService(configManager, redstoneTracker, hopperScanner);

        // 3. 注册监听器
        if (configManager.isRedstoneEnabled()) {
            getServer().getPluginManager().registerEvents(new RedstoneListener(redstoneTracker), this);
        }

        // 4. 注册指令
        PluginCommand command = getCommand("doctor");
        if (command != null) {
            command.setExecutor(new DoctorCommand(this));
            command.setTabCompleter(new DoctorTabCompleter());
        } else {
            getLogger().warning("未能注册 /doctor 指令，请检查 plugin.yml。");
        }

        // 5. 启动定时任务
        if (configManager.isPluginEnabled()) {
            startTasks();
        } else {
            getLogger().info("插件总开关处于关闭状态（settings.enabled=false），未启动采集任务。");
        }

        // 6. 启动横幅
        printStartupBanner();
    }

    @Override
    public void onDisable() {
        stopTasks();
        getServer().getScheduler().cancelTasks(this);
        if (messageManager != null) {
            getServer().getConsoleSender().sendMessage(messageManager.format("startup.shutdown"));
        } else {
            getLogger().info("JibaiServerDoctor 已关闭。");
        }
    }

    /** 重载配置、消息，并重启采集任务。供 /doctor reload 调用。 */
    public void reloadPlugin() {
        stopTasks();
        configManager.load();
        messageManager.load();
        alertService.reset();
        redstoneTracker.reset();
        hopperScanner.reset();
        if (configManager.isPluginEnabled()) {
            startTasks();
        }
    }

    private void startTasks() {
        snapshotService.start(this::onSnapshot);
        hopperScanner.start();
    }

    private void stopTasks() {
        if (snapshotService != null) {
            snapshotService.stop();
        }
        if (hopperScanner != null) {
            hopperScanner.stop();
        }
    }

    /**
     * 快照采集回调（主线程）：根据最新数据触发健康 / 内存告警。
     * 红石、漏斗告警在各自模块内实时触发，这里只处理整体健康与内存。
     */
    private void onSnapshot(ServerSnapshot snapshot) {
        if (!configManager.isAlertsEnabled()) {
            return;
        }
        // 内存告警
        if (snapshot.getMemoryPercent() >= configManager.getMemoryDangerPercent()) {
            alertService.alert("memory", "alert.memory-high",
                    MessageManager.ph("percent", String.valueOf(snapshot.getMemoryPercent())));
        }
        // 健康评分告警（不含较重的日志分析，保持回调轻量）
        if (configManager.isHealthScoreEnabled()) {
            int score = healthScoreService.score(snapshot, redstoneTracker.getCurrentRisks(),
                    hopperScanner.getLatestRisks(), null);
            if (score < configManager.getDangerScore()) {
                alertService.alert("health", "alert.health-low",
                        MessageManager.ph("score", String.valueOf(score)));
            }
        }
    }

    /** 在控制台输出带颜色的启动横幅（使用 Bukkit ChatColor，不用 ANSI 控制码）。 */
    private void printStartupBanner() {
        String version = getDescription().getVersion();
        // 优先用 messages.yml 中可配置的横幅文本
        getServer().getConsoleSender().sendMessage(messageManager.format("startup.banner-line"));
        getServer().getConsoleSender().sendMessage(messageManager.format("startup.banner-name"));
        getServer().getConsoleSender().sendMessage(messageManager.format("startup.banner-author"));
        getServer().getConsoleSender().sendMessage(messageManager.format("startup.banner-email"));
        getServer().getConsoleSender().sendMessage(
                messageManager.format("startup.banner-version", MessageManager.ph("version", version)));
        getServer().getConsoleSender().sendMessage(messageManager.format("startup.banner-status"));
        getServer().getConsoleSender().sendMessage(messageManager.format("startup.banner-line"));
        // 兜底：即使 messages.yml 被清空，也用固定文案保证横幅要素齐全
        if (messageManager.rawString("startup.banner-name").equals("startup.banner-name")) {
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize(
                    "<green>==============================</green>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize(
                    "<aqua>JibaiServerDoctor 已启动</aqua>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize("<yellow>作者: 即白</yellow>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize(
                    "<gold>邮箱: jibai0517@gamil.com</gold>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize(
                    "<gray>版本: " + version + "</gray>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize(
                    "<green>==============================</green>"));
        }
    }

    /** 便捷：生成一份完整体检数据（含日志分析）。必须在主线程调用。 */
    public DiagnosisData diagnoseFull() {
        return diagnosisService.diagnose(true);
    }

    // -------------------- 供指令访问的 getter --------------------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public SnapshotService getSnapshotService() {
        return snapshotService;
    }

    public DiagnosisService getDiagnosisService() {
        return diagnosisService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public ExportService getExportService() {
        return exportService;
    }

    public ChunkInspectService getChunkInspectService() {
        return chunkInspectService;
    }

    public PlayerInspectService getPlayerInspectService() {
        return playerInspectService;
    }

    public LogAnalyzer getLogAnalyzer() {
        return logAnalyzer;
    }
}
