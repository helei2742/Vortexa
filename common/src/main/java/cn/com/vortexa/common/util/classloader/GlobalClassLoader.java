package cn.com.vortexa.common.util.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class GlobalClassLoader extends URLClassLoader {
    private static volatile GlobalClassLoader instance;

    private GlobalClassLoader(URL[] urls) {
        // 父加载器设 null，不走双亲委托，只走自己 + Bootstrap
        super(urls, null);
    }

    public static GlobalClassLoader getInstance(URL[] urls) {
        if (instance == null) {
            synchronized (GlobalClassLoader.class) {
                if (instance == null) {
                    instance = new GlobalClassLoader(urls);
                }
            }
        }
        return instance;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 先看看自己是否已加载
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                // 尝试自己找
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                // 非自己范围，交给 BootstrapClassLoader（比如 java.lang.String）
                c = getSystemClassLoader().loadClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}
