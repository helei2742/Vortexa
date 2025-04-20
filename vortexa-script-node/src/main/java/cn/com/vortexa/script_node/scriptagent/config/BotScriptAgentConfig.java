package cn.com.vortexa.script_node.scriptagent.config;

import cn.com.vortexa.script_agent.ScriptAgent;
import cn.com.vortexa.script_agent.config.ScriptAgentConfig;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.com.vortexa.script_node.scriptagent.BotScriptAgent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
@Data
@Configuration
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "vortexa.script-agent")
@ConditionalOnClass(ScriptAgent.class)
public class BotScriptAgentConfig extends ScriptAgentConfig {
    @Autowired
    private ScriptNodeConfiguration scriptNodeConfiguration;

    @Autowired(required = false)
    private List<RPCServiceInfo<?>> rpcServiceInfos;

    @Bean
    public ScriptAgentConfig scriptAgentClientConfig() {
        return this;
    }

    @Bean
    public BotScriptAgent scriptAgent() throws ExecutionException, InterruptedException {
        ScriptAgentConfig scriptAgentConfig = scriptAgentClientConfig();
        BotScriptAgent botScriptAgent = new BotScriptAgent(scriptAgentConfig, scriptNodeConfiguration, rpcServiceInfos);
        botScriptAgent.connect().get();
        return botScriptAgent;
    }
}
