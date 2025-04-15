package cn.com.vortexa.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class DynamicJavaLoader {

    public static boolean compileJavaFile(String javaFilePath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        String classpath = System.getProperty("java.class.path");
        // 编译参数，可以添加 -Xlint:-options 或 -proc:none
        List<String> options = List.of(
                "-Xlint:-options",
                "-classpath", classpath
        );

        // 创建标准文件管理器
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // 获取要编译的文件
        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjects(javaFilePath);

        // 编译任务
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, null, options, null, compilationUnits
        );

        // 执行编译任务
        boolean success = task.call();

        // 关闭文件管理器
        try {
            fileManager.close();
        } catch (IOException e) {
            log.error("close file manager error", e);
        }

        return success;
    }

    public static Class<?> loadClassFromFile(
            String classPath,
            String className
    ) throws Exception {
        // 1. 获取 class 文件路径
        File classFile = new File(classPath); // 设置为存放 class 文件的路径
        URL classUrl = classFile.toURI().toURL();
        try (URLClassLoader urlClassLoader = new URLClassLoader(
                new URL[]{classUrl},
                Thread.currentThread().getContextClassLoader()
        )) {
            // 2. 加载 class 文件
            return urlClassLoader.loadClass(className);
        }
    }
}
