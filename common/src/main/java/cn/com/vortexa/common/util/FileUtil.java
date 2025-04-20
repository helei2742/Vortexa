package cn.com.vortexa.common.util;

import cn.com.vortexa.common.constants.FilePathType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileUtil {

    public static final String BASE_DIR_NAME = "botData";

    public static final List<String> CONFIG_DIR_BOT_PATH = List.of("config", "bot");

    public static final List<String> CONFIG_DIR_APP_PATH = List.of("config", "app");


    /**
     * app资源根目录
     */
    public static final String RESOURCE_ROOT_DIR = System.getProperty("user.dir") + File.separator + BASE_DIR_NAME;

    /**
     * class资源
     *
     * @return path
     * @throws IOException exception
     */
    public static String getCompileClassResourceDir() throws IOException {
        Path path = Paths.get(RESOURCE_ROOT_DIR + File.separator + "classes");
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        return path.toString();
    }

    /**
     * class资源
     *
     * @return path
     * @throws IOException exception
     */
    public static String getCompileClassResource(String subDir) throws IOException {
        String dir = getCompileClassResourceDir();
        Path path = Paths.get(dir + File.separator + subDir);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        return path.toString();
    }

    /**
     * 获取数据库文件的dir
     *
     * @return String
     */
    public static String getDBResourceDir() {
        return RESOURCE_ROOT_DIR + File.separator + "db";
    }

    /**
     * 获取资源路径
     *
     * @param path     子路径
     * @param fileName 文件名
     * @return 绝对路径
     */
    public static String getAppResourcePath(List<String> path, String fileName) {
        StringBuilder sb = new StringBuilder(RESOURCE_ROOT_DIR);

        for (String p : path) {
            sb.append(File.separator).append(p);
        }
        return sb.append(File.separator).append(fileName).toString();
    }

    /**
     * 获取app配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceConfigPath() {
        return RESOURCE_ROOT_DIR + File.separator + "config";
    }

    /**
     * 获取app配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceAppConfigPath() {
        return RESOURCE_ROOT_DIR + File.separator + String.join(File.separator, CONFIG_DIR_APP_PATH);
    }

    /**
     * 获取系统配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceSystemConfigPath() {
        return RESOURCE_ROOT_DIR + File.separator + String.join(File.separator, CONFIG_DIR_BOT_PATH);
    }

    /**
     * 获取data目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceDataPath() {
        return RESOURCE_ROOT_DIR + File.separator + "data";
    }

    /**
     * 获取script node config目录
     *
     * @return 配置目录绝对路径
     */
    public static String getScriptNodeConfigPath() {
        return getAppResourceConfigPath() + File.separator + "script_node";
    }

    /**
     * 依赖目录
     *
     * @return String
     */
    public static String getLibraryPath() {
        return System.getProperty("user.dir") + File.separator + "lib";
    }


    /**
     * 生成绝对路径
     *
     * @param patternPath             patternPath
     * @param botInstanceResourcePath botInstanceResourcePath
     * @return 绝对路径
     */
    public static String generateAbsPath(String patternPath, String botInstanceResourcePath) {
        FilePathType filePathType = FilePathType.resolveFilePathType(patternPath);
        return switch (filePathType) {
            case absolute -> {
                if (patternPath.startsWith(filePathType.name())) {
                    yield patternPath.replace("absolute:", "");
                }
                yield patternPath;
            }
            case instance_resource ->
                    patternPath.replace("instance_resource:", botInstanceResourcePath + File.separator);
            case app_resource -> patternPath.replace("app_resource:", RESOURCE_ROOT_DIR + File.separator);
            case app_resource_config ->
                    patternPath.replace("app_resource_config:", getAppResourceAppConfigPath() + File.separator);
            case app_resource_data ->
                    patternPath.replace("app_resource_data:", getAppResourceDataPath() + File.separator);
        };
    }

    /**
     * 保存
     */
    public static void saveJSONStringContext(Path filePath, String jsonContext) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(jsonContext);
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建日志目录
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     */
    public static String createLogsDir(String scriptNodeName, String botKey) throws IOException {
        Path path = Paths.get(RESOURCE_ROOT_DIR, "logs", scriptNodeName, botKey);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        return path.toString();
    }
}
