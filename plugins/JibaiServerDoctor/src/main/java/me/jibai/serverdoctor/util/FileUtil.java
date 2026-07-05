package me.jibai.serverdoctor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 文件读写工具。
 *
 * @author 即白
 */
public final class FileUtil {

    private FileUtil() {
    }

    /** 确保目录存在，不存在则创建。 */
    public static void ensureDir(File dir) {
        if (dir != null && !dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    /** 将文本写入文件（UTF-8，覆盖），自动创建父目录。 */
    public static void writeString(File file, String content) throws IOException {
        ensureDir(file.getParentFile());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    /** 复制单个文件（若源存在），自动创建目标父目录。 */
    public static void copyFile(File source, File target) throws IOException {
        if (source == null || !source.exists() || !source.isFile()) {
            return;
        }
        ensureDir(target.getParentFile());
        Files.copy(source.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 高效读取文本文件的末尾若干行，避免把超大日志整体读入内存。
     *
     * <p>做法：从文件尾部按块回退读取，统计换行符，直到收集到 maxLines 行为止。
     * 返回的列表按原文件顺序（从旧到新）排列。</p>
     *
     * @param file     目标文件
     * @param maxLines 最多读取的行数
     * @return 末尾若干行；文件不存在或读取失败时返回空列表
     */
    public static List<String> readLastLines(File file, int maxLines) {
        List<String> result = new ArrayList<>();
        if (file == null || !file.exists() || !file.isFile() || maxLines <= 0) {
            return result;
        }
        // 用顺序读 + 环形缓冲的方式读取末尾若干行。
        // 之所以不用 RandomAccessFile 反向 seek，是因为在 Windows 上日志文件常被
        // 日志框架（log4j2）以独占方式持有，反向随机读易失败；而基于 Files 的
        // BufferedReader 使用共享读通道，兼容性更好。UTF-8 由 reader 正确解码，
        // 也避免了逐字节读取导致的中文乱码。
        Deque<String> ring = new ArrayDeque<>(maxLines);
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (ring.size() >= maxLines) {
                    ring.pollFirst();
                }
                ring.addLast(line);
            }
        } catch (IOException ex) {
            // 读取失败时安静返回已收集内容，交由上层决定如何提示
        }
        result.addAll(ring);
        return result;
    }

    /** 读取整份小文本文件（用于配置等），失败返回空串。 */
    public static String readAll(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        } catch (IOException ex) {
            return "";
        }
        return sb.toString();
    }

    /** 用于日志路径解析：返回服务器根目录下的 logs/latest.log。 */
    public static File resolveLatestLog(Path serverRoot) {
        return serverRoot.resolve("logs").resolve("latest.log").toFile();
    }
}
