package cn.com.vortexa.nameserver.processor;

import cn.com.vortexa.nameserver.constant.*;
import cn.com.vortexa.nameserver.dto.RegisteredService;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.ServiceInstanceVO;
import cn.com.vortexa.nameserver.server.NameserverService;
import cn.com.vortexa.nameserver.service.IRegistryService;
import cn.com.vortexa.websocket.netty.base.AbstractWebSocketClientHandler;
import cn.com.vortexa.websocket.netty.base.NettyClientEventHandler;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.com.vortexa.websocket.netty.util.ProtostuffUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author helei
 * @since 2025/03/11
 */
@Slf4j
@ChannelHandler.Sharable
public class NameserverProcessorAdaptor extends AbstractWebSocketClientHandler<RemotingCommand, RemotingCommand, RemotingCommand> {

    private final NameserverService nameserverService;

    private final ServiceDiscoverProcessor serviceDiscoverProcessor;

    private final ServiceRegistryProcessor serviceRegistryProcessor;

    public NameserverProcessorAdaptor(
            NameserverService nameServerService,
            IRegistryService registryService
    ) {
        super();
        this.nameserverService = nameServerService;
        this.serviceDiscoverProcessor = new ServiceDiscoverProcessor();
        this.serviceRegistryProcessor = new ServiceRegistryProcessor(registryService);
    }

    @Override
    public Object getRequestId(RemotingCommand request) {
        return request.getTransactionId();
    }

    @Override
    public Object getResponseId(RemotingCommand response) {
        return response.getTransactionId();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand remotingCommand) throws Exception {
        Integer opt = remotingCommand.getFlag();

        String clientName = ctx.channel().attr(NettyConstants.CLIENT_NAME).get();
        if (opt.equals(RemotingCommandFlagConstants.PING)) {
            websocketClient.sendRequest(RemotingCommand.PONG_COMMAND);
            log.debug("receive client[{}] ping", clientName);
        } else if (opt.equals(RemotingCommandFlagConstants.PONG)) {
            log.debug("receive client[{}] pong", clientName);
        } else {
            log.info("receive client[{}] command [{}]", clientName, remotingCommand);
            handlerMessage(ctx, remotingCommand);
        }
    }


    protected void handlerMessage(ChannelHandlerContext ctx, RemotingCommand remotingCommand) {
        String tsId = remotingCommand.getTransactionId();

        String group = remotingCommand.getGroup();
        String serviceId = remotingCommand.getServiceId();
        String clientId = remotingCommand.getClientId();

        RemotingCommand response = null;
        CompletableFuture<RegistryState> result;

        switch (remotingCommand.getFlag()) {
            case RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE:  //收到客户端服务注册的请求
                result = serviceRegistryProcessor.clientServiceRegistry(ctx.channel(), remotingCommand);

                response = new RemotingCommand();
                response.setFlag(RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE_RESPONSE);
                response.setTransactionId(tsId);

                try {
                    RegistryState registryState = result.get();
                    if (registryState == RegistryState.OK) {
                        response.setCode(RemotingCommandCodeConstants.SUCCESS);
                    } else {
                        response.setCode(RemotingCommandCodeConstants.FAIL);
                    }

                    log.debug("registry state [{}]", registryState);
                } catch (InterruptedException | ExecutionException e) {
                    log.error("[{}]-[{}] registry error", group, serviceId, e);
                }
                break;

            case RemotingCommandFlagConstants.CLIENT_DISCOVER_SERVICE:
                LoadBalancePolicy policy = LoadBalancePolicy.valueOf(remotingCommand.getExtFieldsValue(
                        ExtFieldsConstants.NAMESERVER_DISCOVER_LOAD_BALANCE_POLICY)
                );

                response = new RemotingCommand();
                response.setFlag(RemotingCommandFlagConstants.CLIENT_DISCOVER_SERVICE_RESPONSE);
                response.setCode(RemotingCommandCodeConstants.SUCCESS);
                response.setTransactionId(tsId);

                List<RegisteredService> services = serviceDiscoverProcessor.discoverService(group, clientId, serviceId, policy);
                response.setBody(ProtostuffUtils.serialize(new ServiceInstanceVO(services)));
        }

        if (response != null) {
            ctx.channel().writeAndFlush(response);
        }
    }
}
