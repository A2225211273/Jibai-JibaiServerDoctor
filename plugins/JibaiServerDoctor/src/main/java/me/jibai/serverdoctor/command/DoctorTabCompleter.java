package me.jibai.serverdoctor.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /doctor 指令的 Tab 补全器。
 *
 * @author 即白
 */
public class DoctorTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "status", "report", "export", "inspect", "risks", "errors", "reload", "help");

    private static final List<String> INSPECT_TARGETS = Arrays.asList("chunk", "player");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUB_COMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("inspect")) {
            return filter(INSPECT_TARGETS, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("inspect")
                && args[1].equalsIgnoreCase("player")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return filter(names, args[2]);
        }
        return new ArrayList<>();
    }

    /** 按已输入前缀过滤候选项（忽略大小写）。 */
    private List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
