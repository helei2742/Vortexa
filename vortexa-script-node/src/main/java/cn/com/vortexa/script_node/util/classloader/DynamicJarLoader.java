package cn.com.vortexa.script_node.util.classloader;

import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.List;

@Slf4j
public class DynamicJarLoader {


    // 动态加载 JAR 包中的类
    public static Class<?> loadClassFromJar(String jarPath, String className, List<String> extraClassNameList) throws Exception {
        URL url = new URL("jar:file:" + jarPath + "!/");

        try (CustomClassLoader customClassLoader = new CustomClassLoader(new URL[]{url},
                Thread.currentThread().getContextClassLoader()
        )) {
            if (extraClassNameList != null && !extraClassNameList.isEmpty()) {
                for (String extraClassName : extraClassNameList) {
                    customClassLoader.loadClass(extraClassName);
                }
            }
            return customClassLoader.loadClass(className);
        }
    }
}
