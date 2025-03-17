package cn.com.vortexa.nameserver.processor;


import cn.com.vortexa.nameserver.config.NameserverClientConfig;
import cn.com.vortexa.nameserver.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.util.DistributeIdMaker;
import cn.com.vortexa.websocket.netty.base.AbstractWebSocketClientHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author helei
 * @since 2025-03-13
 */
@Slf4j
public class NameClientProcessorAdaptor extends AbstractWebSocketClientHandler<RemotingCommand, RemotingCommand, RemotingCommand> {

    private final NameserverClientConfig clientConfig;

    public NameClientProcessorAdaptor(NameserverClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        super.channelActive(ctx);
        sendRegistryCommand();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand remotingCommand) throws Exception {
        Integer opt = remotingCommand.getFlag();
        if (opt.equals(RemotingCommandFlagConstants.PING)) {
            websocketClient.sendRequest(RemotingCommand.PONG_COMMAND);
        } else if (opt.equals(RemotingCommandFlagConstants.PONG)) {
            log.info("{} receive remote[{}] pong", ctx.channel().remoteAddress(), clientConfig.getRegistryCenterUrl());
        }


    }

    @Override
    public Object getRequestId(RemotingCommand request) {
        return request.getTransactionId();
    }

    @Override
    public Object getResponseId(RemotingCommand response) {
        return response.getTransactionId();
    }


    /**
     * 发送服务注册命令
     */
    private void sendRegistryCommand() {
        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setFlag(RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE);
        remotingCommand.setTransactionId(
                DistributeIdMaker.DEFAULT.nextId(clientConfig.getServiceInstance().getServiceId())
        );

        remotingCommand.setBodyFromObject(clientConfig.getServiceInstance());

        websocketClient.sendRequest(remotingCommand).whenComplete((response, throwable) -> {
            if (throwable != null) {
                log.error("{} -> [{}] channel active error",
                        clientConfig.getServiceInstance(), clientConfig.getRegistryCenterUrl(), throwable);
            }

            if (response.getFlag() == RemotingCommandFlagConstants.CLIENT_DISCOVER_SERVICE_RESPONSE) {
                log.info("{} client registry success",  clientConfig.getServiceInstance());
            }
        });
    }
}
