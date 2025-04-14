package cn.com.vortexa.script_node.util;

import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.util.BannerUtil;
import cn.com.vortexa.script_node.view.ScriptNodeCMDLineMenu;
import cn.com.vortexa.script_node.view.commandMenu.DefaultMenuType;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
public class ScriptBotLauncher {

    private static final ScriptNodeCMDLineMenu scriptNodeCMDLineMenu = new ScriptNodeCMDLineMenu(List.of(
            DefaultMenuType.START_BOT_TASK, DefaultMenuType.LAUNCH_SCRIPT
    ));

    private static final ConcurrentHashMap<String, ScriptBotMetaInfo> botMetaInfoMap = new ConcurrentHashMap<>();

    /**
     * 启动bot
     *
     * @param botClass botClass
     * @return ApplicationContext
     * @throws BotStartException BotStartException
     * @throws BotInitException  BotInitException
     */
    public static AutoLaunchBot<?> launch(
            ScriptNodeConfiguration scriptNodeConfiguration,
            Class<? extends AutoLaunchBot<?>> botClass,
            AutoBotConfig botConfig,
            BotApi botApi,
            Function<AutoLaunchBot<?>, Boolean> initHandler,
            boolean launch
    ) throws BotStartException, BotInitException {
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

        bot.setBotName(botName);
        bot.setBotKey(botKey);
        botMetaInfoMap.put(botKey, new ScriptBotMetaInfo(scriptNodeConfiguration, bot, botConfig, botApi, initHandler));

        if (launch) {
            // Step 3 启动bot
            launchResolvedScriptBot(botKey);
        }

        return bot;
    }

    /**
     * 启动命令行菜单
     */
    public static void startCommandLineMenu() {
        scriptNodeCMDLineMenu.start();
    }

    /**
     * 添加Bot到菜单
     *
     * @param botKey botKey
     * @param bot    bot
     */
    public static void addBotInMenu(String botKey, AutoLaunchBot<?> bot) {
        scriptNodeCMDLineMenu.getBotKeyMap().put(botKey, bot);
    }


    public static void launchResolvedScriptBot(String botKey) throws BotStartException, BotInitException {
        ScriptBotMetaInfo scriptBotMetaInfo = botMetaInfoMap.get(botKey);
        if (scriptBotMetaInfo == null) {
            throw new BotStartException(botKey + " didn't resolved by ScriptBotLauncher, place invoke ScriptBotLauncher.launch(...) first");
        }

        log.info("bot[{}] start launch", botKey);
        scriptBotMetaInfo.getBot().launch(
                scriptBotMetaInfo.scriptNodeConfiguration,
                scriptBotMetaInfo.botConfig,
                scriptBotMetaInfo.botApi,
                scriptBotMetaInfo.initHandler
        );
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScriptBotMetaInfo {
        private ScriptNodeConfiguration scriptNodeConfiguration;
        private AutoLaunchBot<?> bot;
        private AutoBotConfig botConfig;
        private BotApi botApi;
        Function<AutoLaunchBot<?>, Boolean> initHandler;
    }
}
