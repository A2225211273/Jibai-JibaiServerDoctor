package me.jibai.serverdoctor.util;

import java.util.regex.Pattern;

/**
 * 敏感信息过滤器。
 *
 * <p>在写入日志摘要 / 诊断包时，把可能的敏感内容（IP、密码、Token、密钥等）替换为占位符，
 * 避免把服务器凭据或玩家隐私一起导出。本类只做“尽力而为”的正则脱敏，不保证覆盖所有情况，
 * 因此诊断包本身也不应包含 server.properties 等敏感配置。</p>
 *
 * @author 即白
 */
public final class SafeTextFilter {

    /** IPv4 地址。 */
    private static final Pattern IPV4 = Pattern.compile(
            "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");

    /** password=xxx / passwd: xxx 等。 */
    private static final Pattern PASSWORD = Pattern.compile(
            "(?i)(pass(word|wd)?\\s*[:=]\\s*)\\S+");

    /** token=xxx / secret: xxx / apikey=xxx 等。 */
    private static final Pattern TOKEN = Pattern.compile(
            "(?i)((token|secret|api[-_]?key|access[-_]?key)\\s*[:=]\\s*)\\S+");

    /** JDBC 连接串中的账号密码。 */
    private static final Pattern JDBC = Pattern.compile(
            "(?i)(jdbc:[^\\s]*?//)([^\\s]+)");

    private SafeTextFilter() {
    }

    /**
     * 对单行文本脱敏。
     *
     * @param line 原始文本；null 视为空串
     * @return 脱敏后的文本
     */
    public static String filter(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        String result = line;
        result = PASSWORD.matcher(result).replaceAll("$1***");
        result = TOKEN.matcher(result).replaceAll("$1***");
        result = JDBC.matcher(result).replaceAll("$1***");
        result = IPV4.matcher(result).replaceAll("***.***.***.***");
        return result;
    }
}
