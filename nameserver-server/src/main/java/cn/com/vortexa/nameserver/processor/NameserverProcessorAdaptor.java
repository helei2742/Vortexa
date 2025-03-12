package cn.com.vortexa.nameserver.processor;

import cn.com.vortexa.nameserver.constant.*;
import cn.com.vortexa.nameserver.dto.RegisteredService;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.ServiceInstanceVO;
import cn.com.vortexa.nameserver.handler.NameserverInitHandler;
import cn.com.vortexa.nameserver.handler.ReceiveMessageHandler;
import cn.com.vortexa.nameserver.server.NameserverService;
import cn.com.vortexa.websocket.netty.base.NettyClientEventHandler;
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
public class NameserverProcessorAdaptor extends AbstractNettyProcessorAdaptor {

    private final NameserverService nameServerService;

    private final ServiceDiscoverProcessor serviceDiscoverProcessor;

    private final ServiceRegistryProcessor serviceRegistryProcessor;

    public NameserverProcessorAdaptor(
            NameserverService nameServerService,
            NettyClientEventHandler eventHandler
    ) {
        super();
        super.useRemotingCommandPool = false;
        super.init(
                new NameserverInitHandler(),
                new ReceiveMessageHandler(),
                eventHandler
        );

        this.nameServerService = nameServerService;
        this.serviceDiscoverProcessor = new ServiceDiscoverProcessor();
        this.serviceRegistryProcessor = new ServiceRegistryProcessor();
    }


    @Override
    protected void handlerMessage(ChannelHandlerContext context, RemotingCommand remotingCommand) {
        log.info("[{}] get message [{}] from [{}]",
                nameServerService.getName(), remotingCommand, context.channel().remoteAddress());

        String tsId = remotingCommand.getTransactionId();

        String group = remotingCommand.getGroup();
        String serviceId = remotingCommand.getServiceId();
        String clientId = remotingCommand.getServiceId();

        RemotingCommand response = null;
        CompletableFuture<RegistryState> result;
        switch (remotingCommand.getFlag()) {
            case RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE:  //收到客户端服务注册的请求
                result = serviceRegistryProcessor.clientServiceRegistry(remotingCommand);

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
            context.channel().writeAndFlush(response);
        }
    }


    @Override
    protected void handlePing(ChannelHandlerContext context, RemotingCommand remotingCommand) {
        log.debug("get ping message [{}] from [{}]", remotingCommand, context.channel().remoteAddress());



        super.handlePing(context, remotingCommand);
    }


    @Override
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        super.handleReaderIdle(ctx);
        sendPingMsg(ctx);
    }


    @Override
    public void printLog(String logStr) {
        log.info(logStr);
    }
}
