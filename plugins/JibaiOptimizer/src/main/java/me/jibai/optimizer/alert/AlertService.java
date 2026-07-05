package me.jibai.optimizer.alert;

import me.jibai.optimizer.JibaiOptimizerPlugin;
import me.jibai.optimizer.config.ConfigManager;
import me.jibai.optimizer.config.MessageManager;
import me.jibai.optimizer.util.ColorUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 告警服务。统一处理告警的分发（控制台 + 在线管理员）与日志记录，并支持按 key 限流，
 * 避免同一类告警在压力持续时刷屏。
 *
 * <p>为兼容 Bukkit / Spigot / Paper / Purpur 全核心，消息统一使用已上色的普通字符串
 * （Bukkit {@link org.bukkit.ChatColor}），不依赖 Paper 的 Adventure API。</p>
 */
public class AlertService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JibaiOptimizerPlugin plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    private final AdminNotifyService adminNotify;

    /** 记录每类告警上次触发时间，用于限流。 */
    private final Map<String, Long> lastAlertTime = new HashMap<>();

    public AlertService(JibaiOptimizerPlugin plugin, ConfigManager config,
                        MessageManager messages, AdminNotifyService adminNotify) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.adminNotify = adminNotify;
    }

    /**
     * 立即发送一条告警到控制台与在线管理员，并按配置写日志。
     *
     * @param message 已上色的消息字符串
     */
    public void alert(String message) {
        adminNotify.notifyAdmins(message);
        logIfEnabled(ColorUtil.stripColor(message));
    }

    /**
     * 按消息路径发送告警。
     */
    public void alertMessage(String path, Map<String, String> placeholders) {
        alert(messages.withPrefix(path, placeholders));
    }

    /**
     * 带限流的告警：距离上次同 key 告警不足 cooldownSeconds 时跳过。
     *
     * @param key             限流标识，例如 "memory-warning"
     * @param cooldownSeconds 冷却秒数
     */
    public void alertThrottled(String key, long cooldownSeconds, String message) {
        long now = System.currentTimeMillis();
        Long last = lastAlertTime.get(key);
        if (last != null && now - last < cooldownSeconds * 1000L) {
            return;
        }
        lastAlertTime.put(key, now);
        alert(message);
    }

    /**
     * 带限流的告警（按消息路径）。
     */
    public void alertMessageThrottled(String key, long cooldownSeconds, String path, Map<String, String> placeholders) {
        alertThrottled(key, cooldownSeconds, messages.withPrefix(path, placeholders));
    }

    private void logIfEnabled(String plainText) {
        if (config.getBool("logging.console", true)) {
            plugin.getLogger().info(plainText);
        }
        if (config.getBool("logging.file", false)) {
            writeToFile(plainText);
        }
    }

    private void writeToFile(String plainText) {
        File logFile = new File(plugin.getDataFolder(), "optimizer.log");
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write("[" + LocalDateTime.now().format(TIME_FORMAT) + "] " + plainText + System.lineSeparator());
        } catch (IOException ex) {
            plugin.getLogger().warning("写入日志文件失败: " + ex.getMessage());
        }
    }
}
