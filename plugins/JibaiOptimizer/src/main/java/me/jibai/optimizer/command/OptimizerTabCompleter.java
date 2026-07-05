package me.jibai.optimizer.command;

import me.jibai.optimizer.util.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /optimizer 指令的 Tab 补全器。只补全发送者有权限使用的子指令。
 */
public class OptimizerTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "status", "memory", "entities", "chunks", "clean", "redstone", "hoppers", "reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return new ArrayList<>();
        }
        String input = args[0].toLowerCase();
        List<String> result = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(input) && hasPermissionFor(sender, sub)) {
                result.add(sub);
            }
        }
        return result;
    }

    private boolean hasPermissionFor(CommandSender sender, String sub) {
        switch (sub) {
            case "status":
                return PermissionUtil.has(sender, PermissionUtil.STATUS);
            case "memory":
                return PermissionUtil.has(sender, PermissionUtil.MEMORY);
            case "entities":
                return PermissionUtil.has(sender, PermissionUtil.ENTITIES);
            case "chunks":
                return PermissionUtil.has(sender, PermissionUtil.CHUNKS);
            case "clean":
                return PermissionUtil.has(sender, PermissionUtil.CLEAN);
            case "reload":
                return PermissionUtil.has(sender, PermissionUtil.RELOAD);
            case "redstone":
            case "hoppers":
                // 红石/漏斗统计归入 status 权限
                return PermissionUtil.has(sender, PermissionUtil.STATUS);
            default:
                return false;
        }
    }
}
