package cn.com.vortexa.script_node;

import cn.com.vortexa.common.dto.config.ClassInfo;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.util.DynamicJavaLoader;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.com.vortexa.script_node.constants.BotStatus;
import cn.com.vortexa.script_node.scriptagent.BotScriptAgent;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.script_node.util.ScriptBotLauncher;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class ScriptNodeStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ScriptNodeConfiguration scriptNodeConfiguration;

    @Autowired
    private BotApi botApi;

    @Autowired
    private BotScriptAgent botScriptAgent;

    @Override
    @SuppressWarnings("unchecked")
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("start launch script bot...");

        Set<String> autoLaunchBotKeys = scriptNodeConfiguration.getAutoLaunchBotKeys();

        // 启动配置的bot
        for (Map.Entry<String, AutoBotConfig> entry : scriptNodeConfiguration.getBotKeyConfigMap().entrySet()) {
            String botKey = entry.getKey();
            AutoBotConfig botConfig = entry.getValue();

            log.info("[{}] start launch...", botKey);
            try {
                // 加载extra class
                List<ClassInfo> extraClass = botConfig.getExtraClass();
                if (extraClass != null && !extraClass.isEmpty()) {
                    for (ClassInfo classInfo : extraClass) {
                        loadScriptNodeResourceClass(classInfo.getClassFilePath(), classInfo.getClassName());
                    }
                }

                // 1 不是class，编译位class
                String classFilePath = botConfig.getClassFilePath();

                // 2 加载class
                Class<?> aClass = loadScriptNodeResourceClass(classFilePath, botConfig.getClassName());

                log.info("[{}] class load success ", botKey);

                if (AutoLaunchBot.class.equals(aClass.getSuperclass())) {
                    Class<AutoLaunchBot<?>> botClass = (Class<AutoLaunchBot<?>>) aClass;

                    boolean launch = autoLaunchBotKeys.contains(botKey);

                    // Step 3 启动bot
                    AutoLaunchBot<?> autoLaunchBot = ScriptBotLauncher.launch(botClass, botConfig, botApi, bot -> {
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
                    }, launch);

                    // Step 4 添加进菜单
                    if (botConfig.isCommandMenu()) {
                        ScriptBotLauncher.addBotInMenu(botKey, autoLaunchBot);
                    }
                } else {
                    log.info("[{}] class[{}} illegal, must extends AutoLaunchBot.class", botKey, classFilePath);
                }

            } catch (BotStartException | BotInitException e) {
                log.error("script botKey[{}] auto launch error", botKey, e);
            } catch (Exception e) {
                throw new RuntimeException("load class error", e);
            }
        }

        // 启动CMD
        ScriptBotLauncher.startCommandLineMenu();
    }

    /**
     * 加载script bot的class文件
     *
     * @param classFilePath classFilePath
     * @param className className
     * @return 已加载的BotClass文件
     * @throws Exception Exception
     */
    private Class<?> loadScriptNodeResourceClass(String classFilePath, String className) throws Exception {
        if (classFilePath.endsWith(".java") && !DynamicJavaLoader.compileJavaFile(classFilePath)) {
            throw new RuntimeException(classFilePath + " compile to class error");
        }
        classFilePath = classFilePath.replace(".java", ".class");

        return DynamicJavaLoader.loadClassFromFile(classFilePath, className);
    }
}
