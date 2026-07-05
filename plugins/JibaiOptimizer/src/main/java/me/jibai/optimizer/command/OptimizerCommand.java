package me.jibai.optimizer.command;

import me.jibai.optimizer.JibaiOptimizerPlugin;
import me.jibai.optimizer.chunk.HopperMonitor;
import me.jibai.optimizer.config.MessageManager;
import me.jibai.optimizer.entity.EntityControlService;
import me.jibai.optimizer.monitor.ServerSnapshot;
import me.jibai.optimizer.optimizer.OptimizationResult;
import me.jibai.optimizer.util.FormatUtil;
import me.jibai.optimizer.util.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/**
 * /optimizer 主指令执行器。分发各子指令并进行权限校验。
 * 所有 Bukkit API 访问均在主线程（指令线程即主线程）执行。
 */
public class OptimizerCommand implements CommandExecutor {

    private final JibaiOptimizerPlugin plugin;
    private final MessageManager msg;

    public OptimizerCommand(JibaiOptimizerPlugin plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status":
                handleStatus(sender);
                break;
            case "memory":
                handleMemory(sender);
                break;
            case "entities":
                handleEntities(sender);
                break;
            case "chunks":
                handleChunks(sender);
                break;
            case "clean":
                handleClean(sender);
                break;
            case "redstone":
                handleRedstone(sender);
                break;
            case "hoppers":
                handleHoppers(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                msg.send(sender, "unknown-command");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        msg.sendRaw(sender, "help.header");
        msg.sendRaw(sender, "help.status");
        msg.sendRaw(sender, "help.memory");
        msg.sendRaw(sender, "help.entities");
        msg.sendRaw(sender, "help.chunks");
        msg.sendRaw(sender, "help.clean");
        msg.sendRaw(sender, "help.redstone");
        msg.sendRaw(sender, "help.hoppers");
        msg.sendRaw(sender, "help.reload");
    }

    private void handleStatus(CommandSender sender) {
        if (noPermission(sender, PermissionUtil.STATUS)) {
            return;
        }
        ServerSnapshot s = plugin.getPerformanceMonitor().getLatest();
        msg.sendRaw(sender, "status.header");
        msg.sendRaw(sender, "status.tps", MessageManager.ph("tps", FormatUtil.round2(s.getTps())));
        msg.sendRaw(sender, "status.mspt", MessageManager.ph("mspt", FormatUtil.round2(s.getMspt())));
        msg.sendRaw(sender, "status.players", MessageManager.ph("players", String.valueOf(s.getOnlinePlayers())));
        msg.sendRaw(sender, "status.worlds", MessageManager.ph("worlds", String.valueOf(s.getWorldCount())));
        msg.sendRaw(sender, "status.memory", MessageManager.ph(
                "used", String.valueOf(s.getUsedMemoryMb()),
                "max", String.valueOf(s.getMaxMemoryMb()),
                "percent", FormatUtil.round1(s.getMemoryPercent())));
        msg.sendRaw(sender, "status.entities", MessageManager.ph("entities", String.valueOf(s.getTotalEntities())));
        msg.sendRaw(sender, "status.items", MessageManager.ph("items", String.valueOf(s.getTotalItems())));
        msg.sendRaw(sender, "status.orbs", MessageManager.ph("orbs", String.valueOf(s.getTotalExpOrbs())));
        msg.sendRaw(sender, "status.chunks", MessageManager.ph("chunks", String.valueOf(s.getLoadedChunks())));
    }

    private void handleMemory(CommandSender sender) {
        if (noPermission(sender, PermissionUtil.MEMORY)) {
            return;
        }
        var mem = plugin.getMemoryMonitor();
        msg.sendRaw(sender, "memory.header");
        msg.sendRaw(sender, "memory.used", MessageManager.ph("used", String.valueOf(mem.getUsedMb())));
        msg.sendRaw(sender, "memory.max", MessageManager.ph("max", String.valueOf(mem.getMaxMb())));
        msg.sendRaw(sender, "memory.free", MessageManager.ph("free", String.valueOf(mem.getFreeMb())));
        msg.sendRaw(sender, "memory.percent", MessageManager.ph("percent", FormatUtil.round1(mem.getPercent())));
    }

    private void handleEntities(CommandSender sender) {
        if (noPermission(sender, PermissionUtil.ENTITIES)) {
            return;
        }
        msg.sendRaw(sender, "entities.header");
        int total = 0;
        Map<String, EntityControlService.WorldEntityStats> stats =
                plugin.getEntityControlService().collectStats();
        for (EntityControlService.WorldEntityStats st : stats.values()) {
            total += st.entities;
            msg.sendRaw(sender, "entities.world-line", MessageManager.ph(
                    "world", st.worldName,
                    "entities", String.valueOf(st.entities),
                    "monsters", String.valueOf(st.monsters),
                    "animals", String.valueOf(st.animals),
                    "items", String.valueOf(st.items)));
        }
        msg.sendRaw(sender, "entities.total", MessageManager.ph("entities", String.valueOf(total)));
    }

    private void handleChunks(CommandSender sender) {
        if (noPermission(sender, PermissionUtil.CHUNKS)) {
            return;
        }
        msg.sendRaw(sender, "chunks.header");
        int total = 0;
        Map<String, Integer> stats = plugin.getChunkMonitor().collectStats();
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            total += entry.getValue();
            msg.sendRaw(sender, "chunks.world-line", MessageManager.ph(
                    "world", entry.getKey(),
                    "chunks", String.valueOf(entry.getValue())));
        }
        msg.sendRaw(sender, "chunks.total", MessageManager.ph("chunks", String.valueOf(total)));
    }

