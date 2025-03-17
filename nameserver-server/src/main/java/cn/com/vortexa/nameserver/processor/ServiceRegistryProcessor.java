package cn.com.vortexa.nameserver.processor;


import cn.com.vortexa.nameserver.constant.RegistryState;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.ServiceInstance;
import cn.com.vortexa.nameserver.service.IRegistryService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author helei
 * @since 2025-03-12
 */
public class ServiceRegistryProcessor {

    private final IRegistryService registryService;

    public ServiceRegistryProcessor(IRegistryService registryService) {
        this.registryService = registryService;
    }

    /**
     * 注册客户端服务
     *
     * @param channel             channel
     * @param remotingCommand remotingCommand
     * @return CompletableFuture<RegistryState>
     */
    public CompletableFuture<RegistryState> clientServiceRegistry(
            Channel channel,
            RemotingCommand remotingCommand
    ) {
        String group = remotingCommand.getGroup();
        String serviceId = remotingCommand.getServiceId();
        String clientId = remotingCommand.getClientId();

        String[] serviceAddress = channel.remoteAddress().toString().split(":");

        ServiceInstance serviceInstance = ServiceInstance.builder()
                .group(group)
                .serviceId(serviceId)
                .clientId(clientId)
                .host(serviceAddress[0])
                .port(Integer.parseInt(serviceAddress[1]))
                .build();

        return registryService.registryService(serviceInstance);
    }
}
