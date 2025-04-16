package cn.com.vortexa.common.util.classloader;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author helei
 * @since 2025-04-17
 */
public class DynamicClassLoader extends ClassLoader{
    public Class<?> loadClassFromFile(String className, String classFilePath) throws IOException {
        // 读取 .class 文件的内容
        File file = new File(classFilePath);
        byte[] classData = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(classData);
        }

        // 调用 defineClass 将字节数组转换为 Class 对象
        return defineClass(className, classData, 0, classData.length);
    }
}
