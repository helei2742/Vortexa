package cn.com.vortexa.bot_platform.script_control;

import cn.com.vortexa.bot_platform.script_control.service.DBRegistryService;
import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.bot_platform.wsController.FrontWebSocketServer;
import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.control.config.ControlServerConfig;
import cn.com.vortexa.control.service.IConnectionService;
import cn.com.vortexa.control.service.IRegistryService;
import cn.com.vortexa.control.service.impl.MemoryConnectionService;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author helei
 * @since 2025-03-21
 */
@Slf4j
@Configuration
public class BotPlatformControlServerConfig {

    private static final String APPLICATION_FILE_NAME = "application.yaml";

    private static final String NAMESERVER_CONFIG_PREFIX = "vortexa.controlServer";

    @Autowired
    private List<RPCServiceInfo<?>> rpcServiceInfos;

    @Autowired
    private IScriptNodeService scriptNodeService;

    @Autowired
    private FrontWebSocketServer frontWebSocketServer;

    @Bean
    public ControlServerConfig controlServerConfig() throws FileNotFoundException {
        return ControlServerConfig.loadConfig(APPLICATION_FILE_NAME, NAMESERVER_CONFIG_PREFIX);
    }

    @Bean
    public ExecutorService controlServerThreadPool() throws FileNotFoundException {
        return Executors.newThreadPerTaskExecutor(
                new NamedThreadFactory(controlServerConfig().getServiceInstance().toString())
        );
    }

    @Bean
    public IRegistryService registryService() throws FileNotFoundException {
        return new DBRegistryService(scriptNodeService, controlServerThreadPool());
    }

    @Bean
    public IConnectionService connectionService() {
        return new MemoryConnectionService();
    }

    @Bean
    public BotPlatformControlServer botControlServer() throws Exception {
        ControlServerConfig controlServerConfig = controlServerConfig();
        log.info("start launch BotPlatFormNameserver[{}]", controlServerConfig.getServiceInstance());
        BotPlatformControlServer botControlServer = new BotPlatformControlServer(controlServerConfig, frontWebSocketServer, rpcServiceInfos);
        botControlServer.init(registryService(), connectionService());
        botControlServer.start().get();
        log.info("BotPlatFormNameserver[{}] launch finish", controlServerConfig.getServiceInstance());
        return botControlServer;
    }
}
