package cn.com.vortexa.bot_platform.script_control;


import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.control.config.ControlServerConfig;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.BotControlServer;
import cn.com.vortexa.control.service.IConnectionService;
import cn.com.vortexa.control.service.IRegistryService;
import cn.com.vortexa.control.service.impl.FileRegistryService;
import cn.com.vortexa.control.service.impl.MemoryConnectionService;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.control.util.RPCMethodUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
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
        for (RPCServiceInfo<?> rpcServiceInfo : rpcServiceInfos) {
            Class<?> interfaces = rpcServiceInfo.getInterfaces();

            Object ref = rpcServiceInfo.getRef();

            for (Method method : interfaces.getDeclaredMethods()) {
                method.setAccessible(true);
                String key = RPCMethodUtil.buildRpcMethodKey(interfaces.getName(), method);

                log.info("add custom command handler [{}]", key);

                botControlServer.addCustomCommandHandler(key, request -> {
                    RequestHandleResult result = new RequestHandleResult();

                    log.debug("invoke rpc method[{}]", method.getName());
                    try {
                        byte[] body = request.getBody();
                        RPCArgsWrapper params = Serializer.Algorithm.JDK.deserialize(body, RPCArgsWrapper.class);

                        Object invoke = method.invoke(ref, params.getArgs());
                        log.info("invoke rpc[{}] method[{}] return [{}]", request.getTransactionId(), key, invoke);
                        result.setData(invoke);
                        result.setSuccess(true);
                        return result;
                    } catch (Exception e) {
                        log.error("invoke rpc method [{}] error", method.getName());
                        throw new CustomCommandInvokeException(e);
                    }
                });
            }
        }
    }
}
