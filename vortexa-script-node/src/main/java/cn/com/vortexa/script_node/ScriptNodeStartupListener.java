package cn.com.vortexa.script_node;

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

import java.util.Map;

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

        // 启动配置的bot
        for (Map.Entry<String, AutoBotConfig> entry : scriptNodeConfiguration.getBotKeyConfigMap().entrySet()) {
            String botKey = entry.getKey();
            AutoBotConfig botConfig = entry.getValue();

            log.info("[{}] start launch...", botKey);

            // 1 不是class，编译位class
            String classFilePath = botConfig.getClassFilePath();
            if (classFilePath.endsWith(".java") && !DynamicJavaLoader.compileJavaFile(classFilePath)) {
                throw new RuntimeException(classFilePath + " compile to class error");
            }
            classFilePath = classFilePath.replace(".java", ".class");

            // 2 加载class
            try {
                Class<?> aClass = DynamicJavaLoader.loadClassFromFile(classFilePath, botConfig.getClassName());

                if (AutoLaunchBot.class.equals(aClass.getSuperclass())) {
                    log.info("[{}] class load success ", botKey);

                    Class<AutoLaunchBot<?>> botClass = (Class<AutoLaunchBot<?>>) aClass;

                    AutoLaunchBot<?> launch = ScriptBotLauncher.launch(botClass, botConfig, botApi, bot -> {
                        bot.setBotStatusChangeHandler((oldStatus, newStatus) -> {
                            if (newStatus == BotStatus.RUNNING) {
                                botScriptAgent.addRunningBot(bot.getBotInfo().getName(), botKey, bot);
                            }
                            if (newStatus == BotStatus.SHUTDOWN) {
                                botScriptAgent.removeRunningBot(bot.getBotInfo().getName(), botKey);
                            }
                        });

                        return true;
                    });
                } else {
                    log.info("[{}] class[{}} illegal, must extends AutoLaunchBot.class", botKey, classFilePath);
                }
            } catch (Exception e) {
                throw new RuntimeException("load class error", e);
            }
        }

        // 启动CMD
        ScriptBotLauncher.startCommandLineMenu();
    }

}
