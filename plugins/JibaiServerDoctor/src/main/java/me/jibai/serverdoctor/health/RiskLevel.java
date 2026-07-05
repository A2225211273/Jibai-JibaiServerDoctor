package me.jibai.serverdoctor.health;

import org.bukkit.ChatColor;

/**
 * 单条风险的严重程度。
 *
 * <p>用于风险排序（严重的排前面）和展示上色。</p>
 *
 * @author 即白
 */
public enum RiskLevel {

    LOW("低", ChatColor.GREEN, 1),
    MEDIUM("中", ChatColor.YELLOW, 2),
    HIGH("高", ChatColor.GOLD, 3),
    CRITICAL("严重", ChatColor.RED, 4);

    private final String displayName;
    private final ChatColor color;
    private final int weight;

    RiskLevel(String displayName, ChatColor color, int weight) {
        this.displayName = displayName;
        this.color = color;
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    /** 权重，越大越严重，用于排序。 */
    public int getWeight() {
        return weight;
    }
}
