package cn.com.vortexa.nameserver.processor;

import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.nameserver.constant.*;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.server.NameserverService;
import cn.com.vortexa.nameserver.service.IRegistryService;
import cn.com.vortexa.websocket.netty.handler.BaseWebSocketInboundHandler;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        this.pingCommandProcessor = new PingCommandProcessor();
        this.pongCommandProcessor = new PongCommandProcessor();
        this.serviceRegistryProcessor = new ServiceRegistryProcessor(registryService);
        this.serviceDiscoverProcessor = new ServiceDiscoverProcessor(registryService);

        init(Executors.newThreadPerTaskExecutor(new NamedThreadFactory("nameserver-processor")));
    }


    @Override
    protected void handlerMessage(ChannelHandlerContext ctx, RemotingCommand remotingCommand) {
        Integer opt = remotingCommand.getFlag();
        String txId = remotingCommand.getTransactionId();

        Channel channel = ctx.channel();
        String clientName = channel.attr(NettyConstants.CLIENT_NAME).get();
        log.debug("receive client[{}] command [{}]", clientName, remotingCommand);

        CompletableFuture.supplyAsync(() -> switch (opt) {
                    case RemotingCommandFlagConstants.PING ->
                            pingCommandProcessor.handlerPing(clientName, channel, remotingCommand);
                    case RemotingCommandFlagConstants.PONG ->
                            pongCommandProcessor.handlerPong(clientName, channel, remotingCommand);
                    case RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE ->
                            serviceRegistryProcessor.handlerClientServiceRegistry(channel, remotingCommand);
                    case RemotingCommandFlagConstants.CLIENT_DISCOVER_SERVICE ->
                            serviceDiscoverProcessor.handlerDiscoverService(channel, remotingCommand);
                    default -> throw new IllegalStateException("Unexpected value: " + opt);
                }, getCallbackInvoker())
                .whenCompleteAsync((response, ex) -> {
                    if (ex != null) {
                        log.error("client[{}] command process failed", clientName, ex);
                    }
                    if (response != null) {
                        response.setTransactionId(txId);
                        ctx.channel().writeAndFlush(response);
                    }
                });
    }

    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        nameserverService.closeChannel(ctx.channel());
    }

    @Override
    protected Object getMessageId(RemotingCommand message) {
        return message.getTransactionId();
    }
}
