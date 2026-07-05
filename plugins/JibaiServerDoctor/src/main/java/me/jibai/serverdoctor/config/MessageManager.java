package me.jibai.serverdoctor.config;

import me.jibai.serverdoctor.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * messages.yml 管理器。
 *
 * <p>负责加载可自定义提示文本，支持 {@code {key}} 占位符替换，并通过 {@link ColorUtil}
 * 把颜色标签 / &amp; 码解析为 Bukkit {@link org.bukkit.ChatColor}。所有对外发送方法输出的
 * 都是已上色的普通字符串，兼容全核心与控制台。</p>
 *
 * @author 即白
 */
public class MessageManager {

    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private String prefix = "";

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 加载（或重载）messages.yml。首次调用会释放默认文件。 */
    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
        this.prefix = messages.getString("prefix", "");
    }

    /**
     * 便捷构造占位符 Map，参数按 key1, value1, key2, value2... 交替传入。
     */
    public static Map<String, String> ph(String... kv) {
        if (kv.length == 0) {
            return Collections.emptyMap();
        }
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("占位符参数必须成对出现");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }

    /** 获取原始字符串（未解析颜色、未加前缀），路径不存在时返回路径本身，方便排查。 */
    public String rawString(String path) {
        return messages.getString(path, path);
    }

    /** 解析为已上色字符串，不加前缀。 */
    public String format(String path, Map<String, String> placeholders) {
        return ColorUtil.colorize(applyPlaceholders(rawString(path), placeholders));
    }

    public String format(String path) {
        return format(path, Collections.emptyMap());
    }

    /** 解析为已上色字符串，并在前面拼接消息前缀。 */
    public String formatPrefixed(String path, Map<String, String> placeholders) {
        return ColorUtil.colorize(prefix + applyPlaceholders(rawString(path), placeholders));
    }

    public String formatPrefixed(String path) {
        return formatPrefixed(path, Collections.emptyMap());
    }

    // -------------------- 发送快捷方法 --------------------

    /** 发送带前缀的消息。 */
    public void send(CommandSender target, String path, Map<String, String> placeholders) {
        target.sendMessage(formatPrefixed(path, placeholders));
    }

    public void send(CommandSender target, String path) {
        send(target, path, Collections.emptyMap());
    }

    /** 发送不带前缀的消息（用于多行列表输出）。 */
    public void sendRaw(CommandSender target, String path, Map<String, String> placeholders) {
        target.sendMessage(format(path, placeholders));
    }

    public void sendRaw(CommandSender target, String path) {
        sendRaw(target, path, Collections.emptyMap());
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
