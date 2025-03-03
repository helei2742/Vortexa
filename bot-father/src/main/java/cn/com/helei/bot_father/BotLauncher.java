package cn.com.helei.bot_father;

import cn.com.helei.bot_father.anno.BotApplication;
import cn.com.helei.bot_father.bot.AutoLaunchBot;
import cn.com.helei.bot_father.config.AutoBotConfig;
import cn.com.helei.bot_father.service.BotApi;
import cn.com.helei.common.exception.BotInitException;
import cn.com.helei.common.exception.BotStartException;
import cn.hutool.core.util.StrUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;

@SpringBootApplication(exclude = ServletWebServerFactoryAutoConfiguration.class)
@ImportAutoConfiguration({BotFatherAutoConfig.class})
public class BotLauncher {

    private static final String BOT_KEY_PARAM_KEY = "--bot.botKey=";

    /**
     * 启动bot
     *
     * @param botClass botClass
     * @param args     args
     * @param <T>      T
     * @return ApplicationContext
     * @throws BotStartException BotStartException
     * @throws BotInitException  BotInitException
     */
    public static <T extends AutoLaunchBot<T>> ApplicationContext launch(Class<T> botClass, String[] args)
            throws BotStartException, BotInitException {
        // 命令行参数设置app name
        Optional<String> botKeyOp = Arrays.stream(args).filter(arg -> arg.startsWith("--bot.botKey=")).findFirst();
        if (botKeyOp.isEmpty()) throw new BotStartException("bot key is empty");
        System.setProperty("spring.application.name", botKeyOp.get().replace(BOT_KEY_PARAM_KEY, ""));

        // 解析注解上的bot name
        BotApplication annotation = botClass.getAnnotation(BotApplication.class);
        String botName = null;
        if (annotation == null || StrUtil.isBlank((botName = annotation.name()))) {
            throw new BotStartException("bot must have @BotApplication annotation and must have name");
        }
        AutoBotConfig.BOT_NAME = botName;


        // Step 1 创建容器
        ConfigurableApplicationContext applicationContext = SpringApplication.run(BotLauncher.class, args);

        // Step 2 获取配置Bean
        AutoBotConfig botConfig = applicationContext.getBean(AutoBotConfig.class);
        BotApi botApi = applicationContext.getBean(BotApi.class);

        // Step 3 创建bot实例
        Constructor<T> constructor = null;
        T t = null;
        try {
            constructor = botClass.getConstructor();
            t = constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new BotInitException(e);
        }

        // Step 4 启动bot
        t.launch(botConfig, botApi);

        return applicationContext;
    }
}
