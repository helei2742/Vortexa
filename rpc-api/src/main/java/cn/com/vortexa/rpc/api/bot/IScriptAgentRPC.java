package cn.com.vortexa.rpc.api.bot;

import cn.com.vortexa.control.dto.ServiceInstance;

public interface IScriptAgentRPC {

    String testRPC(ServiceInstance targetServiceInstance, String testParam1);

}
