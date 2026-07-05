package me.jibai.serverdoctor.alert;

import me.jibai.serverdoctor.config.ConfigManager;
import me.jibai.serverdoctor.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理员告警服务。
 *
 * <p>当服务器出现高风险时，向拥有 {@code jibai.doctor.notify} 权限的在线管理员发送提醒，
 * 并在控制台输出。为避免刷屏，同一类告警在冷却时间内只发送一次。</p>
 *
 * <p>告警只是提醒，插件不会因此采取任何破坏性操作。</p>
 *
 * @author 即白
 */
public class AlertService {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final MessageManager messages;

    /** 记录每类告警上次发送的时间戳（毫秒）。key 为告警类别标识。 */
    private final Map<String, Long> lastSentMillis = new ConcurrentHashMap<>();

    public AlertService(JavaPlugin plugin, ConfigManager config, MessageManager messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    /**
     * 发送一条告警。
     *
     * @param category    告警类别，用于冷却去重（如 "memory"、"redstone:world,1,2"）
     * @param messagePath messages.yml 中的路径
     * @param placeholders 占位符
     */
    public void alert(String category, String messagePath, Map<String, String> placeholders) {
        if (!config.isAlertsEnabled()) {
            return;
        }
        if (isOnCooldown(category)) {
            return;
        }
        lastSentMillis.put(category, System.currentTimeMillis());

        String rendered = messages.format(messagePath, placeholders);

        // 通知有权限的在线玩家
        String permission = config.getAlertPermission();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(rendered);
            }
        }

        // 控制台输出
        if (config.isConsoleLogging()) {
            CommandSender console = Bukkit.getConsoleSender();
            console.sendMessage(rendered);
        }
    }

    private boolean isOnCooldown(String category) {
        Long last = lastSentMillis.get(category);
        if (last == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - last;
        return elapsed < config.getAlertCooldownSeconds() * 1000L;
    }

    /** 清空冷却记录（重载时调用）。 */
    public void reset() {
        lastSentMillis.clear();
    }
}
