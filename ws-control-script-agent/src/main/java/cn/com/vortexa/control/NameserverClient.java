package cn.com.vortexa.control;


import cn.com.vortexa.control.config.NameserverClientConfig;
import cn.com.vortexa.control.constant.*;
import cn.com.vortexa.control.dto.RemoteNameserverStatus;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.ServiceInstance;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.handler.CustomRequestHandler;
import cn.com.vortexa.control.processor.CustomCommandProcessor;
import cn.com.vortexa.control.processor.NameClientProcessorAdaptor;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.util.DistributeIdMaker;
import cn.com.vortexa.control.util.RemotingCommandDecoder;
import cn.com.vortexa.control.util.RemotingCommandEncoder;
import cn.com.vortexa.websocket.netty.base.AbstractWebsocketClient;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author helei
 * @since 2025-03-13
 */
@Slf4j
public class NameserverClient extends AbstractWebsocketClient<RemotingCommand> {

    private final NameserverClientConfig clientConfig;  // 配置
    private final String clientName;    // 客户端name
    private final RemoteNameserverStatus remoteStatus; // 远程服务命名中心状态
    private final CustomCommandProcessor customCommandProcessor;

    @Setter
    private Consumer<RemotingCommand> afterRegistryHandler = null;  // 注册成功后回调


    public NameserverClient(NameserverClientConfig clientConfig) {
        this(clientConfig, new NameClientProcessorAdaptor(clientConfig));
    }

    @Override
    protected void afterBoostrapConnected(Channel channel) {
        // 每次连接成功，都发送注册消息
        channel.attr(NettyConstants.CLIENT_NAME).set(getName());
        sendRegistryCommand();
    }

    public NameserverClient(NameserverClientConfig clientConfig, NameClientProcessorAdaptor nameClientProcessorAdaptor) {
        super(clientConfig.getRegistryCenterUrl(), clientConfig.getServiceInstance().toString(), nameClientProcessorAdaptor);
        super.setHandshake(false);
        this.clientConfig = clientConfig;
        this.clientName = clientConfig.getServiceInstance().toString();
        this.remoteStatus = new RemoteNameserverStatus();
        this.customCommandProcessor = new CustomCommandProcessor();

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
    protected void doSendMessage(RemotingCommand message, boolean b) {
        log.debug("send message to nameserver: {}", message);

        ServiceInstance serviceInstance = clientConfig.getServiceInstance();

        if (b && StrUtil.isBlank(message.getTransactionId())) {
            message.setTransactionId(clientName);
        }

        message.setGroup(serviceInstance.getGroup());
        message.setServiceId(serviceInstance.getServiceId());
        message.setClientId(serviceInstance.getInstanceId());

        Optional.of(getChannel()).ifPresent(channel -> channel.writeAndFlush(message));
    }

    /**
     * 发送服务注册命令
     */
    public void sendRegistryCommand() {
        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setFlag(RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE);
        remotingCommand.setTransactionId(
                DistributeIdMaker.DEFAULT.nextId(clientConfig.getServiceInstance().getServiceId())
        );

        remotingCommand.setBodyFromObject(new HashMap<>());

        sendRequest(remotingCommand).whenComplete((response, throwable) -> {
            if (throwable != null) {
                log.error("{} -> [{}] channel active error",
                        clientConfig.getServiceInstance(), clientConfig.getRegistryCenterUrl(), throwable);
            }

            // 注册成功
            if (response.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                log.info("{} client registry success", clientConfig.getServiceInstance());
                if (afterRegistryHandler != null) {
                    afterRegistryHandler.accept(response);
                }

                ServiceInstance nameserviceInstance = Serializer.Algorithm.Protostuff
                        .deserialize(response.getBody(), ServiceInstance.class);

                remoteStatus.setNameserverInstance(nameserviceInstance);
            } else {
                // 注册失败
                log.error("{} client registry failed, response: {}", clientConfig.getServiceInstance(), response);
                close();
            }

            remoteStatus.setLastUpdateTimestamp(System.currentTimeMillis());
            remoteStatus.setNameserverState(NameserverState.valueOf(
                    response.getExtFieldsValue(ExtFieldsConstants.NAMESERVER_STATUS)
            ));
        });
    }

    /**
     * 添加自定义命令处理器
     *
     * @param commandFlag          commandFlag 命令标识
     * @param customRequestHandler customRequestHandler    处理器
     */
    public void addCustomCommandHandler(String commandFlag, CustomRequestHandler customRequestHandler)
            throws CustomCommandException {
        customCommandProcessor.addCustomCommandHandler(commandFlag, customRequestHandler);
    }

    /**
     * 处理自定义请求
     *
     * @param remotingCommand request
     * @return response
     */
    public RemotingCommand tryResolveCustomRequest(RemotingCommand remotingCommand) {
        RemotingCommand response = null;
        try {
            response = customCommandProcessor.tryInvokeCustomCommandHandler(getChannel(), remotingCommand);
        } catch (Exception e) {
            log.error("custom request[{}] execute error, ", remotingCommand, e);
            response = new RemotingCommand();
            response.setTransactionId(remotingCommand.getTransactionId());
            response.setCode(RemotingCommandCodeConstants.FAIL);
            response.setBody(
                    Serializer.Algorithm.Protostuff.serialize(e)
            );
        }
        return response;
    }
}
