package cn.com.vortexa.nameserver.handler;

import java.util.Map;

public interface ClientInitMsgHandler {

    /**
     * 通过prop的配置，初始化客户端
     *
     * @param prop prop
     */
    boolean initClient(Map<String, String> prop);
}
