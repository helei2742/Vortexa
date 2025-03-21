package cn.com.vortexa.control.processor;

import cn.com.vortexa.control.constant.ExtFieldsConstants;
import cn.com.vortexa.control.constant.RegistryState;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.ServiceInstance;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.service.IRegistryService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
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
     * @param channel channel
     * @param remotingCommand remotingCommand
     * @return CompletableFuture<RegistryState>
     */
    public RemotingCommand handlerClientServiceRegistry(
            Channel channel,
            RemotingCommand remotingCommand
    ) {
        // Step 1 解析获取参数
        String group = remotingCommand.getGroup();
        String serviceId = remotingCommand.getServiceId();
        String clientId = remotingCommand.getClientId();

        String[] serviceAddress = channel.remoteAddress().toString().split(":");

        ServiceInstance serviceInstance = ServiceInstance.builder()
                .group(group)
                .serviceId(serviceId)
                .instanceId(clientId)
                .host(serviceAddress[0])
                .port(Integer.parseInt(serviceAddress[1]))
                .build();

        Map<?, ?> serviceProps = null;
        if (remotingCommand.getBody() != null && remotingCommand.getBody().length > 0) {
            try {
                serviceProps = Serializer.Algorithm.Protostuff.deserialize(remotingCommand.getBody(), HashMap.class);
            } catch (Exception e) {
                log.warn("get service props from remoting command error, {}", e.getMessage());
            }
        }

        // Step 2 注册服务实例
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

            log.info("client[{}] registry state [{}]", serviceInstance, registryState);
        } catch (Exception e) {
            log.error("[{}]-[{}] registry error", group, serviceId, e);
            registryState = RegistryState.UNKNOWN_ERROR;
            response.setCode(RemotingCommandCodeConstants.FAIL);
        }

        // Step 3 添加注册状态
        response.addExtField(ExtFieldsConstants.NAMESERVER_REGISTRY_STATUS, registryState.name());
        return response;
    }
}
