import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.classloader.DynamicJavaLoader;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author h30069248
 * @since 2025/4/17 11:52
 */
public class TestLoadJavaFile {
    public static void main(String[] args) throws Exception {
        String javaFile = "D:\\workspace\\Vortexa-dev_3.0\\vortexa-script-bot\\src\\main\\java\\cn\\com\\vortexa\\script_bot\\daily\\beamable";
        String outputDir = FileUtil.getCompileClassResource("beamable_test");
        String className = "cn.com.vortexa.script_bot.daily.beamable.BeamableBot";

        // 1. 编译外部 Java 源文件
        if (!DynamicJavaLoader.compileJavaDir(javaFile, outputDir)) {
            System.out.println("编译失败");
            return;
        }

        Class<?> clazz = DynamicJavaLoader.loadClassFromDir(
                outputDir,
                className,
                null
        );
        // 4. 创建实例
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // 5. 调用 run 方法
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            System.out.println(declaredMethod.getName());
        }
    }
}
