package cn.com.vortexa.common.util;

import cn.com.vortexa.common.constants.FilePathType;

import java.io.*;
        import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileUtil {
    public static String LIBRARY_DIR_NAME = "lib";
    public static String JAR_FILE_DIR_NAME = "jarFile";

    public static final String BASE_DIR_NAME = "vortexa-data";

    public static final List<String> CONFIG_DIR_BOT_PATH = List.of("config", "bot");

    public static final List<String> CONFIG_DIR_APP_PATH = List.of("config", "app");

    public static final String USER_DIR = System.getProperty("user.dir");
    /**
     * app资源根目录
     */
    public static final String RESOURCE_ROOT_DIR = USER_DIR + File.separator + BASE_DIR_NAME;

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
    public static String getAppResourceConfigDir() {
        return RESOURCE_ROOT_DIR + File.separator + "config";
    }

    /**
     * 获取app配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceAppConfigDir() {
        return RESOURCE_ROOT_DIR + File.separator + String.join(File.separator, CONFIG_DIR_APP_PATH);
    }

    /**
     * 获取系统配置目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceSystemConfigDir() {
        return RESOURCE_ROOT_DIR + File.separator + String.join(File.separator, CONFIG_DIR_BOT_PATH);
    }

    /**
     * 获取data目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceDataDir() {
        return RESOURCE_ROOT_DIR + File.separator + "data";
    }

    /**
     * 获取data目录
     *
     * @return 配置目录绝对路径
     */
    public static String getAppResourceDataPath(String fileName) {
        return getAppResourceDataDir() + File.separator + fileName;
    }

    /**
     * 获取script node config目录
     *
     * @return 配置目录绝对路径
     */
    public static String getScriptNodeConfigDir() {
        return getAppResourceConfigDir() + File.separator + "script_node";
    }

    /**
     * 获取script node config file
     *
     * @return 配置目录绝对路径
     */
    public static Path getScriptNodeConfig(String fileName) throws IOException {
        Path path = Paths.get(getScriptNodeConfigDir() + File.separator + fileName);
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
        }
        return path;
    }

    /**
     * 依赖目录
     *
     * @return String
     */
    public static String getLibraryDir() {
        return USER_DIR + File.separator + LIBRARY_DIR_NAME;
    }
    /**
     * 依赖文件
     *
     * @return String
     */
    public static String getLibraryPath(String fileName) {
        return getLibraryDir() + File.separator + fileName + (fileName.endsWith(".jar") ? "" : ".jar");
    }

    /**
     * 依赖文件
     *
     * @return String
     */
    public static Path getAndCreateLibraryPath(String fileName) throws IOException {
        Path path = Paths.get(getLibraryPath(fileName));
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
        }
        return path;
    }

    /**
     * 依赖目录
     *
     * @return String
     */
    public static String getJarFileDir() {
        return RESOURCE_ROOT_DIR + File.separator + JAR_FILE_DIR_NAME;
    }

    /**
     * 依赖文件
     *
     * @return String
     */
    public static String getJarFilePath(String... fileName) {
        return getJarFileDir() + File.separator + String.join(File.separator, fileName);
    }

    /**
     * bot实例配置目录
     *
     * @return String
     */
    public static String getBotInstanceConfigDir() {
        return USER_DIR + File.separator + "instance";
    }

    /**
     * bot日志目录
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @return  String
     */
    public static String getBotInstanceLogsDir(String scriptNodeName, String botKey) {
        return RESOURCE_ROOT_DIR + File.separator + "logs" + File.separator + scriptNodeName + File.separator + botKey;
    }

    /**
     * bot日志目录
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @return  String
     */
    public static String getBotInstanceCurrentLogPath(String scriptNodeName, String botKey) {
        return getBotInstanceLogsDir(scriptNodeName, botKey) + File.separator +  botKey + ".log";
    }

    /**
     * 生成绝对路径
     *
     * @param patternPath     patternPath
     * @param botResourcePath botResourcePath
     * @return 绝对路径
     */
    public static String generateAbsPath(String patternPath, String botResourcePath) {
        FilePathType filePathType = FilePathType.resolveFilePathType(patternPath);
        if (filePathType == null) return null;
        return switch (filePathType) {
            case absolute -> {
                if (patternPath.startsWith(filePathType.name())) {
                    yield patternPath.replace("absolute:", "");
                }
                yield patternPath;
            }
            case instance_resource -> patternPath.replace("instance_resource:", botResourcePath + File.separator);
            case app_resource -> patternPath.replace("app_resource:", RESOURCE_ROOT_DIR + File.separator);
            case app_resource_config ->
                    patternPath.replace("app_resource_config:", getAppResourceAppConfigDir() + File.separator);
            case app_resource_data ->
                    patternPath.replace("app_resource_data:", getAppResourceDataDir() + File.separator);
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


    /**
     * 解压 JAR 文件到指定目录
     *
     * @param jarFilePath jarFilePath
     * @param outputDir   outputDir
     * @throws IOException IOException
     */
    public static void extractJar(String jarFilePath, String outputDir) throws IOException {
        // 打开 JAR 文件
        try (JarFile jarFile = new JarFile(jarFilePath);) {
            // 创建目标目录，如果不存在则创建
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }
            // 获取 JAR 文件中的所有条目
            Enumeration<JarEntry> entries = jarFile.entries();

            // 遍历所有条目
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // 获取条目的名称（路径）
                String entryName = entry.getName();
                File entryFile = new File(outputDirFile, entryName);

                // 如果条目是目录，则创建该目录
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else if (!entryName.endsWith(".class")) {
                    // 如果条目是文件，则解压文件内容
                    try (InputStream inputStream = jarFile.getInputStream(entry);
                         OutputStream outputStream = new FileOutputStream(entryFile)) {

                        // 缓冲区
                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        // 读取 JAR 文件内容并写入目标文件
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }
}
