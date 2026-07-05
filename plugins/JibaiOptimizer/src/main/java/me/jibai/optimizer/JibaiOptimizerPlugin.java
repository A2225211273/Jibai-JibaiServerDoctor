package me.jibai.optimizer;

import me.jibai.optimizer.alert.AdminNotifyService;
import me.jibai.optimizer.alert.AlertService;
import me.jibai.optimizer.chunk.ChunkMonitor;
import me.jibai.optimizer.chunk.HopperMonitor;
import me.jibai.optimizer.command.OptimizerCommand;
import me.jibai.optimizer.command.OptimizerTabCompleter;
import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.config.MessageManager;
import me.jibai.optimizer.entity.EntityControlService;
import me.jibai.optimizer.entity.ExpOrbOptimizer;
import me.jibai.optimizer.entity.ItemOptimizer;
import me.jibai.optimizer.entity.SpawnLimiterListener;
import me.jibai.optimizer.monitor.MemoryMonitor;
import me.jibai.optimizer.monitor.PerformanceMonitor;
import me.jibai.optimizer.monitor.ServerSnapshot;
import me.jibai.optimizer.monitor.TpsMonitor;
import me.jibai.optimizer.optimizer.AutoOptimizer;
import me.jibai.optimizer.redstone.RedstoneLimitService;
import me.jibai.optimizer.redstone.RedstoneMonitorListener;
import me.jibai.optimizer.util.ColorUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * JibaiOptimizer 插件主类。
 *
 * <p>职责：加载配置与消息、组装各功能模块、注册指令与监听器、启动/停止定时任务、输出启动横幅。
 * 具体业务逻辑均分散在各模块中，主类只负责装配与生命周期管理。</p>
 *
 * <p>兼容目标：Bukkit / Spigot / Paper / Purpur（1.20+）。全程仅使用 Bukkit/Spigot 通用 API，
 * 不使用 NMS / 反射 / CraftBukkit 内部类，TPS/MSPT 通过调度器自行采样近似计算。</p>
 *
 * @author 即白
 */
