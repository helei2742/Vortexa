package cn.com.vortexa.control.processor;

import cn.com.vortexa.control.NameserverClient;
import cn.com.vortexa.control.config.NameserverClientConfig;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.util.DistributeIdMaker;
import cn.com.vortexa.websocket.netty.handler.AbstractWebSocketClientHandler;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * @author helei
 * @since 2025-03-13
 */
@Slf4j
public class NameClientProcessorAdaptor extends AbstractWebSocketClientHandler<RemotingCommand> {

    @Setter
    private NameserverClient nameserverClient;

    @Getter
    private final NameserverClientConfig clientConfig;

    public NameClientProcessorAdaptor(NameserverClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    protected void handlerMessage(ChannelHandlerContext ctx, RemotingCommand remotingCommand) {
        Integer opt = remotingCommand.getFlag();
        String txId = remotingCommand.getTransactionId();

        Channel channel = ctx.channel();
        String key = channel.attr(NettyConstants.CLIENT_NAME).get();
        log.debug("receive client[{}] command [{}]", key, remotingCommand);

        CompletableFuture.supplyAsync(() -> switch (opt) {
                    case RemotingCommandFlagConstants.PING -> this.handlerPing(remotingCommand);
                    case RemotingCommandFlagConstants.PONG -> this.handlerPong(remotingCommand);
                    case RemotingCommandFlagConstants.CUSTOM_COMMAND -> this.handlerCustomCommand(remotingCommand);
                    case RemotingCommandFlagConstants.CUSTOM_COMMAND_RESPONSE -> this.handlerCustomCommandResponse(remotingCommand);
                    default -> throw new RuntimeException("resolve custom request[%s] error ".formatted(
                            remotingCommand
                    ));
                }, nameserverClient.getCallbackInvoker())
                .whenCompleteAsync((response, throwable) -> {
                    if (throwable != null) {
                        log.error("remote command[{}} resolve error", remotingCommand, throwable);
                        return;
                    }
                    if (response != null) {
                        response.setTransactionId(txId);
                        nameserverClient.sendRequest(response);
                    }
                });
    }

    @Override
    protected Object getMessageId(RemotingCommand message) {
        return message.getTransactionId();
    }

    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        super.handleAllIdle(ctx);
        RemotingCommand ping = RemotingCommand.generatePingCommand(nameserverClient.getName());
        ping.setTransactionId(
                DistributeIdMaker.DEFAULT.nextId(nameserverClient.getName())
        );
        log.info("send ping to remote");
        nameserverClient.sendRequest(ping).whenComplete((response, throwable) -> {
            if (throwable != null) {
                log.error("send ping to remote server[{}] error", clientConfig.getRegistryCenterUrl());
                return;
            }
            log.debug("receive remote server pong [{}]", response);
        });
    }

    /**
     * 处理Ping
     *
     * @param ping ping
     * @return RemotingCommand
     */
    private RemotingCommand handlerPing(RemotingCommand ping) {
        log.debug("receive pint[{}] from server", ping);
        RemotingCommand pong = RemotingCommand.generatePongCommand(nameserverClient.getName());
        pong.setTransactionId(ping.getTransactionId());
        return pong;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        log.error("exception ", cause);
    }

    /**
     * 处理Pong
     *
     * @param pong pong
     * @return RemotingCommand
     */
    private RemotingCommand handlerPong(RemotingCommand pong) {
        log.debug("receive pong[{}] from server", pong);
        return null;
    }


    private RemotingCommand handlerCustomCommand(RemotingCommand remotingCommand) {
        return nameserverClient.tryResolveCustomRequest(remotingCommand);
    }

    private RemotingCommand handlerCustomCommandResponse(RemotingCommand remotingCommand) {
        return null;
    }
}
