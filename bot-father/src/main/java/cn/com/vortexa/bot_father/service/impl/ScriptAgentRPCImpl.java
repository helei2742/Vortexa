package cn.com.vortexa.bot_father.service.impl;


import cn.com.vortexa.bot_father.bot.AutoLaunchBot;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.rpc.api.bot.IScriptAgentRPC;
import lombok.Setter;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author helei
 * @since 2025-03-23
 */
@Setter
@Service
public class ScriptAgentRPCImpl implements IScriptAgentRPC {

    private AutoLaunchBot<?> bot;

    @Override
    public String testRPC(ServiceInstance targetServiceInstance, String testParam1) {
        return "";
    }

    @Override
    public Map<String, AutoBotJobParam> queryScriptAgentJobType() {
        if (bot == null) return new HashMap<>();
        return bot.getBotInstance().getJobParams();
    }

}