public class JibaiOptimizerPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;

    private MemoryMonitor memoryMonitor;
    private TpsMonitor tpsMonitor;
    private PerformanceMonitor performanceMonitor;

    private ItemOptimizer itemOptimizer;
    private ExpOrbOptimizer expOrbOptimizer;
    private EntityControlService entityControlService;

    private AutoOptimizer autoOptimizer;

    private ChunkMonitor chunkMonitor;
    private HopperMonitor hopperMonitor;

    private RedstoneLimitService redstoneLimitService;

    @Override
    public void onEnable() {
        // 1. 加载配置与消息
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        configManager.load();
        messageManager.load();

        // 2. 组装模块
        this.memoryMonitor = new MemoryMonitor(configManager);
        // TPS 采样器（全核心兼容：调度器自行采样，不依赖 Paper API）
        this.tpsMonitor = new TpsMonitor(this);
        this.performanceMonitor = new PerformanceMonitor(this, configManager, memoryMonitor, tpsMonitor);

        AdminNotifyService adminNotify = new AdminNotifyService(this);
        AlertService alertService = new AlertService(this, configManager, messageManager, adminNotify);

        this.itemOptimizer = new ItemOptimizer(configManager);
        this.expOrbOptimizer = new ExpOrbOptimizer(configManager);
        this.entityControlService = new EntityControlService(configManager);

        this.autoOptimizer = new AutoOptimizer(configManager, messageManager, alertService,
                itemOptimizer, expOrbOptimizer, memoryMonitor);

        this.chunkMonitor = new ChunkMonitor(configManager, alertService);
        this.hopperMonitor = new HopperMonitor(this, configManager, alertService);
        this.redstoneLimitService = new RedstoneLimitService(this, configManager, alertService);

        // 3. 注册监听器（只需注册一次，内部逻辑实时读取配置）
        getServer().getPluginManager().registerEvents(
                new SpawnLimiterListener(configManager, tpsMonitor), this);
        getServer().getPluginManager().registerEvents(new RedstoneMonitorListener(redstoneLimitService), this);

        // 4. 注册指令
        PluginCommand command = getCommand("optimizer");
        if (command != null) {
            command.setExecutor(new OptimizerCommand(this));
            command.setTabCompleter(new OptimizerTabCompleter());
        } else {
            getLogger().warning("未能注册 /optimizer 指令，请检查 plugin.yml。");
        }

        // 5. 启动定时任务
        if (configManager.isPluginEnabled()) {
            startTasks();
        } else {
            getLogger().info("插件总开关处于关闭状态（settings.enabled=false），未启动监控任务。");
        }

        // 6. 启动横幅
        printStartupBanner();
    }

    @Override
    public void onDisable() {
        stopTasks();
        // 保险起见取消本插件所有任务
        getServer().getScheduler().cancelTasks(this);
        if (messageManager != null) {
            getServer().getConsoleSender().sendMessage(messageManager.raw("startup.shutdown"));
        } else {
            getLogger().info("JibaiOptimizer 已卸载。");
        }
    }

    /**
     * 重载配置、消息并重启定时任务。供 /optimizer reload 调用。
     */
    public void reloadPlugin() {
        stopTasks();
        configManager.load();
        messageManager.load();
        if (configManager.isPluginEnabled()) {
            startTasks();
        }
    }

    private void startTasks() {
        tpsMonitor.start();
        performanceMonitor.start(this::onSnapshot);
        hopperMonitor.start();
        redstoneLimitService.start();
    }

    private void stopTasks() {
        if (tpsMonitor != null) {
            tpsMonitor.stop();
        }
        if (performanceMonitor != null) {
            performanceMonitor.stop();
        }
        if (hopperMonitor != null) {
            hopperMonitor.stop();
        }
        if (redstoneLimitService != null) {
            redstoneLimitService.stop();
        }
    }

    /**
     * 性能采集回调（主线程）：触发自动优化、内存告警与区块告警。
     */
    private void onSnapshot(ServerSnapshot snapshot) {
        autoOptimizer.check(snapshot);
        chunkMonitor.checkWarning(snapshot.getLoadedChunks());
    }

    /**
     * 在控制台输出带颜色的启动横幅（使用 Bukkit ChatColor，不用复杂 ANSI 控制码，避免乱码）。
     * 横幅内容优先取 messages.yml，被清空时用固定文案兜底，保证插件名/作者/邮箱/版本/状态齐全。
     */
    private void printStartupBanner() {
        String version = getDescription().getVersion();
        boolean hasBannerText = !messageManager.rawString("startup.banner-name").equals("startup.banner-name");
        if (hasBannerText) {
            getServer().getConsoleSender().sendMessage(messageManager.raw("startup.banner-line"));
            getServer().getConsoleSender().sendMessage(messageManager.raw("startup.banner-name"));
            getServer().getConsoleSender().sendMessage(messageManager.raw("startup.banner-author"));
            getServer().getConsoleSender().sendMessage(messageManager.raw("startup.banner-email"));
            getServer().getConsoleSender().sendMessage(
                    messageManager.raw("startup.banner-version", MessageManager.ph("version", version)));
            getServer().getConsoleSender().sendMessage(messageManager.raw("startup.banner-status"));
            getServer().getConsoleSender().sendMessage(messageManager.raw("startup.banner-line"));
        } else {
            // 兜底：即使 messages.yml 被清空，也保证横幅要素齐全
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize("<green>==============================</green>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize("<aqua>JibaiOptimizer 已启动</aqua>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize("<yellow>作者: 即白</yellow>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize("<gold>邮箱: jibai0517@gamil.com</gold>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize("<gray>版本: " + version + "</gray>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize("<green>状态: 已启动</green>"));
            getServer().getConsoleSender().sendMessage(ColorUtil.colorize("<green>==============================</green>"));
        }
    }

    // -------------------- 供指令访问的 getter --------------------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public MemoryMonitor getMemoryMonitor() {
        return memoryMonitor;
    }

    public TpsMonitor getTpsMonitor() {
        return tpsMonitor;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public EntityControlService getEntityControlService() {
        return entityControlService;
    }

    public ChunkMonitor getChunkMonitor() {
        return chunkMonitor;
    }

    public HopperMonitor getHopperMonitor() {
        return hopperMonitor;
    }

    public RedstoneLimitService getRedstoneLimitService() {
        return redstoneLimitService;
    }

    public AutoOptimizer getAutoOptimizer() {
        return autoOptimizer;
    }
}
