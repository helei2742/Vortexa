package cn.com.vortexa.bot_father.service.impl;


import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.rpc.api.bot.IScriptAgentRPC;
import org.springframework.stereotype.Service;

/**
 * @author helei
 * @since 2025-03-23
 */
@Service
public class ScriptAgentRPCImpl implements IScriptAgentRPC {
    @Override
    public String testRPC(ServiceInstance targetServiceInstance, String testParam1) {
        return "";
    }
}
