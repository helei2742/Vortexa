package cn.com.vortexa.nameserver.processor;

import cn.com.vortexa.nameserver.constant.*;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.server.NameserverService;
import cn.com.vortexa.nameserver.service.IRegistryService;
import cn.com.vortexa.nameserver.util.NameserverUtil;
import cn.com.vortexa.websocket.netty.handler.BaseWebSocketInboundHandler;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * @author helei
 * @since 2025/03/11
 */
@Slf4j
@ChannelHandler.Sharable
public class NameserverProcessorAdaptor extends BaseWebSocketInboundHandler<RemotingCommand> {

    @Getter
    private final NameserverService nameserverService;
    private final PingCommandProcessor pingCommandProcessor;
    private final PongCommandProcessor pongCommandProcessor;
    private final ServiceRegistryProcessor serviceRegistryProcessor;
    private final ServiceDiscoverProcessor serviceDiscoverProcessor;

    public NameserverProcessorAdaptor(
            NameserverService nameServerService,
            IRegistryService registryService
    ) {
        super();
        this.nameserverService = nameServerService;
        this.pingCommandProcessor = new PingCommandProcessor(nameserverService);
        this.pongCommandProcessor = new PongCommandProcessor(nameserverService);
        this.serviceRegistryProcessor = new ServiceRegistryProcessor(registryService);
        this.serviceDiscoverProcessor = new ServiceDiscoverProcessor(registryService);

        init(this.nameserverService.getExecutorService());
    }

    @Override
    protected void handlerMessage(ChannelHandlerContext ctx, RemotingCommand remotingCommand) {
        Integer opt = remotingCommand.getFlag();
        String txId = remotingCommand.getTransactionId();

        Channel channel = ctx.channel();
        String key = channel.attr(NettyConstants.CLIENT_NAME).get();
        log.debug("receive client[{}] command [{}]", key, remotingCommand);

        CompletableFuture.supplyAsync(() -> switch (opt) {
                    case RemotingCommandFlagConstants.PING ->
                            pingCommandProcessor.handlerPing(key, channel, remotingCommand);
                    case RemotingCommandFlagConstants.PONG ->
                            pongCommandProcessor.handlerPong(key, channel, remotingCommand);
                    case RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE -> {
                        RemotingCommand registryResult = serviceRegistryProcessor.handlerClientServiceRegistry(
                                channel, remotingCommand);
                        if (registryResult.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                            // 给channel设置名字
                            String group = remotingCommand.getGroup();
                            String serviceId = remotingCommand.getServiceId();
                            String clientId = remotingCommand.getClientId();
                            String newKey = NameserverUtil.generateServiceInstanceKey(group, serviceId, clientId);

                            channel.attr(NettyConstants.CLIENT_NAME).set(newKey);

                            // 注册成功，添加channel连接
                            nameserverService.getConnectionService().addServiceChannel(newKey, channel);
                        }

                        // 返回状态
                        registryResult.addExtField(
                                ExtFieldsConstants.NAMESERVER_STATUS,
                                nameserverService.getState().name()
                        );
                        yield registryResult;
                    }
                    case RemotingCommandFlagConstants.CLIENT_DISCOVER_SERVICE ->
                            serviceDiscoverProcessor.handlerDiscoverService(channel, remotingCommand);
                    default -> {
                        // 判断是不是自定义命令
                        try {
                            yield nameserverService.tryInvokeCustomCommand(channel, remotingCommand);
                        } catch (Exception e) {
                            throw new IllegalStateException("Unexpected value: " + opt, e);
                        }
                    }
                }, getCallbackInvoker())
                .whenCompleteAsync((response, ex) -> {
                    if (ex != null) {
                        log.error("client[{}] command process failed", key, ex);
                    }
                    if (response != null) {
                        response.setTransactionId(txId);

                        log.debug("send response[{}]", response);
                        ctx.channel().writeAndFlush(response);
                        //失败关闭连接
                        if (response.getCode() == RemotingCommandCodeConstants.FAIL) {
                            ctx.close();
                        }
                    }
                });
    }

    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.debug("channel[{}][{}] not active long time, will close it",
                channel.id(), channel.attr(NettyConstants.CLIENT_NAME));

        nameserverService.closeChannel(channel);
    }

    @Override
    protected Object getMessageId(RemotingCommand message) {
        return message.getTransactionId();
    }
}
