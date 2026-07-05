package me.jibai.optimizer.config;

import me.jibai.optimizer.JibaiOptimizerPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 主配置文件（config.yml）管理器。
 *
 * <p>负责加载 config.yml、在首次启用时保存默认配置，并对外提供带默认值的类型化读取。
 * 所有 getter 在配置缺失时返回安全默认值，避免因为配置项被误删而导致插件报错。</p>
 */
public class ConfigManager {

    private final JibaiOptimizerPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JibaiOptimizerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载（或重载）配置文件。首次调用会释放默认 config.yml。
     */
    public void load() {
        // 若插件目录下不存在 config.yml，则从 jar 内复制一份默认配置
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration raw() {
        return config;
    }

    // -------------------- 便捷类型化读取（均带默认值） --------------------

    public boolean getBool(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public long getLong(String path, long def) {
        return config.getLong(path, def);
    }

    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    // -------------------- 常用总开关 --------------------

    /** 插件总开关。 */
    public boolean isPluginEnabled() {
        return getBool("settings.enabled", true);
    }
}
