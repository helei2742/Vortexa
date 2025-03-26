package cn.com.vortexa.control.processor;

import cn.com.vortexa.control.constant.*;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.BotControlServer;
import cn.com.vortexa.control.service.IRegistryService;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.com.vortexa.websocket.netty.handler.BaseWebSocketInboundHandler;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * @author helei
 * @since 2025/03/11
 */
@Slf4j
@ChannelHandler.Sharable
public class ControlServerProcessorAdaptor extends BaseWebSocketInboundHandler<RemotingCommand> {

    @Getter
    private final BotControlServer botControlServer;
    private final PingCommandProcessor pingCommandProcessor;
    private final PongCommandProcessor pongCommandProcessor;
    private final ServiceRegistryProcessor serviceRegistryProcessor;
    private final ServiceDiscoverProcessor serviceDiscoverProcessor;
    private final ScriptAgentMetricsCommandProcessor scriptAgentMetricsCommandProcessor;

    public ControlServerProcessorAdaptor(
            BotControlServer nameServerService,
            IRegistryService registryService
    ) {
        super();
        this.botControlServer = nameServerService;
        this.pingCommandProcessor = new PingCommandProcessor(botControlServer);
        this.pongCommandProcessor = new PongCommandProcessor(botControlServer);
        this.serviceRegistryProcessor = new ServiceRegistryProcessor(registryService);
        this.serviceDiscoverProcessor = new ServiceDiscoverProcessor(registryService);
        this.scriptAgentMetricsCommandProcessor = new ScriptAgentMetricsCommandProcessor(botControlServer);

        init(this.botControlServer.getExecutorService());
    }

    @Override
    protected void handlerMessage(ChannelHandlerContext ctx, RemotingCommand remotingCommand) {
        Integer opt = remotingCommand.getFlag();
        String txId = remotingCommand.getTransactionId();

        Channel channel = ctx.channel();
        String key = channel.attr(NettyConstants.CLIENT_NAME).get();
        log.debug("receive client[{}] command [{}]", key, remotingCommand);

        CompletableFuture.supplyAsync(() -> switch (opt) {
                    case RemotingCommandFlagConstants.PING -> pingCommandProcessor.handlerPing(key, channel, remotingCommand);
                    case RemotingCommandFlagConstants.PONG -> pongCommandProcessor.handlerPong(key, channel, remotingCommand);
                    case RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE -> {
                        RemotingCommand registryResult = serviceRegistryProcessor.handlerClientServiceRegistry(
                                channel, remotingCommand);
                        if (registryResult.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                            // 给channel设置名字
                            String group = remotingCommand.getGroup();
                            String serviceId = remotingCommand.getServiceId();
                            String clientId = remotingCommand.getClientId();
                            String newKey = ControlServerUtil.generateServiceInstanceKey(group, serviceId, clientId);

                            channel.attr(NettyConstants.CLIENT_NAME).set(newKey);

                            // 注册成功，添加channel连接
                            botControlServer.getConnectionService().addServiceChannel(newKey, channel);
                        }

                        // 返回状态
                        registryResult.addExtField(
                                ExtFieldsConstants.NAMESERVER_STATUS,
                                botControlServer.getState().name()
                        );
                        yield registryResult;
                    }
                    case RemotingCommandFlagConstants.CLIENT_DISCOVER_SERVICE ->
                            serviceDiscoverProcessor.handlerDiscoverService(channel, remotingCommand);
                    case RemotingCommandFlagConstants.CUSTOM_COMMAND ->
                            botControlServer.tryInvokeCustomCommand(channel, remotingCommand);
                    case RemotingCommandFlagConstants.SCRIPT_AGENT_METRICS_UPLOAD ->
                            scriptAgentMetricsCommandProcessor.handlerScriptAgentMetricsUpload(key, remotingCommand);
                    default -> {
                        BiFunction<Channel, RemotingCommand, RemotingCommand> customProcessor = botControlServer.getCustomRemotingCommandHandlerMap().get(opt);
                        if (customProcessor != null) {
                            yield customProcessor.apply(channel, remotingCommand);
                        } else {
                            throw new IllegalStateException("Unexpected value: " + opt);
                        }
                    }
                }, getCallbackInvoker())
                .whenCompleteAsync((response, ex) -> {
                    if (ex != null) {
                        log.error("client[{}] command process failed", key, ex);

                        RemotingCommand errorResponse = new RemotingCommand();
                        errorResponse.setFlag(-1 * opt);
                        errorResponse.setCode(RemotingCommandCodeConstants.FAIL);
                        errorResponse.addExtField(
                                ExtFieldsConstants.REQUEST_ERROR_MSG,
                                ex.getMessage()
                        );

                        ctx.channel().writeAndFlush(errorResponse);
                    } else if (response != null) {
                        response.setTransactionId(txId);

                        log.debug("send response[{}]", response);
                        ctx.channel().writeAndFlush(response);
                    }
                });
    }

    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.debug("channel[{}][{}] not active long time, will close it",
                channel.id(), channel.attr(NettyConstants.CLIENT_NAME));

        botControlServer.closeChannel(channel);
    }

    @Override
    protected Object getMessageId(RemotingCommand message) {
        return message.getTransactionId();
    }
}
