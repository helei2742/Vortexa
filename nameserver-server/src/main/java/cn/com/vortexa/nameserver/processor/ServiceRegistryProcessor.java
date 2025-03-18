package cn.com.vortexa.nameserver.processor;


import cn.com.vortexa.nameserver.constant.ExtFieldsConstants;
import cn.com.vortexa.nameserver.constant.RegistryState;
import cn.com.vortexa.nameserver.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.nameserver.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.ServiceInstance;
import cn.com.vortexa.nameserver.protocol.Serializer;
import cn.com.vortexa.nameserver.service.IRegistryService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author helei
 * @since 2025-03-12
 */
@Slf4j
public class ServiceRegistryProcessor {

    private final IRegistryService registryService;

    public ServiceRegistryProcessor(IRegistryService registryService) {
        this.registryService = registryService;
    }

    /**
     * 注册客户端服务
     *
     * @param channel         channel
     * @param remotingCommand remotingCommand
     * @return CompletableFuture<RegistryState>
     */
    public RemotingCommand handlerClientServiceRegistry(
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

        Map<String, Object> serviceProps = null;
        if (remotingCommand.getBody() != null && remotingCommand.getBody().length > 0) {
            try {
                serviceProps = Serializer.Algorithm.Protostuff.deserialize(remotingCommand.getBody(), Map.class);
            } catch (Exception e) {
                log.warn("get service props from remoting command error, {}", e.getMessage());
            }
        }


        RemotingCommand response = new RemotingCommand();
        response.setFlag(RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE_RESPONSE);
        response.setTransactionId(remotingCommand.getTransactionId());
        RegistryState registryState;

        try {
            registryState = registryService.registryService(serviceInstance, serviceProps);

            if (registryState == RegistryState.OK) {
                response.setCode(RemotingCommandCodeConstants.SUCCESS);
            } else {
                response.setCode(RemotingCommandCodeConstants.FAIL);
            }

            log.debug("registry state [{}]", registryState);
        } catch (Exception e) {
            log.error("[{}]-[{}] registry error", group, serviceId, e);
            registryState = RegistryState.UNKNOWN_ERROR;
            response.setCode(RemotingCommandCodeConstants.FAIL);
        }

        response.addExtField(ExtFieldsConstants.NAMESERVER_REGISTRY_STATUS, registryState.name());
        return response;
    }
}
