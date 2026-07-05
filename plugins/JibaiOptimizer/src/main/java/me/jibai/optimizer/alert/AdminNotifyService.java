package me.jibai.optimizer.alert;

import me.jibai.optimizer.JibaiOptimizerPlugin;
import me.jibai.optimizer.util.PermissionUtil;
import org.bukkit.entity.Player;

/**
 * 管理员通知服务。负责把告警消息发送给所有拥有通知权限的在线管理员。
 *
 * <p>消息为已上色的普通字符串（Bukkit ChatColor），兼容全核心。</p>
 */
public class AdminNotifyService {

    private final JibaiOptimizerPlugin plugin;

    public AdminNotifyService(JibaiOptimizerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 向所有拥有 {@code jibai.optimizer.notify}（或管理员总权限）的在线玩家发送消息。
     * 必须在主线程调用。
     */
    public void notifyAdmins(String message) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission(PermissionUtil.ADMIN) || player.hasPermission(PermissionUtil.NOTIFY)) {
                player.sendMessage(message);
            }
        }
    }
}
