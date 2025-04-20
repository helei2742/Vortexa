package cn.com.vortexa.script_node.util.classloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DynamicClassLoader extends ClassLoader {
    private final Map<String, Class<?>> loadedClass = new HashMap<>();
    private final Map<String, Path> classNameMapBaseDir = new HashMap<>();

    public DynamicClassLoader(ClassLoader classLoader) {
        super(classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (loadedClass.containsKey(name)) {
            return loadedClass.get(name);
        }


        Path baseDir = classNameMapBaseDir.get(name);
        if (baseDir == null) {
            return super.findClass(name);
        }

        // 1. 拼接出 class 文件的路径
        String classFilePath = baseDir + File.separator + name.replace('.', File.separatorChar) + ".class";

        // 2. 尝试从指定目录加载 class 文件
        try {
            byte[] classData = loadClassData(classFilePath);
            if (classData != null) {
                Class<?> aClass = defineClass(name, classData, 0, classData.length);
                loadedClass.put(name, aClass);
                return aClass;
            }
        } catch (IOException e) {
            throw new RuntimeException(name + " load error", e);
        }

        // 3. 如果没有找到，交给父类加载器处理
        return super.findClass(name);
    }

    public void registryClass(String outputDir, String name) {
        classNameMapBaseDir.put(name, Paths.get(outputDir));
    }

    private byte[] loadClassData(String classFilePath) throws IOException {
        Path path = Paths.get(classFilePath);
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
        return null; // 返回 null 表示没有找到 class 文件
    }
}
