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
public class NameserverClient extends AbstractWebsocketClient<RemotingCommand, RemotingCommand, RemotingCommand> {

    private final NameserverClientConfig clientConfig;

    public NameserverClient(NameserverClientConfig clientConfig) {
        super(
                clientConfig.getRegistryCenterUrl(),
                new NameClientProcessorAdaptor(clientConfig)
        );
        this.clientConfig = clientConfig;

        setName(clientConfig.getServiceInstance().toString());
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
        p.addLast(handler);
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
        sendMessage(RemotingCommand.PING_COMMAND);
    }

    @Override
    public void sendPong() {
        sendMessage(RemotingCommand.PONG_COMMAND);
    }

    /**
     * 转换为写入channel的数据
     */
    @Override
    protected RemotingCommand convertToChannelWriteData(RemotingCommand request) {
        return request;
    }
}
