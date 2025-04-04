package cn.com.vortexa.script_node.util;

import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.util.BannerUtil;
import cn.com.vortexa.script_node.view.ScriptNodeCMDLineMenu;
import cn.com.vortexa.script_node.view.commandMenu.DefaultMenuType;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class ScriptBotLauncher {

    private static final ScriptNodeCMDLineMenu scriptNodeCMDLineMenu = new ScriptNodeCMDLineMenu(List.of(
            DefaultMenuType.START_BOT_TASK
    ));

    /**
     * 启动bot
     *
     * @param botClass botClass
     * @return ApplicationContext
     * @throws BotStartException BotStartException
     * @throws BotInitException  BotInitException
     */
    public static AutoLaunchBot<?> launch(
            Class<? extends AutoLaunchBot<?>> botClass,
            AutoBotConfig botConfig,
            BotApi botApi,
            Function<AutoLaunchBot<?>, Boolean> initHandler
    ) throws BotStartException, BotInitException {
        BannerUtil.printBanner("");

        String botKey = botConfig.getBotKey();
        if (StrUtil.isBlank(botKey)) {
            throw new BotStartException("bot key is empty");
        }

        System.setProperty("spring.application.name", botKey);

        // 解析注解上的bot name
        BotApplication annotation = botClass.getAnnotation(BotApplication.class);
        String botName = null;
        if (annotation == null || StrUtil.isBlank((botName = annotation.name()))) {
            throw new BotStartException("bot must have @BotApplication annotation and must have name");
        }

        // Step 1 创建bot实例
        Constructor<? extends AutoLaunchBot<?>> constructor = null;
        AutoLaunchBot<?> bot = null;
        try {
            constructor = botClass.getConstructor();
            bot = constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new BotInitException(e);
        }

        if (botConfig.isCommandMenu()) {
            scriptNodeCMDLineMenu.getBotKeyMap().put(botKey, bot);
        }

        log.info("bot[{}][{}] start launch", botName, botKey);
        // Step 3 启动bot
        bot.launch(botConfig, botApi, initHandler);

        return bot;
    }

    public static void startCommandLineMenu() {
        scriptNodeCMDLineMenu.start();
    }
}
