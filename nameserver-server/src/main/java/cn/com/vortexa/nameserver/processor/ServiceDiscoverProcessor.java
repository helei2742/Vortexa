package cn.com.vortexa.nameserver.processor;

import cn.com.vortexa.nameserver.constant.ExtFieldsConstants;
import cn.com.vortexa.nameserver.constant.LoadBalancePolicy;
import cn.com.vortexa.nameserver.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.nameserver.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.nameserver.dto.RegisteredService;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.ServiceInstanceVO;
import cn.com.vortexa.nameserver.service.IRegistryService;
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
        String clientId = remotingCommand.getClientId();
        LoadBalancePolicy policy = LoadBalancePolicy.valueOf(remotingCommand.getExtFieldsValue(
                ExtFieldsConstants.NAMESERVER_DISCOVER_LOAD_BALANCE_POLICY)
        );

        List<RegisteredService> services = discoverServiceList(group, clientId, serviceId, policy);

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
     * @param policy    policy
     * @return List<RegisteredService>
     */
    public List<RegisteredService> discoverServiceList(
            String group,
            String serviceId,
            String clientId,
            LoadBalancePolicy policy
    ) {
        List<RegisteredService> serviceInstances = registryService.queryServiceInstance(group, serviceId, clientId);
        log.debug("[{}]-[{}]-[{}] discover service total [{}]", group, serviceId, clientId, serviceInstances.size());
        // TODO 根据策略宣泄

        return serviceInstances;
    }
}
