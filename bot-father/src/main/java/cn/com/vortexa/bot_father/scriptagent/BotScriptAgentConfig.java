package cn.com.vortexa.bot_father.scriptagent;

import cn.com.vortexa.control.ScriptAgent;
import cn.com.vortexa.control.config.ScriptAgentConfig;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.control.util.RPCMethodUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author helei
 * @since 2025/3/21 15:04
 */
@Slf4j
@Configuration
@ConditionalOnClass(ScriptAgent.class)
public class BotScriptAgentConfig {

    private static final String APPLICATION_FILE_NAME = "application.yaml";

    private static final String NAMESERVER_CONFIG_PREFIX = "vortexa.scriptAgent";

    @Autowired(required = false)
    private List<RPCServiceInfo<?>> rpcServiceInfos;

    @Bean
    public ScriptAgentConfig scriptAgentClientConfig() throws FileNotFoundException {
        return ScriptAgentConfig.loadConfig(APPLICATION_FILE_NAME, NAMESERVER_CONFIG_PREFIX);
    }

    @Bean
    public ScriptAgent scriptAgent() throws FileNotFoundException, CustomCommandException {
        ScriptAgentConfig scriptAgentConfig = scriptAgentClientConfig();
        ScriptAgent scriptAgent = new ScriptAgent(scriptAgentConfig);

        addCustomCommandHandler(scriptAgent);

        return scriptAgent;
    }

    private void addCustomCommandHandler(ScriptAgent scriptAgent) throws CustomCommandException {
        if (rpcServiceInfos == null) return;

        for (RPCServiceInfo<?> rpcServiceInfo : rpcServiceInfos) {
            Class<?> interfaces = rpcServiceInfo.getInterfaces();
            Object ref = rpcServiceInfo.getRef();

            for (Method method : interfaces.getDeclaredMethods()) {
                method.setAccessible(true);
                String key = RPCMethodUtil.buildRpcMethodKey(interfaces.getName(), method);

                scriptAgent.addCustomCommandHandler(key, request -> {
                    RequestHandleResult result = new RequestHandleResult();

                    log.debug("invoke rpc method[{}]", method.getName());
                    try {
                        byte[] body = request.getBody();
                        RPCArgsWrapper params = Serializer.Algorithm.JDK.deserialize(body, RPCArgsWrapper.class);
                        result.setData(method.invoke(ref, params.getArgs()));
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
