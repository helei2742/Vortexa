package cn.com.vortexa.control;

import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.control.config.ControlServerConfig;
import cn.com.vortexa.control.constant.ControlServerState;
import cn.com.vortexa.control.constant.WSControlSystemConstants;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.control.dto.ConnectEntry;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.RPCResultWrapper;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.ControlServerException;
import cn.com.vortexa.control.handler.CustomRequestHandler;
import cn.com.vortexa.control.processor.ControlServerProcessorAdaptor;
import cn.com.vortexa.common.util.protocol.Serializer;
import cn.com.vortexa.control.service.IConnectionService;
import cn.com.vortexa.control.service.IMetricsService;
import cn.com.vortexa.control.service.IRegistryService;
import cn.com.vortexa.control.processor.CustomCommandProcessor;
import cn.com.vortexa.control.service.impl.InfluxDBMetricsService;
import cn.com.vortexa.control.util.DistributeIdMaker;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.com.vortexa.control.util.RemotingCommandDecoder;
import cn.com.vortexa.control.util.RemotingCommandEncoder;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static cn.com.vortexa.control.constant.ControlServerState.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

@Slf4j
@Getter
public class BotControlServer {
    private final ControlServerConfig controlServerConfig;    // nameServerConfig
    private final CustomCommandProcessor customCommandProcessor;   // 自定义命令处理服务
    private final ExecutorService executorService;
    private final long startTime;   //启动时间
    private final Map<Integer, BiFunction<Channel, RemotingCommand, RemotingCommand>> customRemotingCommandHandlerMap = new HashMap<>();

    private volatile ControlServerState state; // name server state
    private ServerBootstrap serverBootstrap;    //serverBootstrap
    private ChannelFuture nameserverChannelFuture;  //nameserverChannelFuture

    private ControlServerProcessorAdaptor processorAdaptor;  // 消息处理器
    private IRegistryService registryService;   // 注册服务
    private IConnectionService connectionService;   // 连接服务
    private IMetricsService metricsService; // 指标服务

    public BotControlServer(ControlServerConfig controlServerConfig) throws ControlServerException {
        this(controlServerConfig, Executors.newThreadPerTaskExecutor(
                new NamedThreadFactory(controlServerConfig.getServiceInstance().toString())
        ));
    }

    public BotControlServer(
            ControlServerConfig controlServerConfig,
            ExecutorService executorService
    ) throws ControlServerException {
        this.controlServerConfig = controlServerConfig;
        updateNameServerState(JUST_START);
        this.startTime = System.currentTimeMillis();
        this.executorService = executorService;

        this.customCommandProcessor = new CustomCommandProcessor();
    }

