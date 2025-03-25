package cn.com.vortexa.bot_father;

import cn.com.vortexa.bot_father.anno.BotApplication;
import cn.com.vortexa.bot_father.bot.AutoLaunchBot;
import cn.com.vortexa.bot_father.config.AutoBotConfig;
import cn.com.vortexa.bot_father.constants.BotStatus;
import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.bot_father.service.impl.ScriptAgentRPCImpl;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.util.BannerUtil;
import cn.com.vortexa.control.ScriptAgent;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.hutool.core.util.BooleanUtil;
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

    private static final String BOT_KEY_PARAM_KEY = "--vortexa.botKey=";

    public static AutoLaunchBot<?> LAUNCHED_BOT = null;

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
        BannerUtil.printBanner("");

        // 命令行参数设置app name
        Optional<String> botKeyOp = Arrays.stream(args).filter(arg -> arg.startsWith(BOT_KEY_PARAM_KEY)).findFirst();
        if (botKeyOp.isEmpty()) {
            throw new BotStartException("bot key is empty");
        }
        String botKey = botKeyOp.get().replace(BOT_KEY_PARAM_KEY, "");
        System.setProperty("spring.application.name", botKey);

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
        ScriptAgent scriptAgent = applicationContext.getBean(ScriptAgent.class);

        // Step 3 创建bot实例
        Constructor<T> constructor = null;
        try {
            constructor = botClass.getConstructor();
            LAUNCHED_BOT = constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new BotInitException(e);
        }

        ScriptAgentRPCImpl scriptAgentRPC = (ScriptAgentRPCImpl) botApi.getScriptAgentRPC();
        scriptAgentRPC.setBot(LAUNCHED_BOT);

        // Step 4 启动script agent
        // Step 4.1 替换ScriptAgent的配置，设置为botName和botKey
        ServiceInstance scriptAgentConfig = scriptAgent.getClientConfig().getServiceInstance();
        scriptAgentConfig.setGroup(scriptAgentConfig.getGroup() == null ? "default" : scriptAgentConfig.getGroup());
        scriptAgentConfig.setServiceId(botName);
        scriptAgentConfig.setInstanceId(botKey);
        scriptAgent.setName(scriptAgentConfig.toString());

        // Step 4.2 设置注册完成后的回调，回调中启动bot
        scriptAgent.setAfterRegistryHandler(response -> {
            try {
                startLaunch(response, botConfig, botApi);
            } catch (BotStartException | BotInitException e) {
                LAUNCHED_BOT.logger.error("launch bot error", e);
                throw new RuntimeException(e);
            }
        });

        try {
            Boolean success = scriptAgent.connect().get();
            if (BooleanUtil.isTrue(success)) {
                LAUNCHED_BOT.logger.info("script agent connect to ControlServer success");
            } else {
                LAUNCHED_BOT.logger.error("script agent connect to ControlServer fail");
            }
        } catch (Exception e) {
            LAUNCHED_BOT.logger.error("script agent connect to ControlServer error", e);
        }

        return applicationContext;
    }

    private static void startLaunch(RemotingCommand response, AutoBotConfig botConfig, BotApi botApi)
            throws BotStartException, BotInitException {
        if (response.getCode() == RemotingCommandCodeConstants.SUCCESS) {
            if (LAUNCHED_BOT.getStatus() == BotStatus.NEW) {
                // Step 4 启动bot
                LAUNCHED_BOT.launch(botConfig, botApi, () -> true);
            }
        } else {
            LAUNCHED_BOT.logger.error("registry fail, response:" + response);
        }
    }
}
