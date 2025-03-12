package cn.com.vortexa.nameserver.processor;


import cn.com.vortexa.nameserver.constant.RegistryState;
import cn.com.vortexa.nameserver.dto.RemotingCommand;

import java.util.concurrent.CompletableFuture;

/**
 * @author helei
 * @since 2025-03-12
 */
public class ServiceRegistryProcessor {

    /**
     * 注册客户端服务
     *
     * @param remotingCommand remotingCommand
     * @return CompletableFuture<RegistryState>
     */
    public CompletableFuture<RegistryState> clientServiceRegistry(RemotingCommand remotingCommand) {
        return null;
    }
}
