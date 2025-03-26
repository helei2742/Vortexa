package cn.com.vortexa.rpc.api.bot;


import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;

import java.util.Map;

public interface IScriptAgentRPC {

    String testRPC(ServiceInstance targetServiceInstance, String testParam1);

    Map<String, AutoBotJobParam> queryScriptAgentJobType();
}
