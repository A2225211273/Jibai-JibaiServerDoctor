package me.jibai.serverdoctor.health;

/**
 * 一条风险记录。
 *
 * <p>每条风险都包含：严重程度、面向服主的原因说明、以及处理建议。
 * 这与开发提示词要求“每条异常都要给出原因和建议、让新手服主看得懂”一致。</p>
 *
 * @author 即白
 */
public class Risk {

    private final RiskLevel level;
    private final String message;
    private final String suggestion;

    public Risk(RiskLevel level, String message, String suggestion) {
        this.level = level;
        this.message = message;
        this.suggestion = suggestion;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestion() {
        return suggestion;
    }
}