    private void handleClean(CommandSender sender) {
        if (noPermission(sender, PermissionUtil.CLEAN)) {
            return;
        }
        OptimizationResult result = plugin.getAutoOptimizer().runManual();
        msg.send(sender, "optimizer.manual-done", MessageManager.ph(
                "items", String.valueOf(result.getCleanedItems()),
                "orbs", String.valueOf(result.getCleanedOrbs()),
                "merged", String.valueOf(result.getMergedGroups())));
    }

    private void handleRedstone(CommandSender sender) {
        if (noPermission(sender, PermissionUtil.STATUS)) {
            return;
        }
        msg.sendRaw(sender, "redstone.header");
        Map<String, Integer> snapshot = plugin.getRedstoneLimitService().getTracker().snapshot();
        if (snapshot.isEmpty()) {
            msg.sendRaw(sender, "redstone.empty");
            return;
        }
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            msg.sendRaw(sender, "redstone.line", MessageManager.ph(
                    "chunk", entry.getKey(),
                    "count", String.valueOf(entry.getValue())));
        }
    }

    private void handleHoppers(CommandSender sender) {
        if (noPermission(sender, PermissionUtil.STATUS)) {
            return;
        }
        msg.sendRaw(sender, "hopper.header");
        // 只读取最近一次分批扫描的缓存结果，不触发即时全量扫描（问题 5 修复）
        if (!plugin.getHopperMonitor().hasCache()) {
            msg.sendRaw(sender, "hopper.no-cache");
            return;
        }
        List<HopperMonitor.ChunkHopperCount> over = plugin.getHopperMonitor().getCachedOver();
        if (over.isEmpty()) {
            msg.sendRaw(sender, "hopper.empty");
            return;
        }
        for (HopperMonitor.ChunkHopperCount item : over) {
            msg.sendRaw(sender, "hopper.line", MessageManager.ph(
                    "chunk", item.label,
                    "count", String.valueOf(item.count)));
        }
    }

    private void handleReload(CommandSender sender) {
        if (noPermission(sender, PermissionUtil.RELOAD)) {
            return;
        }
        plugin.reloadPlugin();
        msg.send(sender, "reload-success");
    }

    /**
     * 无权限时发送提示并返回 true。
     */
    private boolean noPermission(CommandSender sender, String permission) {
        if (!PermissionUtil.has(sender, permission)) {
            msg.send(sender, "no-permission");
            return true;
        }
        return false;
    }
}
