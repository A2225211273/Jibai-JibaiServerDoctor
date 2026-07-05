package me.jibai.serverdoctor.command;

import me.jibai.serverdoctor.JibaiServerDoctorPlugin;
import me.jibai.serverdoctor.config.MessageManager;
import me.jibai.serverdoctor.health.DiagnosisData;
import me.jibai.serverdoctor.health.Risk;
import me.jibai.serverdoctor.inspect.InspectResult;
import me.jibai.serverdoctor.logs.LogErrorSummary;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

/**
 * /doctor 指令执行器。
 *
 * <p>处理所有子指令：status / report / export / inspect / risks / errors / reload / help。
 * 涉及游戏对象（世界、实体、区块、玩家）的读取都在主线程执行；报告 / 诊断包的文件写入
 * 放到异步线程，避免阻塞主线程造成卡顿。</p>
 *
 * @author 即白
 */
public class DoctorCommand implements CommandExecutor {

    private final JibaiServerDoctorPlugin plugin;
    private final MessageManager msg;

    public DoctorCommand(JibaiServerDoctorPlugin plugin) {
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
            case "report":
                handleReport(sender);
                break;
            case "export":
                handleExport(sender);
                break;
            case "inspect":
                handleInspect(sender, args);
                break;
            case "risks":
                handleRisks(sender);
                break;
            case "errors":
                handleErrors(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "help":
                sendHelp(sender);
                break;
            default:
                msg.send(sender, "unknown-command");
        }
        return true;
    }

    // -------------------- status --------------------

    private void handleStatus(CommandSender sender) {
        if (noPermission(sender, "jibai.doctor.status")) {
            return;
        }
        // status 不做较重的日志分析，保持轻量
        DiagnosisData data = plugin.getDiagnosisService().diagnose(false);
        var s = data.getSnapshot();

        msg.sendRaw(sender, "status.header");
        msg.sendRaw(sender, "status.score", MessageManager.ph(
                "score", String.valueOf(data.getScore()),
                "level", data.getLevel().getDisplayName()));
        msg.sendRaw(sender, "status.players", MessageManager.ph(
                "players", String.valueOf(s.getOnlinePlayers()),
                "worlds", String.valueOf(s.getWorldCount())));
        msg.sendRaw(sender, "status.memory", MessageManager.ph(
                "used", String.valueOf(s.getUsedMemoryMb()),
                "max", String.valueOf(s.getMaxMemoryMb()),
                "percent", String.valueOf(s.getMemoryPercent())));
        msg.sendRaw(sender, "status.entities", MessageManager.ph(
                "entities", String.valueOf(s.getTotalEntities()),
                "items", String.valueOf(s.getTotalDroppedItems()),
                "orbs", String.valueOf(s.getTotalExpOrbs())));
        msg.sendRaw(sender, "status.chunks", MessageManager.ph(
                "chunks", String.valueOf(s.getTotalLoadedChunks())));
        msg.sendRaw(sender, "status.tick", MessageManager.ph(
                "tick", String.valueOf(Math.round(s.getApproxTickDelayMs()))));
        msg.sendRaw(sender, "status.risks", MessageManager.ph(
                "risks", String.valueOf(data.getRisks().size())));
        msg.sendRaw(sender, "status.footer");
    }

    // -------------------- report --------------------

