package cn.com.vortexa.bot_father.config;

import cn.com.vortexa.control.NameserverClient;
import cn.com.vortexa.control.config.NameserverClientConfig;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.rpc.dto.RPCMethodInfo;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * @author helei
 * @since 2025/3/21 15:04
 */
@Slf4j
@Configuration
public class BotScriptAgentConfig {

    private static final String APPLICATION_FILE_NAME = "application.yaml";

    private static final String NAMESERVER_CONFIG_PREFIX = "vortexa.nameserver.client";

    @Autowired
    private RPCProviderScanner providerScanner;

    @Bean
    public NameserverClientConfig nameserverClientConfig() throws FileNotFoundException {
        return NameserverClientConfig.loadConfig(APPLICATION_FILE_NAME, NAMESERVER_CONFIG_PREFIX);
    }

    @Bean
    public NameserverClient nameserverClient() throws FileNotFoundException, CustomCommandException {
        NameserverClientConfig nameserverClientConfig = nameserverClientConfig();
        NameserverClient nameserverClient = new NameserverClient(nameserverClientConfig);

        for (Map.Entry<Integer, RPCMethodInfo> entry : providerScanner.getProviderInfoMap().entrySet()) {
            int code = entry.getKey();
            RPCMethodInfo rpcMethodInfo = entry.getValue();

            nameserverClient.addCustomCommandHandler(code, request -> {
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

        return nameserverClient;
    }
}
