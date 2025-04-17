package cn.com.vortexa.script_node;

import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.com.vortexa.script_node.util.ScriptBotLauncher;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class ScriptNodeStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ScriptNodeConfiguration scriptNodeConfiguration;

    @Autowired
    private ScriptBotLauncher scriptBotLauncher;

    @Override
    @SuppressWarnings("unchecked")
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("start launch script bot...");

        Set<String> autoLaunchBotKeys = scriptNodeConfiguration.getAutoLaunchBotKeys();

        // 启动配置的bot
        for (Map.Entry<String, AutoBotConfig> entry : scriptNodeConfiguration.getBotKeyConfigMap().entrySet()) {
            String botKey = entry.getKey();
            scriptBotLauncher.addBotInMenu(botKey);

            if (autoLaunchBotKeys.contains(botKey)) {
                scriptBotLauncher.loadAndLaunchBot(entry.getValue());
            }
        }

        // 启动CMD
        ScriptBotLauncher.startCommandLineMenu();
    }
}
