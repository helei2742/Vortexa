package cn.com.vortexa.nameserver.server;

import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.nameserver.config.NameserverServerConfig;
import cn.com.vortexa.nameserver.constant.NameserverState;
import cn.com.vortexa.nameserver.constant.NameserverSystemConstants;
import cn.com.vortexa.nameserver.dto.ConnectEntry;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.exception.CustomCommandException;
import cn.com.vortexa.nameserver.exception.NameserverException;
import cn.com.vortexa.nameserver.handler.CustomRequestHandler;
import cn.com.vortexa.nameserver.processor.NameserverProcessorAdaptor;
import cn.com.vortexa.nameserver.service.IConnectionService;
import cn.com.vortexa.nameserver.service.IRegistryService;
import cn.com.vortexa.nameserver.processor.CustomCommandProcessor;
import cn.com.vortexa.nameserver.util.NameserverUtil;
import cn.com.vortexa.nameserver.util.RemotingCommandDecoder;
import cn.com.vortexa.nameserver.util.RemotingCommandEncoder;
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

import static cn.com.vortexa.nameserver.constant.NameserverState.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
public class NameserverService {
    private final NameserverServerConfig nameserverServerConfig;    // nameServerConfig
    private final CustomCommandProcessor customCommandProcessor;   // 自定义命令处理服务
    private final ExecutorService executorService;
    private final long startTime;   //启动时间

    private volatile NameserverState state; // name server state
    private ServerBootstrap serverBootstrap;    //serverBootstrap
    private ChannelFuture nameserverChannelFuture;  //nameserverChannelFuture

    private NameserverProcessorAdaptor processorAdaptor;  // 消息处理器
    private IRegistryService registryService;   // 注册服务
    private IConnectionService connectionService;   // 连接服务

    public NameserverService(NameserverServerConfig nameserverServerConfig) throws NameserverException {
        this.nameserverServerConfig = nameserverServerConfig;
        updateNameServerState(JUST_START);
        this.startTime = System.currentTimeMillis();
        this.executorService = Executors.newThreadPerTaskExecutor(
                new NamedThreadFactory(nameserverServerConfig.getServiceInstance().toString())
        );

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
    ) throws NameserverException {
        this.registryService = registryService;
        this.connectionService = connectionService;
        this.processorAdaptor = new NameserverProcessorAdaptor(
                this,
                registryService
        );

        serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup(nameserverServerConfig.getNioThreadCount()), new NioEventLoopGroup())
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
                                0, 0, nameserverServerConfig.getServiceOfflineTtl()));

                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                                NameserverSystemConstants.MAX_FRAME_LENGTH, 0,
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
     * @throws NameserverException NameserverStartException
     */
    public ChannelFuture start() throws NameserverException {
        log.info("start nameserver [{}], configuration:\n {}",
                nameserverServerConfig.getServiceInstance(), nameserverServerConfig);

        try {
            nameserverChannelFuture = serverBootstrap.bind(
                    nameserverServerConfig.getServiceInstance().getHost(),
                    nameserverServerConfig.getServiceInstance().getPort()
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
            throw new NameserverException("start error", e);
        }
    }

    /**
     * 添加自定义命令处理器
     *
     * @param commandFlag          commandFlag 命令标识
     * @param customRequestHandler customRequestHandler    处理器
     */
    public void addCustomCommandHandler(Integer commandFlag, CustomRequestHandler customRequestHandler)
            throws CustomCommandException {
        customCommandProcessor.addCustomCommandHandler(commandFlag, customRequestHandler);
    }

    /**
     * 调用自定义命令handler
     *
     * @param channel channel
     * @param request request
     * @return response
     */
    public RemotingCommand tryInvokeCustomCommand(Channel channel, RemotingCommand request)
            throws CustomCommandException {
        // Step 1 校验是否注册服务
        String key = channel.attr(NettyConstants.CLIENT_NAME).get();

        if (!registryService.existServiceInstance(key)) {
            log.debug("channel[{}] didn't registry, drop request [{}]", key, request);
            closeChannel(channel);
            return null;
        }

        // Step 2 运行自定义命令
        RemotingCommand response = customCommandProcessor.tryInvokeCustomCommandHandler(request);
        if (response.getTransactionId() == null) {
            response.setTransactionId(request.getTransactionId());
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
        // Step 1 获取连接
        String key = NameserverUtil.generateServiceInstanceKey(group, serviceId, instanceId);
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

                if (registry) {
                    connectEntry.getChannel().writeAndFlush(remotingCommand);
                }

                latch.await();
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
     * 更新状态
     *
     * @param newState newState
     * @throws NameserverException NameserverException
     */
    private void updateNameServerState(NameserverState newState) throws NameserverException {
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
                log.info("nameserver[{}] status updated [{}]->[{}]", nameserverServerConfig.getServiceInstance(), state,
                        newState);
                state = newState;
            } else {
                throw new NameserverException("state cannot from [%s] to [%s]".formatted(state, newState));
            }
        }
    }
}
