package cn.com.vortexa.script_node.scriptagent.config;

import cn.com.vortexa.script_agent.ScriptAgent;
import cn.com.vortexa.script_agent.config.ScriptAgentConfig;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.com.vortexa.script_node.scriptagent.BotScriptAgent;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    @Autowired
    private ScriptNodeConfiguration scriptNodeConfiguration;

    @Autowired(required = false)
    private List<RPCServiceInfo<?>> rpcServiceInfos;

    @Bean
    public ScriptAgentConfig scriptAgentClientConfig() throws FileNotFoundException {
        return ScriptAgentConfig.loadConfig(APPLICATION_FILE_NAME, NAMESERVER_CONFIG_PREFIX);
    }

    @Bean
    public BotScriptAgent scriptAgent() throws FileNotFoundException, ExecutionException, InterruptedException {
        ScriptAgentConfig scriptAgentConfig = scriptAgentClientConfig();
        BotScriptAgent botScriptAgent = new BotScriptAgent(scriptAgentConfig, scriptNodeConfiguration, rpcServiceInfos);
        botScriptAgent.connect().get();
        return botScriptAgent;
    }
}
