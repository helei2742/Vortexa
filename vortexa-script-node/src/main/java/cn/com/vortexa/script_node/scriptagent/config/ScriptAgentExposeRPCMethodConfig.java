package cn.com.vortexa.script_node.scriptagent.config;


import cn.com.vortexa.script_agent.ScriptAgent;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.rpc.api.bot.IScriptAgentRPC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author helei
 * @since 2025-03-23
 */
@Configuration
@ConditionalOnClass(ScriptAgent.class)
public class ScriptAgentExposeRPCMethodConfig {


    @Bean
    @ConditionalOnBean(IScriptAgentRPC.class)
    public RPCServiceInfo<IScriptAgentRPC> accountBaseInfoRPCServiceInfo(
            IScriptAgentRPC ref
    ) {
        return RPCServiceInfo
                .<IScriptAgentRPC>builder()
                .interfaces(IScriptAgentRPC.class)
                .ref(ref)
                .build();
    }
}
