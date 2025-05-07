package cn.com.vortexa.common.util;

import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author helei
 * @since 2025/5/6 14:35
 */
@Slf4j
public class VersionUtil {

    /**
     * 获取目录下的botName 和版本信息map
     *
     * @param jarDir jarDir
     * @return Map<String, String>
     * @throws IOException IOException
     */
    public static Map<String, List<String>> scanJarLibForBotVersionMap(String jarDir) throws IOException {
        Path dir = Paths.get(jarDir);
        if (!Files.exists(dir)) {
            return new HashMap<>();
        }
        Map<String, Set<String>> map = new HashMap<>();
        try (Stream<Path> list = Files.list(dir)) {
            list.filter(path -> path.toString().endsWith(".jar"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        try {
                            Pair<String, String> nameAndVersion = VersionUtil.getBotNameAndVersionFromJarPath(
                                    fileName);
                            map.compute(nameAndVersion.getKey(), (k, v) -> {
                                if (v == null) {
                                    v = new HashSet<>();
                                }
                                v.add(nameAndVersion.getValue());
                                return v;
                            });
                        } catch (Exception e) {
                            log.error("resolve bot version failed, jar name:{}", fileName, e);
                        }
                    });
        }
        Map<String, List<String>> versionMap = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            ArrayList<String> versionList = new ArrayList<>(entry.getValue());
            versionList.sort((v1, v2) -> VersionUtil.compareVersion(v2, v1));
            versionMap.put(entry.getKey(), versionList);
        }
        return versionMap;
    }

    /**
     * 从jar的path中获取botName 和版本信息
     *
     * @param fileName fileName
     * @return Pair<String, String>
     */
    public static Pair<String, String> getBotNameAndVersionFromJarPath(String fileName) {
        String name = fileName.split("\\.jar")[0];
        String[] nameAndVersion = name.split("-v");
        if (nameAndVersion.length == 2) {
            return Pair.of(nameAndVersion[0], "v" + nameAndVersion[1]);
        } else {
            throw new IllegalArgumentException("jar name format error, should be botName-version.jar");
        }
    }

    /**
     * 获取bot jar的文件名
     *
     * @param botName   botName
     * @param version   version
     * @return  String
     */
    public static String getBotJarFileName(String botName, String version) {
        return botName + "-" + version + ".jar";
    }

    /**
     * 版本比较
     *
     * @param v1 v1
     * @param v2 v2
     * @return int
     */
    public static int compareVersion(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            String p1 = i < parts1.length ? parts1[i] : "0";
            String p2 = i < parts2.length ? parts2[i] : "0";
            int cmp = comparePart(p1, p2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static int comparePart(String p1, String p2) {
        try {
            int i1 = Integer.parseInt(p1);
            int i2 = Integer.parseInt(p2);
            return Integer.compare(i1, i2);
        } catch (Exception e) {
            return p1.compareTo(p2);
        }
    }
}