    /**
     * 初始化
     *
     * @param registryService registryService
     */
    public void init(
            IRegistryService registryService,
            IConnectionService connectionService
    ) throws Exception {
        this.registryService = registryService;
        this.connectionService = connectionService;
        this.processorAdaptor = new ControlServerProcessorAdaptor(
                this,
                registryService
        );
        this.metricsService = new InfluxDBMetricsService();

        serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup(controlServerConfig.getNioThreadCount()), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, 65535)
                .childOption(ChannelOption.SO_RCVBUF, 65535)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(
                                0, 0, controlServerConfig.getServiceOfflineTtl()));

                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                                WSControlSystemConstants.MAX_FRAME_LENGTH, 0,
                                4, 0, 4));

                        ch.pipeline().addLast(new LengthFieldPrepender(4));

                        ch.pipeline().addLast(new RemotingCommandDecoder());
                        ch.pipeline().addLast(new RemotingCommandEncoder());
                        ch.pipeline().addLast(processorAdaptor);
                    }
                });

        updateNameServerState(INIT_FINISH);
    }

    /**
     * 启动
     *
     * @throws ControlServerException NameserverStartException
     */
    public ChannelFuture start() throws ControlServerException {
        log.info("start nameserver [{}], configuration:\n {}",
                controlServerConfig.getServiceInstance(), controlServerConfig);

        try {
            nameserverChannelFuture = serverBootstrap.bind(
                    controlServerConfig.getServiceInstance().getHost(),
                    controlServerConfig.getServiceInstance().getPort()
            );

            nameserverChannelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    updateNameServerState(RUNNING);
                }
            });

            return nameserverChannelFuture;
        } catch (Exception e) {
            updateNameServerState(SHUT_DOWN);
            throw new ControlServerException("start error", e);
        }
    }

    /**
     * 添加自定义命令处理器
     *
     * @param commandKey           commandKey 命令标识
     * @param customRequestHandler customRequestHandler    处理器
     */
    public void addCustomCommandHandler(String commandKey, CustomRequestHandler customRequestHandler)
            throws CustomCommandException {
        customCommandProcessor.addCustomCommandHandler(commandKey, customRequestHandler);
    }

    /**
     * 调用自定义命令handler
     *
     * @param channel channel
     * @param request request
     * @return response
     */
    public RemotingCommand tryInvokeCustomCommand(Channel channel, RemotingCommand request) {
        RemotingCommand response = null;

        try {
            // Step 1 校验是否注册服务
            String key = channel.attr(NettyConstants.CLIENT_NAME).get();

            if (!registryService.existServiceInstance(key)) {
                log.debug("channel[{}] didn't registry, drop request [{}]", key, request);
                closeChannel(channel);
                return null;
            }

            // Step 2 运行自定义命令
            response = customCommandProcessor.tryInvokeCustomCommandHandler(channel, request);

            if (response.getTransactionId() == null) {
                response.setTransactionId(request.getTransactionId());
            }
        } catch (Exception e) {
            log.error("client custom command[{}] execute error", request, e);

            response = new RemotingCommand();
            response.setTransactionId(request.getTransactionId());
            response.setFlag(RemotingCommandFlagConstants.CUSTOM_COMMAND_RESPONSE);
            response.setCode(RemotingCommandCodeConstants.FAIL);
            response.setBody(Serializer.Algorithm.JDK.serialize(new RPCResultWrapper<>(null, e)));
        }

        return response;
    }

    /**
     * 给服务实例发送命令
     *
     * @param group           group
     * @param serviceId       serviceId
     * @param instanceId      instanceId
     * @param remotingCommand remotingCommand
     * @return CompletableFuture<RemotingCommand>
     */
    public CompletableFuture<RemotingCommand> sendCommandToServiceInstance(
            String group,
            String serviceId,
            String instanceId,
            RemotingCommand remotingCommand
    ) {
        String key = ControlServerUtil.generateServiceInstanceKey(group, serviceId, instanceId);
        return sendCommandToServiceInstance(key, remotingCommand);
    }

    /**
     * 给服务实例发送命令
     *
     * @param key             key
     * @param remotingCommand remotingCommand
     * @return CompletableFuture<RemotingCommand>
     */
    public CompletableFuture<RemotingCommand> sendCommandToServiceInstance(
            String key, RemotingCommand remotingCommand
    ) {
        // Step 1 获取连接
        ConnectEntry connectEntry = connectionService.getServiceInstanceChannel(key);

        if (connectEntry == null || !connectEntry.isUsable()) {
            log.error("[{}] channel is unusable", key);
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<RemotingCommand> result = new AtomicReference<>();

                // Step 2 注册请求
                boolean registry = processorAdaptor.registryRequest(remotingCommand, response -> {
                    result.set(response);
                    latch.countDown();
                });

                connectEntry.getChannel().writeAndFlush(remotingCommand);
                latch.await();

                // Step 3 没有注册到请求，说明只是发的消息，唤醒线程
                if (!registry) {
                    result.set(null);
                    latch.countDown();
                }
                return result.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("send command %s error".formatted(remotingCommand), e);
            }
        }, executorService);
    }

    /**
     * 关闭连接某客户端的channel连接
     *
     * @param channel channel
     */
    public void closeChannel(Channel channel) {
        if (channel == null) {
            return;
        }

        String key = channel.attr(NettyConstants.CLIENT_NAME).get();

        if (registryService.existServiceInstance(key)) {
            connectionService.closeServiceChannel(channel, key);
        } else if (channel.isActive()) {
            channel.close();
        }
    }

    /**
     * 新建命令
     *
     * @param flag flag
     * @return RemotingCommand
     */
    public RemotingCommand newRemotingCommand(int flag, boolean needId) {
        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setFlag(flag);
        ServiceInstance serviceInstance = controlServerConfig.getServiceInstance();
        remotingCommand.setGroup(serviceInstance.getGroupId());
        remotingCommand.setServiceId(serviceInstance.getServiceId());
        remotingCommand.setInstanceId(serviceInstance.getInstanceId());
        remotingCommand.setTransactionId(needId ? nextTxId() : null);
        return remotingCommand;
    }

    public String nextTxId() {
        return DistributeIdMaker.DEFAULT.nextId(controlServerConfig.getServiceInstance().toString());
    }

    /**
     * 更新状态
     *
     * @param newState newState
     * @throws ControlServerException NameserverException
     */
    private void updateNameServerState(ControlServerState newState) throws ControlServerException {
        synchronized (this) {
            boolean isUpdate = switch (newState) {
                case JUST_START: {
                    yield state == null || state == JUST_START;
                }
                case INIT_FINISH:
                    yield state == JUST_START || state == INIT_FINISH;
                case RUNNING:
                    yield state == INIT_FINISH;
                case SHUT_DOWN:
                    yield state != SHUT_DOWN;
            };

            if (isUpdate) {
                log.info("nameserver[{}] status updated [{}]->[{}]", controlServerConfig.getServiceInstance(), state,
                        newState);
                state = newState;
            } else {
                throw new ControlServerException("state cannot from [%s] to [%s]".formatted(state, newState));
            }
        }
    }
}
