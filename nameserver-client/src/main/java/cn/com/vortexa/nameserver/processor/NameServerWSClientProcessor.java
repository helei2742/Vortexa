package cn.com.vortexa.nameserver.processor;


import cn.com.vortexa.nameserver.config.NameserverClientConfig;
import cn.com.vortexa.nameserver.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.util.DistributeIdMaker;
import cn.com.vortexa.websocket.netty.base.AbstractWebSocketClientHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

/**
 * @author helei
 * @since 2025-03-13
 */
@Slf4j
public class NameServerWSClientProcessor extends AbstractWebSocketClientHandler<RemotingCommand, RemotingCommand> {

    private final NameserverClientConfig clientConfig;

    public NameServerWSClientProcessor(NameserverClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        super.channelActive(ctx);

        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setFlag(RemotingCommandFlagConstants.CLIENT_INIT);
        remotingCommand.setTransactionId(
                DistributeIdMaker.DEFAULT.nextId(clientConfig.getServiceInstance().getServiceId())
        );

        try {
            RemotingCommand response = websocketClient.sendRequest(remotingCommand).get();

            if (response.getFlag() == RemotingCommandFlagConstants.CLIENT_INIT_RESPONSE) {
                log.info("Client init success");
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    protected void handleOtherMessage(RemotingCommand message) {

    }

    @Override
    public RemotingCommand convertMessageToRespType(String message) {

        return null;
    }

    @Override
    public Object getRequestId(RemotingCommand request) {
        return request.getTransactionId();
    }

    @Override
    public Object getResponseId(RemotingCommand response) {
        return response.getTransactionId();
    }
}