    private void handleReport(CommandSender sender) {
        if (noPermission(sender, "jibai.doctor.report")) {
            return;
        }
        // 数据采集（主线程）——本方法就在主线程执行指令
        DiagnosisData data = plugin.getDiagnosisService().diagnose(true);
        // 文件写入放异步，完成后回主线程反馈
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File file = plugin.getReportService().generate(data);
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        msg.send(sender, "report.generated",
                                MessageManager.ph("file", relativePath(file))));
            } catch (Exception ex) {
                plugin.getLogger().warning("生成报告失败：" + ex.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        msg.send(sender, "report.failed"));
            }
        });
    }

    // -------------------- export --------------------

    private void handleExport(CommandSender sender) {
        if (noPermission(sender, "jibai.doctor.export")) {
            return;
        }
        DiagnosisData data = plugin.getDiagnosisService().diagnose(true);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File folder = plugin.getExportService().export(data);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    msg.send(sender, "export.generated",
                            MessageManager.ph("folder", relativePath(folder)));
                    msg.send(sender, "export.hint");
                });
            } catch (Exception ex) {
                plugin.getLogger().warning("导出诊断包失败：" + ex.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        msg.send(sender, "export.failed"));
            }
        });
    }

    // -------------------- inspect --------------------

    private void handleInspect(CommandSender sender, String[] args) {
        if (noPermission(sender, "jibai.doctor.inspect")) {
            return;
        }
        if (args.length < 2) {
            msg.sendRaw(sender, "help.inspect-chunk");
            msg.sendRaw(sender, "help.inspect-player");
            return;
        }
        String target = args[1].toLowerCase();
        if (target.equals("chunk")) {
            if (!(sender instanceof Player)) {
                msg.send(sender, "only-player");
                return;
            }
            Player player = (Player) sender;
            Chunk chunk = player.getLocation().getChunk();
            InspectResult result = plugin.getChunkInspectService().inspect(chunk);
            renderChunkInspect(sender, result);
        } else if (target.equals("player")) {
            if (args.length < 3) {
                msg.sendRaw(sender, "help.inspect-player");
                return;
            }
            Player targetPlayer = plugin.getServer().getPlayerExact(args[2]);
            if (targetPlayer == null) {
                msg.send(sender, "player-not-found", MessageManager.ph("player", args[2]));
                return;
            }
            InspectResult result = plugin.getPlayerInspectService().inspect(targetPlayer);
            renderPlayerInspect(sender, targetPlayer.getName(), result);
        } else {
            msg.sendRaw(sender, "help.inspect-chunk");
            msg.sendRaw(sender, "help.inspect-player");
        }
    }

    private void renderChunkInspect(CommandSender sender, InspectResult r) {
        msg.sendRaw(sender, "inspect.chunk-header", MessageManager.ph(
                "world", r.getWorldName(), "x", String.valueOf(r.getCenterX()),
                "z", String.valueOf(r.getCenterZ())));
        renderInspectBody(sender, r);
    }

    private void renderPlayerInspect(CommandSender sender, String playerName, InspectResult r) {
        msg.sendRaw(sender, "inspect.player-header", MessageManager.ph(
                "player", playerName,
                "radius", String.valueOf(plugin.getPlayerInspectService().getBlockRadius())));
        renderInspectBody(sender, r);
    }

    private void renderInspectBody(CommandSender sender, InspectResult r) {
        msg.sendRaw(sender, "inspect.entities", MessageManager.ph(
                "entities", String.valueOf(r.getEntities())));
        msg.sendRaw(sender, "inspect.items", MessageManager.ph(
                "items", String.valueOf(r.getDroppedItems()),
                "orbs", String.valueOf(r.getExpOrbs())));
        msg.sendRaw(sender, "inspect.monsters", MessageManager.ph(
                "monsters", String.valueOf(r.getMonsters()),
                "animals", String.valueOf(r.getAnimals())));
        msg.sendRaw(sender, "inspect.hoppers", MessageManager.ph(
                "hoppers", String.valueOf(r.getHoppers())));
        msg.sendRaw(sender, "inspect.redstone", MessageManager.ph(
                "redstone", String.valueOf(r.getRedstoneCount()),
                "window", String.valueOf(plugin.getConfigManager().getRedstoneWindowSeconds())));
        msg.sendRaw(sender, "inspect.chunks", MessageManager.ph(
                "chunks", String.valueOf(r.getActiveChunks())));
        msg.sendRaw(sender, "inspect.level", MessageManager.ph(
                "color", r.getLevel().getColor().toString(),
                "level", r.getLevel().getDisplayName()));
    }

    // -------------------- risks --------------------

    private void handleRisks(CommandSender sender) {
        if (noPermission(sender, "jibai.doctor.risks")) {
            return;
        }
        DiagnosisData data = plugin.getDiagnosisService().diagnose(true);
        List<Risk> risks = data.getRisks();
        if (risks.isEmpty()) {
            msg.send(sender, "risk.none");
            return;
        }
        msg.sendRaw(sender, "risk.header");
        int index = 1;
        for (Risk risk : risks) {
            msg.sendRaw(sender, "risk.item", MessageManager.ph(
                    "index", String.valueOf(index++),
                    "color", risk.getLevel().getColor().toString(),
                    "message", risk.getMessage()));
            msg.sendRaw(sender, "risk.suggestion", MessageManager.ph(
                    "suggestion", risk.getSuggestion()));
        }
    }

    // -------------------- errors --------------------

    private void handleErrors(CommandSender sender) {
        if (noPermission(sender, "jibai.doctor.errors")) {
            return;
        }
        // 日志分析是纯文件读取，放异步执行
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            LogErrorSummary summary = plugin.getLogAnalyzer().analyze();
            plugin.getServer().getScheduler().runTask(plugin, () -> renderErrors(sender, summary));
        });
    }

    private void renderErrors(CommandSender sender, LogErrorSummary summary) {
        if (summary == null || !summary.isLogAvailable() || !summary.hasProblems()) {
            msg.send(sender, "errors.none");
            return;
        }
        int limit = 10;
        msg.sendRaw(sender, "errors.header", MessageManager.ph("limit", String.valueOf(limit)));
        msg.sendRaw(sender, "errors.summary", MessageManager.ph(
                "errors", String.valueOf(summary.getErrorCount()),
                "warns", String.valueOf(summary.getWarnCount())));
        int index = 1;
        for (LogErrorSummary.Entry entry : summary.getEntries()) {
            if (index > limit) {
                break;
            }
            String pluginName = entry.getPluginName() == null || entry.getPluginName().isEmpty()
                    ? "未知" : entry.getPluginName();
            msg.sendRaw(sender, "errors.item", MessageManager.ph(
                    "index", String.valueOf(index++),
                    "count", String.valueOf(entry.getCount()),
                    "plugin", pluginName,
                    "message", entry.getMessage()));
        }
    }

    // -------------------- reload --------------------

    private void handleReload(CommandSender sender) {
        if (noPermission(sender, "jibai.doctor.reload")) {
            return;
        }
        plugin.reloadPlugin();
        msg.send(sender, "reload-success");
    }

    // -------------------- help --------------------

    private void sendHelp(CommandSender sender) {
        msg.sendRaw(sender, "help.header");
        msg.sendRaw(sender, "help.status");
        msg.sendRaw(sender, "help.report");
        msg.sendRaw(sender, "help.export");
        msg.sendRaw(sender, "help.inspect-chunk");
        msg.sendRaw(sender, "help.inspect-player");
        msg.sendRaw(sender, "help.risks");
        msg.sendRaw(sender, "help.errors");
        msg.sendRaw(sender, "help.reload");
        msg.sendRaw(sender, "help.footer");
    }

    // -------------------- 工具方法 --------------------

    private boolean noPermission(CommandSender sender, String permission) {
        // 管理员总权限或对应子权限之一即可
        if (sender.hasPermission("jibai.doctor.admin") || sender.hasPermission(permission)) {
            return false;
        }
        msg.send(sender, "no-permission");
        return true;
    }

    /** 把绝对路径转成相对服务器根目录的相对路径，便于展示。 */
    private String relativePath(File file) {
        try {
            File serverRoot = plugin.getDataFolder().getParentFile().getParentFile();
            String base = serverRoot.getAbsolutePath();
            String path = file.getAbsolutePath();
            if (path.startsWith(base)) {
                String rel = path.substring(base.length());
                return rel.startsWith(File.separator) ? rel.substring(1) : rel;
            }
            return path;
        } catch (Exception ex) {
            return file.getName();
        }
    }
}
