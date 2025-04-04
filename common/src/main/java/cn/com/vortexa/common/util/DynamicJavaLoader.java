package cn.com.vortexa.common.util;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class DynamicJavaLoader {

    public static boolean compileJavaFile(String javaFilePath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int compilationResult = compiler.run(null, null, null, javaFilePath);

        return compilationResult == 0;
    }

    public static Class<?> loadClassFromFile(
            String classPath,
            String className
    ) throws Exception {
        // 1. 获取 class 文件路径
        File classFile = new File(classPath); // 设置为存放 class 文件的路径
        URL classUrl = classFile.toURI().toURL();
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{classUrl})) {
            // 2. 加载 class 文件
            return urlClassLoader.loadClass(className);
        }
    }
}
