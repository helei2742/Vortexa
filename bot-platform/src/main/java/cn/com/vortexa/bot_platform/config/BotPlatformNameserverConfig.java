package cn.com.vortexa.bot_platform.config;


import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.control.config.ControlServerConfig;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.BotControlServer;
import cn.com.vortexa.control.service.IConnectionService;
import cn.com.vortexa.control.service.IRegistryService;
import cn.com.vortexa.control.service.impl.FileRegistryService;
import cn.com.vortexa.control.service.impl.MemoryConnectionService;
import cn.com.vortexa.rpc.dto.RPCMethodInfo;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author helei
 * @since 2025-03-21
 */
@Slf4j
@Configuration
public class BotPlatformNameserverConfig  {

    private static final String APPLICATION_FILE_NAME = "application.yaml";

    private static final String NAMESERVER_CONFIG_PREFIX = "vortexa.controlServer";

    @Autowired
    private RPCProviderScanner rpcProviderScanner;

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
        return new FileRegistryService(controlServerThreadPool());
    }

    @Bean
    public IConnectionService connectionService() {
        return new MemoryConnectionService();
    }


    @Bean
    public BotControlServer botControlServer() throws Exception {
        ControlServerConfig controlServerConfig = controlServerConfig();
        log.info("start launch BotPlatFormNameserver[{}]", controlServerConfig.getServiceInstance());
        BotControlServer botControlServer = new BotControlServer(controlServerConfig);

        addCustomCommandHandler(botControlServer);

        botControlServer.init(registryService(), connectionService());

        botControlServer.start().get();
        log.info("BotPlatFormNameserver[{}] launch finish", controlServerConfig.getServiceInstance());
        return botControlServer;
    }

    private void addCustomCommandHandler(BotControlServer botControlServer) throws CustomCommandException {
        // 添加命令处理器
        for (Map.Entry<String, RPCMethodInfo> entry : rpcProviderScanner.getProviderInfoMap().entrySet()) {
            String key = entry.getKey();
            RPCMethodInfo rpcMethodInfo = entry.getValue();

            botControlServer.addCustomCommandHandler(key, request -> {
                RequestHandleResult result = new RequestHandleResult();
                Method method = rpcMethodInfo.getMethod();
                Object bean = rpcMethodInfo.getBean();
                Parameter[] parameters = method.getParameters();

                Map<?, ?> params = null;

                log.debug("invoke rpc method[{}]", method.getName());
                try {
                    byte[] body = request.getBody();
                    params = Serializer.Algorithm.Protostuff.deserialize(body, Map.class);

                    Object[] args = new Object[parameters.length];
                    for (int i = 0; i < parameters.length; i++) {
                        String parameterName = parameters[i].getName();
                        args[i] = params.get(parameterName);
                    }

                    result.setData(method.invoke(bean, args));
                    result.setSuccess(true);
                } catch (Exception e) {
                    result.setSuccess(false);
                    log.error("invoke rpc method [{}] error, params[{}]", method.getName(), params);
                }
                return result;
            });
        }
    }
}
