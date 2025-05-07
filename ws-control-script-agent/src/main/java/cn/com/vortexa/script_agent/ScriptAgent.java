package cn.com.vortexa.script_agent;

import cn.com.vortexa.script_agent.config.ScriptAgentConfig;
import cn.com.vortexa.control.constant.*;
import cn.com.vortexa.script_agent.dto.RemoteControlServerStatus;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.handler.CustomRequestHandler;
import cn.com.vortexa.control.processor.CustomCommandProcessor;
import cn.com.vortexa.script_agent.processor.ScriptAgentProcessorAdaptor;
import cn.com.vortexa.common.util.protocol.Serializer;
import cn.com.vortexa.script_agent.service.ScriptAgentMetricsUploadService;
import cn.com.vortexa.control.util.DistributeIdMaker;
import cn.com.vortexa.control.util.RemotingCommandDecoder;
import cn.com.vortexa.control.util.RemotingCommandEncoder;
import cn.com.vortexa.websocket.netty.base.AbstractWebsocketClient;
import cn.com.vortexa.websocket.netty.base.AutoConnectWSService;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author helei
 * @since 2025-03-13
 */
@Slf4j
@Getter
public class ScriptAgent extends AbstractWebsocketClient<RemotingCommand> {

    private final ScriptAgentConfig clientConfig;  // 配置
    private final ServiceInstance serviceInstance;
    private final RemoteControlServerStatus remoteStatus; // 远程服务命名中心状态
    private final CustomCommandProcessor customCommandProcessor;    // 自定义命令处理器
    private final ScriptAgentMetricsUploadService metricsUploadService; // 指标上传服务
    private final Map<Integer, BiFunction<Channel, RemotingCommand, RemotingCommand>> customRemotingCommandHandlerMap = new HashMap<>();

    @Setter
    private Supplier<Serializable> registryBodySetter = null; // 注册时的body
    @Setter
    private Consumer<RemotingCommand> afterRegistryHandler = null;  // 注册成功后回调

    public ScriptAgent(ScriptAgentConfig clientConfig) {
        this(clientConfig, new ScriptAgentProcessorAdaptor(clientConfig));
        // 设置无需重连
        super.setReconnectLimit(AutoConnectWSService.UN_LIMIT_RECONNECT_MARK);
    }

    public ScriptAgent(ScriptAgentConfig clientConfig, ScriptAgentProcessorAdaptor scriptAgentProcessorAdaptor) {
        super(clientConfig.getRegistryCenterUrl(), clientConfig.getServiceInstance().toString(),
                scriptAgentProcessorAdaptor);
        super.setHandshake(false);
        this.clientConfig = clientConfig;
        this.serviceInstance = clientConfig.getServiceInstance();
        super.setName(this.serviceInstance.toString());
        this.remoteStatus = new RemoteControlServerStatus();
        this.customCommandProcessor = new CustomCommandProcessor();

        this.metricsUploadService = new ScriptAgentMetricsUploadService(this, this.getCallbackInvoker());
        ((ScriptAgentProcessorAdaptor) getHandler()).setScriptAgent(this);
    }

    @Override
    protected void afterBoostrapConnected(Channel channel) {
        // 每次连接成功，都发送注册消息
        channel.attr(NettyConstants.CLIENT_NAME).set(getName());
        sendRegistryCommand();
    }

    @Override
    public void addPipeline(ChannelPipeline p) {
        p.addLast(new IdleStateHandler(0,
                0, clientConfig.getServiceOfflineTtl()));

        p.addLast(new LengthFieldBasedFrameDecoder(WSControlSystemConstants.MAX_FRAME_LENGTH,
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
            message.setTransactionId(nextTxId());
        }

        message.setGroup(serviceInstance.getGroupId());
        message.setServiceId(serviceInstance.getServiceId());
        message.setInstanceId(serviceInstance.getInstanceId());

        Channel channel;
        if ((channel = getChannel()) != null) {
            channel.writeAndFlush(message);
        } else {
            throw new RuntimeException("channel is null");
        }
    }

    /**
     * 构建ping命令
     *
     * @return RemotingCommand
     */
    public RemotingCommand buildPingCommand() {
        RemotingCommand ping = RemotingCommand.generatePingCommand(getName());
        ping.setTransactionId(
                DistributeIdMaker.DEFAULT.nextId(getName())
        );
        return ping;
    }

    /**
     * 发送服务注册命令
     */
    public void sendRegistryCommand() {
        RemotingCommand remotingCommand = newRequestCommand(RemotingCommandFlagConstants.CLIENT_REGISTRY_SERVICE);
        Serializable body = null;
        if (registryBodySetter != null) {
            body = registryBodySetter.get();
        }
        remotingCommand.setObjBody(body);

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
            remoteStatus.setControlServerState(ControlServerState.valueOf(
                    response.getExtFieldsValue(ExtFieldsConstants.NAMESERVER_STATUS)
            ));
        });
    }

    /**
     * 添加自定义命令处理器,flag为CUSTOM_COMMAND，参数的commandFlag是放RemotingCommand的extFields里的
     *
     * @param commandFlag          commandFlag 命令标识
     * @param customRequestHandler customRequestHandler    处理器
     */
    public void addCustomCommandHandler(String commandFlag, CustomRequestHandler customRequestHandler)
            throws CustomCommandException {
        customCommandProcessor.addCustomCommandHandler(commandFlag, customRequestHandler);
    }

    /**
     * 添加自定义远程命令处理器
     * 与addCustomCommandHandler(String commandFlag, CustomRequestHandler customRequestHandler)不同的是，
     * 这个注册的RemotingCommand 的flag为 commandFlag
     *
     * @param commandFlag commandFlag
     * @param handler     handler
     */
    public void addCustomRemotingCommandHandler(
            Integer commandFlag,
            BiFunction<Channel, RemotingCommand, RemotingCommand> handler
    ) {
        customRemotingCommandHandlerMap.put(commandFlag, handler);
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

    /**
     * 构建命令
     *
     * @param commandFlag commandFlag
     * @return RemotingCommand
     */
    public RemotingCommand newRequestCommand(int commandFlag) {
        return newRequestCommand(commandFlag, true);
    }

    /**
     * 构建命令
     *
     * @param commandFlag commandFlag
     * @return RemotingCommand
     */
    public RemotingCommand newRequestCommand(int commandFlag, boolean needTxId) {
        RemotingCommand command = new RemotingCommand();
        command.setGroup(serviceInstance.getGroupId());
        command.setServiceId(serviceInstance.getServiceId());
        command.setInstanceId(serviceInstance.getInstanceId());
        command.setTransactionId(needTxId ? nextTxId() : null);

        command.setFlag(commandFlag);
        return command;
    }

    /**
     * 获取下一个事务id
     *
     * @return String
     */
    public String nextTxId() {
        return DistributeIdMaker.DEFAULT.nextId(getName());
    }
}
