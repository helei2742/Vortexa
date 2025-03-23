package cn.com.vortexa.rpc.api.bot;


import cn.com.vortexa.common.dto.control.ServiceInstance;

public interface IScriptAgentRPC {

    String testRPC(ServiceInstance targetServiceInstance, String testParam1);

}
