package cn.com.vortexa.nameserver.processor;


import cn.com.vortexa.nameserver.NameserverClient;
import cn.com.vortexa.nameserver.config.NameserverClientConfig;
import cn.com.vortexa.nameserver.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.util.DistributeIdMaker;
import cn.com.vortexa.websocket.netty.handler.AbstractWebSocketClientHandler;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * @author helei
 * @since 2025-03-13
 */
@Slf4j
public class NameClientProcessorAdaptor extends AbstractWebSocketClientHandler<RemotingCommand> {

    @Getter
    private final NameserverClientConfig clientConfig;

    @Setter
    private NameserverClient nameserverClient;

    public NameClientProcessorAdaptor(NameserverClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().attr(NettyConstants.CLIENT_NAME).set(nameserverClient.getName());
        super.channelActive(ctx);
        sendRegistryCommand();
    }

    @Override
    protected void handlerMessage(ChannelHandlerContext ctx, RemotingCommand message) {
        log.debug("receive [{}] ", message);
    }

    @Override
    protected Object getMessageId(RemotingCommand message) {
        return message.getTransactionId();
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

        remotingCommand.setBodyFromObject(new HashMap<>());

        nameserverClient.sendRequest(remotingCommand).whenComplete((response, throwable) -> {
            if (throwable != null) {
                log.error("{} -> [{}] channel active error",
                        clientConfig.getServiceInstance(), clientConfig.getRegistryCenterUrl(), throwable);
            }

            if (response.getFlag() == RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE_RESPONSE) {
                log.info("{} client registry success", clientConfig.getServiceInstance());
            }
        });
    }
}
