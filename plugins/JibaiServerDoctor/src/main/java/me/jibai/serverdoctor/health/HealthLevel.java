package me.jibai.serverdoctor.health;

import org.bukkit.ChatColor;

/**
 * 健康评分等级。
 *
 * <p>按开发提示词划分五档，并各自附带展示颜色与中文名。</p>
 *
 * @author 即白
 */
public enum HealthLevel {

    EXCELLENT("优秀", ChatColor.GREEN),
    NORMAL("正常", ChatColor.AQUA),
    ATTENTION("需要关注", ChatColor.YELLOW),
    RISK("存在风险", ChatColor.GOLD),
    CRITICAL("严重异常", ChatColor.RED);

    private final String displayName;
    private final ChatColor color;

    HealthLevel(String displayName, ChatColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    /**
     * 根据 0-100 的评分映射到等级。
     *
     * <pre>
     * 90 - 100：优秀
     * 70 - 89 ：正常
     * 50 - 69 ：需要关注
     * 30 - 49 ：存在风险
     * 0  - 29 ：严重异常
     * </pre>
     */
    public static HealthLevel fromScore(int score) {
        if (score >= 90) {
            return EXCELLENT;
        } else if (score >= 70) {
            return NORMAL;
        } else if (score >= 50) {
            return ATTENTION;
        } else if (score >= 30) {
            return RISK;
        } else {
            return CRITICAL;
        }
    }
}
