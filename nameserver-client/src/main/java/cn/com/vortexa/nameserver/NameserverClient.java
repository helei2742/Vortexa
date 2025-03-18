package cn.com.vortexa.nameserver;


import cn.com.vortexa.nameserver.config.NameserverClientConfig;
import cn.com.vortexa.nameserver.constant.NameserverSystemConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.ServiceInstance;
import cn.com.vortexa.nameserver.processor.NameClientProcessorAdaptor;
import cn.com.vortexa.nameserver.util.DistributeIdMaker;
import cn.com.vortexa.nameserver.util.RemotingCommandDecoder;
import cn.com.vortexa.nameserver.util.RemotingCommandEncoder;
import cn.com.vortexa.websocket.netty.base.AbstractWebsocketClient;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * @author helei
 * @since 2025-03-13
 */
@Slf4j
public class NameserverClient extends AbstractWebsocketClient<RemotingCommand> {

    private final NameserverClientConfig clientConfig;

    public NameserverClient(NameserverClientConfig clientConfig) {
        this(clientConfig, new NameClientProcessorAdaptor(clientConfig));
    }

    public NameserverClient(NameserverClientConfig clientConfig, NameClientProcessorAdaptor nameClientProcessorAdaptor) {
        super(clientConfig.getRegistryCenterUrl(), clientConfig.getServiceInstance().toString(), nameClientProcessorAdaptor);
        super.setHandshake(false);
        this.clientConfig = clientConfig;
        ((NameClientProcessorAdaptor) getHandler()).setNameserverClient(this);
    }

    @Override
    public void addPipeline(ChannelPipeline p) {
        p.addLast(new IdleStateHandler(0,
                0, clientConfig.getServiceOfflineTtl()));

        p.addLast(new LengthFieldBasedFrameDecoder(NameserverSystemConstants.MAX_FRAME_LENGTH,
                0, 4, 0, 4));
        p.addLast(new LengthFieldPrepender(4));

        p.addLast(new RemotingCommandDecoder());
        p.addLast(new RemotingCommandEncoder());
        p.addLast(getHandler());
    }

    @Override
    public Object getIdFromMessage(RemotingCommand message) {
        return message.getTransactionId();
    }

    @Override
    protected void doSendMessage(RemotingCommand message, boolean b) {
        ServiceInstance serviceInstance = clientConfig.getServiceInstance();

        if (b && StrUtil.isNotBlank(message.getTransactionId())) {
            message.setTransactionId(
                    DistributeIdMaker.DEFAULT.nextId(serviceInstance.getServiceId())
            );
        }

        message.setGroup(serviceInstance.getGroup());
        message.setServiceId(serviceInstance.getServiceId());
        message.setClientId(serviceInstance.getClientId());

        Optional.of(getChannel()).ifPresent(channel -> channel.writeAndFlush(message));
    }

    @Override
    public void sendPing() {
        sendRequest(RemotingCommand.PING_COMMAND);
    }

    @Override
    public void sendPong() {
        sendRequest(RemotingCommand.PONG_COMMAND);
    }
}
