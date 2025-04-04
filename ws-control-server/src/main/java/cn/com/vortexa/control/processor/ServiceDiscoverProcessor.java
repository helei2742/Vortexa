package cn.com.vortexa.control.processor;

import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.common.dto.control.RegisteredService;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.ServiceInstanceVO;
import cn.com.vortexa.control.service.IRegistryService;
import cn.com.vortexa.websocket.netty.util.ProtostuffUtils;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author helei
 * @since 2025-03-12
 */
@Slf4j
public class ServiceDiscoverProcessor {

    private final IRegistryService registryService;

    public ServiceDiscoverProcessor(IRegistryService registryService) {
        this.registryService = registryService;
    }

    /**
     * 处理服务发现请求
     *
     * @param channel         channel
     * @param remotingCommand remotingCommand
     * @return RemotingCommand
     */
    public RemotingCommand handlerDiscoverService(Channel channel, RemotingCommand remotingCommand) {
        String group = remotingCommand.getGroup();
        String serviceId = remotingCommand.getServiceId();
        String clientId = remotingCommand.getInstanceId();


        List<RegisteredService> services = discoverServiceList(group, clientId, serviceId);

        RemotingCommand response = new RemotingCommand();
        response.setFlag(RemotingCommandFlagConstants.CLIENT_DISCOVER_SERVICE_RESPONSE);
        response.setCode(RemotingCommandCodeConstants.SUCCESS);
        response.setBody(ProtostuffUtils.serialize(new ServiceInstanceVO(services)));
        return response;
    }

    /**
     * 查询服务列表
     *
     * @param group     group
     * @param serviceId serviceId
     * @param clientId  clientId
     * @return List<RegisteredService>
     */
    public List<RegisteredService> discoverServiceList(
            String group,
            String serviceId,
            String clientId
    ) {
        List<RegisteredService> serviceInstances = registryService.queryServiceInstance(group, serviceId, clientId);
        log.debug("[{}]-[{}]-[{}] discover service total [{}]", group, serviceId, clientId, serviceInstances.size());
        // TODO 根据策略宣泄

        return serviceInstances;
    }
}
