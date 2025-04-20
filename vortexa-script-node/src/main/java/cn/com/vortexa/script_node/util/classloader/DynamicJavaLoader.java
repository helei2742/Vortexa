package cn.com.vortexa.script_node.util.classloader;

import lombok.extern.slf4j.Slf4j;

import javax.tools.*;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class DynamicJavaLoader {
    /**
     * 编译java文件夹
     *
     * @param sourceDir sourceDir
     * @param outputDir outputDir
     * @return boolean
     * @throws IOException IOException
     */
    public static boolean compileJavaDir(String sourceDir, String outputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("no usable compiler, please use JDK compile");
        }

        File sourceFolder = new File(sourceDir);
        File[] javaFiles = sourceFolder.listFiles(f -> f.getName().endsWith(".java"));
        if (javaFiles == null || javaFiles.length == 0) {
            log.warn("there is no any .java file in {}", sourceDir);
            return false;
        }

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,
                StandardCharsets.UTF_8)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(new File(outputDir)));

            String classpath = System.getProperty("java.class.path");
            List<String> options = List.of(
                    "-Xlint:-options",
                    "-cp", classpath
            );

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(javaFiles);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null,
                    compilationUnits);

            return task.call();
        }
    }

    /**
     * 编译java文件
     *
     * @param javaFilePath   javaFilePath
     * @param classOutputDir classOutputDir
     * @return boolean
     * @throws IOException IOException
     */
    public static boolean compileJavaFile(String javaFilePath, String classOutputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("no usable compiler, please use JDK compile");
        }
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(new File(classOutputDir)));

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(
                    new File(javaFilePath));

            // 编译参数，可以添加 -Xlint:-options 或 -proc:none
            String classpath = System.getProperty("java.class.path");
            List<String> options = List.of(
                    "-Xlint:-options",
                    "-cp", classpath
            );

            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null,
                    compilationUnits);
            return task.call();
        }
    }

    public static Class<?> loadClassFromFile(
            String outputDir,
            String className
    ) throws Exception {
        // 加载 class
        URL[] urls = {new File(outputDir).toURI().toURL()};
        try (URLClassLoader classLoader = new URLClassLoader(urls);) {
            return classLoader.loadClass(className);
        }
    }

    public static Class<?> loadClassFromDir(
            String outputDir, String className, List<String> otherClassName
    ) throws Exception {
        // 加载 class
        URL[] urls = {new File(outputDir).toURI().toURL()};
        try (URLClassLoader classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader())) {
            Thread.currentThread().setContextClassLoader(classLoader);
            if (otherClassName != null && !otherClassName.isEmpty()) {
                for (String aClass : otherClassName) {
                    classLoader.loadClass(aClass);
                }
            }
            return classLoader.loadClass(className);
        }
    }
}
