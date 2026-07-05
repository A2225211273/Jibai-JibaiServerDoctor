package me.jibai.optimizer.util;

import org.bukkit.command.CommandSender;

/**
 * 权限节点常量与检查工具。
 */
public final class PermissionUtil {

    public static final String ADMIN = "jibai.optimizer.admin";
    public static final String STATUS = "jibai.optimizer.status";
    public static final String MEMORY = "jibai.optimizer.memory";
    public static final String ENTITIES = "jibai.optimizer.entities";
    public static final String CHUNKS = "jibai.optimizer.chunks";
    public static final String CLEAN = "jibai.optimizer.clean";
    public static final String RELOAD = "jibai.optimizer.reload";
    public static final String NOTIFY = "jibai.optimizer.notify";

    private PermissionUtil() {
    }

    /**
     * 判断发送者是否拥有指定权限或管理员总权限。
     */
    public static boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(ADMIN) || sender.hasPermission(permission);
    }
}
