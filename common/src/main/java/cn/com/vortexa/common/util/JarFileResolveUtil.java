package cn.com.vortexa.common.util;

import cn.com.vortexa.common.dto.BotMetaInfo;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author helei
 * @since 2025/5/7 16:08
 */
@Slf4j
public class JarFileResolveUtil {
    public static final String BOT_META_INF_FILE_NAME = "bot-meta-info.yaml";
    public static final String BOT_ICON_FILE_NAME = "icon.png";
    public static final List<String> BOT_META_INFO_PREFIX = List.of("vortexa", "botMetaInfo");

    public static Map<String, BotMetaInfo> tryExtractJarAndResolveBotMetaInfo(
            List<String> jarFileNames
    ) throws IOException {
        return tryExtractJarAndResolveBotMetaInfo(jarFileNames, false);
    }

    /**
     * 解压jar包并解析bot meta info
     *
     * @throws IOException IOException
     */
    public static Map<String, BotMetaInfo> tryExtractJarAndResolveBotMetaInfo(
            List<String> jarFileNames, boolean loadIcon
    ) throws IOException {
        Map<String, BotMetaInfo> metaInfoMap = new HashMap<>();
        if (CollUtil.isEmpty(jarFileNames)) {
            return metaInfoMap;
        }
        for (String botJarFileName : jarFileNames) {
            String jarLibraryPath = FileUtil.getLibraryPath(botJarFileName);
            Path jarFilePath = Paths.get(FileUtil.getJarFilePath(botJarFileName.replace(".jar", "")));
            if (!Files.exists(jarFilePath)) {
                FileUtil.extractJar(
                        jarLibraryPath,
                        jarFilePath.toString()
                );
            } else {
                log.info("jar[{}] extracted, skip extract it", botJarFileName);
            }

            // 解析文件夹
            log.info("start resolve bot meta info config from dir[{}]", jarFilePath);
            try (Stream<Path> walk = Files.walk(jarFilePath, 5)) {
                walk.filter(Files::isDirectory).forEach(dir -> {
                    resolveBotDir(dir, jarLibraryPath, botJarFileName, metaInfoMap, loadIcon);
                });
            }
        }
        return metaInfoMap;
    }

    /**
     * 处理bot 文件夹
     *
     * @param dir dir
     * @param jarLibraryPath jarLibraryPath
     * @param botJarFileName botJarFileName
     * @param metaInfoMap metaInfoMap
     * @param loadIcon  loadIcon
     */
    private static void resolveBotDir(
            Path dir, String jarLibraryPath, String botJarFileName, Map<String, BotMetaInfo> metaInfoMap, boolean loadIcon
    ) {
        Pair<String, String> pair = VersionUtil.getBotNameAndVersionFromJarPath(botJarFileName);
        String loadBotName = pair.getKey();
        String version = pair.getValue();

        Path configFilePath = dir.resolve(BOT_META_INF_FILE_NAME);
        if (Files.exists(configFilePath)) {
            BotMetaInfo metaInfo = YamlConfigLoadUtil.load(configFilePath.toFile(),
                    BOT_META_INFO_PREFIX, BotMetaInfo.class);
            // 配置文件校验
            if (metaInfo == null) {
                throw new IllegalArgumentException(
                        "bot meta info file [" + BOT_META_INF_FILE_NAME + "] illegal");
            }
            // 设置bot资源目录
            metaInfo.setResourceDir(dir.toString());
            // 设置所在jar包路径
            metaInfo.setClassJarPath(jarLibraryPath);

            if (loadIcon) {
                try {
                    metaInfo.setIcon(
                            ImageBase64Util.pngToBase64DataUrl(
                                    metaInfo.getResourceDir() + File.separator + BOT_ICON_FILE_NAME)
                    );
                } catch (IOException e) {
                    log.warn("bot[{}] icon png image load fail, {}", loadBotName, e.getMessage());
                }
            }

            metaInfo.setVersion_code(version);
            metaInfoMap.put(metaInfo.getBotName(), metaInfo);
            log.info("botName[{}]-[{}] meta info loaded", metaInfo.getBotName(), version);
        }
    }
}
