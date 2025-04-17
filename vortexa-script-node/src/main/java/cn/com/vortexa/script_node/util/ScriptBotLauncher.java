package cn.com.vortexa.script_node.util;

import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.classloader.DynamicJavaLoader;
import cn.com.vortexa.script_node.anno.BotApplication;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.com.vortexa.script_node.constants.BotStatus;
import cn.com.vortexa.script_node.scriptagent.BotScriptAgent;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
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
    public volatile static ScriptBotLauncher INSTANCE;

    private final ScriptNodeConfiguration scriptNodeConfiguration;
    private final BotApi botApi;
    private final BotScriptAgent botScriptAgent;

    public static ScriptBotLauncher buildScriptBotLauncher(ScriptNodeConfiguration scriptNodeConfiguration, BotApi botApi, BotScriptAgent botScriptAgent) {
        if (INSTANCE == null) {
            synchronized (ScriptBotLauncher.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScriptBotLauncher(scriptNodeConfiguration, botApi, botScriptAgent);
                }
            }
        }
        return INSTANCE;
    }

    protected ScriptBotLauncher(ScriptNodeConfiguration scriptNodeConfiguration, BotApi botApi, BotScriptAgent botScriptAgent) {
        this.scriptNodeConfiguration = scriptNodeConfiguration;
        this.botApi = botApi;
        this.botScriptAgent = botScriptAgent;
    }

    /**
     * 加载并启动bot
     *
     * @param botKey botKey
     */
    public void loadAndLaunchBot(String botKey) {
        AutoBotConfig botConfig = scriptNodeConfiguration.getBotKeyConfigMap().get(botKey);
        if (botConfig == null) {
            throw new IllegalArgumentException("no bot in script node" + botKey);
        }
        loadAndLaunchBot(botConfig);
    }

    /**
     * 加载并启动bot
     *
     * @param botConfig botConfig
     */
    public void loadAndLaunchBot(AutoBotConfig botConfig) {
        String botKey = botConfig.getBotKey();

        log.info("[{}] start launch...", botKey);
        String className = botConfig.getClassName();
        if (StrUtil.isBlank(className)) {
            throw new IllegalArgumentException(botKey + " config class name is null");
        }
        try {
            // 加载 class
            // 1 编译为class
            // 2 加载class
            Class<?> aClass = loadScriptNodeResourceClass(
                    botConfig.getResourceDir(),
                    className,
                    botConfig.getExtraClassNameList(),
                    botKey
            );

            log.info("[{}] class load success ", botKey);
            if (isClassInInheritanceChain(aClass, AutoLaunchBot.class)) {
                Class<AutoLaunchBot<?>> botClass = (Class<AutoLaunchBot<?>>) aClass;
                // Step 3 启动bot
                AutoLaunchBot<?> autoLaunchBot = launch(botClass, botConfig, bot -> {
                    bot.setBotStatusChangeHandler((oldStatus, newStatus) -> {
                        // 3.1 添加监听， bot状态改变时上报
                        if (newStatus == BotStatus.RUNNING) {
                            botScriptAgent.addRunningBot(bot.getBotInfo().getName(), botKey, bot);
                        }

                        if (newStatus == BotStatus.STOPPED || newStatus == BotStatus.SHUTDOWN) {
                            botScriptAgent.removeRunningBot(bot.getBotInfo().getName(), botKey);
                        }
                    });
                    return true;
                });

                // Step 4 添加进菜单
                setLoadedBotInMenu(botKey, autoLaunchBot);
            } else {
                log.info("[{}] class[{}} illegal, must extends AutoLaunchBot.class", botKey, className);
            }
        } catch (BotStartException | BotInitException e) {
            log.error("script botKey[{}] auto launch error", botKey, e);
        } catch (Exception e) {
            log.error("script botKey[{}] auto launch error", botKey, e);
            throw new RuntimeException("load class error", e);
        }
    }


    /**
     * 启动bot
     *
     * @param botClass botClass
     * @return ApplicationContext
     * @throws BotStartException BotStartException
     * @throws BotInitException  BotInitException
     */
    public AutoLaunchBot<?> launch(
            Class<? extends AutoLaunchBot<?>> botClass,
            AutoBotConfig botConfig,
            Function<AutoLaunchBot<?>, Boolean> initHandler
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

        botConfig.setBotName(botName);
        bot.setBotName(botName);
        bot.setBotKey(botKey);
        botMetaInfoMap.put(botKey, new ScriptBotMetaInfo(
                bot,
                botConfig,
                initHandler
        ));

        // Step 3 启动bot
        launchResolvedScriptBot(botKey);

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
     */
    public void addBotInMenu(String botKey) {
        scriptNodeCMDLineMenu.getUsableBotKeySet().add(botKey);
    }

    /**
     * 添加加载完成的bot到菜单
     *
     * @param botKey botKey
     * @param bot    bot
     */
    public void setLoadedBotInMenu(String botKey, AutoLaunchBot<?> bot) {
        scriptNodeCMDLineMenu.getLoadedBotMap().put(botKey, bot);
    }

    private void launchResolvedScriptBot(String botKey) throws BotStartException, BotInitException {
        ScriptBotMetaInfo scriptBotMetaInfo = botMetaInfoMap.get(botKey);
        if (scriptBotMetaInfo == null) {
            throw new BotStartException(botKey + " didn't resolved by ScriptBotLauncher, place invoke ScriptBotLauncher.launch(...) first");
        }

        log.info("bot[{}] start launch", botKey);
        scriptBotMetaInfo.getBot().launch(
                scriptNodeConfiguration,
                scriptBotMetaInfo.botConfig,
                botApi,
                scriptBotMetaInfo.initHandler
        );
    }

    /**
     * 加载script bot的class文件
     *
     * @param sourceDir sourceDir
     * @param className className
     * @return 已加载的BotClass文件
     * @throws Exception Exception
     */
    private static Class<?> loadScriptNodeResourceClass(
            String sourceDir, String className, List<String> extraClassName, String botKey
    ) throws Exception {
        String classOutputDir = FileUtil.getCompileClassResource(botKey);
        if (!DynamicJavaLoader.compileJavaDir(sourceDir, classOutputDir)) {
            throw new RuntimeException(sourceDir + " compile to class error");
        }
        log.info("{} compile finish, start output dir {}", sourceDir, classOutputDir);
        return DynamicJavaLoader.loadClassFromDir(classOutputDir, className, extraClassName);
    }

    public static boolean isClassInInheritanceChain(Class<?> subclass, Class<?> superclass) {
        Class<?> currentClass = subclass;
        while (currentClass != null) {
            if (currentClass.equals(superclass)) {
                return true;
            }
            currentClass = currentClass.getSuperclass();
        }
        return false;
    }

    /**
     * 获取bot状态
     *
     * @param botKey botKey
     * @return BotStatus
     */
    public BotStatus getBotStatus(String botKey) {
        ScriptBotMetaInfo botMetaInfo = botMetaInfoMap.get(botKey);
        if (botMetaInfo == null) {
            return BotStatus.NOT_LOADED;
        }
        return botMetaInfo.getBot().getStatus();
    }

    /**
     * 根据botKey 获取bot
     *
     * @param botKey botKey
     * @return AutoLaunchBot
     */
    public AutoLaunchBot<?> getBotByBotKey(String botKey) {
        ScriptBotMetaInfo botMetaInfo = botMetaInfoMap.get(botKey);
        return botMetaInfo == null ? null : botMetaInfo.getBot();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScriptBotMetaInfo {
        private AutoLaunchBot<?> bot;
        private AutoBotConfig botConfig;
        Function<AutoLaunchBot<?>, Boolean> initHandler;
    }
}
