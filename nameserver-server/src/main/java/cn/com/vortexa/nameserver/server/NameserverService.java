package cn.com.vortexa.nameserver.server;


import cn.com.vortexa.nameserver.config.NameserverServerConfig;
import cn.com.vortexa.nameserver.constant.NameServerState;
import cn.com.vortexa.nameserver.constant.NameserverSystemConstants;
import cn.com.vortexa.nameserver.exception.NameserverException;
import cn.com.vortexa.nameserver.processor.NameserverProcessorAdaptor;
import cn.com.vortexa.nameserver.util.RemotingCommandDecoder;
import cn.com.vortexa.nameserver.util.RemotingCommandEncoder;
import cn.com.vortexa.websocket.netty.base.NettyClientEventHandler;
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
import org.jetbrains.annotations.NotNull;

import static cn.com.vortexa.nameserver.constant.NameServerState.*;

@Slf4j
public class NameserverService {

    /**
     * nameServerConfig
     */
    @Getter
    private final NameserverServerConfig nameserverServerConfig;

    /**
     * 启动时间
     */
    @Getter
    private final long startTime;

    /**
     * serverBootstrap
     */
    private ServerBootstrap serverBootstrap;

    /**
     * nameserverChannelFuture
     */
    @Getter
    private ChannelFuture nameserverChannelFuture;

    /**
     * name server state
     */
    private volatile NameServerState state;

    /**
     * 事件处理器
     */
    private NettyClientEventHandler eventHandler;


    public NameserverService(NameserverServerConfig nameserverServerConfig) throws NameserverException {
        this.nameserverServerConfig = nameserverServerConfig;

        updateNameServerState(JUST_START);
        this.startTime = System.currentTimeMillis();

        if (eventHandler == null) {
            eventHandler = generateDefaultEventHandler();
        }
    }

    /**
     * 启动
     *
     * @throws NameserverException NameserverStartException
     */
    public void start() throws NameserverException {
        log.info("start nameserver [{}], configuration:\n {}",
                nameserverServerConfig.getServiceInstance(), nameserverServerConfig);

        try {
            init(nameserverServerConfig);

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

        } catch (Exception e) {
            updateNameServerState(SHUT_DOWN);
            throw new NameserverException("start error", e);
        }
    }

    public String getName() {
        return nameserverServerConfig.getServiceInstance().getGroup() + ":" + nameserverServerConfig.getServiceInstance().getServiceId();
    }


    /**
     * 初始化
     *
     * @param nameserverServerConfig nameServerConfig
     */
    private void init(NameserverServerConfig nameserverServerConfig) {
        NameserverProcessorAdaptor adaptor = new NameserverProcessorAdaptor(this, eventHandler);

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

                        ch.pipeline().addLast(adaptor);
                    }
                });
    }

    /**
     * 更新状态
     *
     * @param newState newState
     * @throws NameserverException NameserverException
     */
    private void updateNameServerState(NameServerState newState) throws NameserverException {
        synchronized (this) {
            boolean isUpdate = switch (newState) {
                case JUST_START: {
                    yield state == null || state == JUST_START;
                }
                case RUNNING:
                    yield state == JUST_START;
                case SHUT_DOWN:
                    yield state != SHUT_DOWN;
            };

            if (isUpdate) {
                log.info("nameserver[{}] status updated [{}]->[{}]", nameserverServerConfig.getServiceInstance(), state, newState);
                state = newState;
            } else {
                throw new NameserverException("state cannot from [%s] to [%s]".formatted(state, newState));
            }
        }
    }



    @NotNull
    private static NettyClientEventHandler generateDefaultEventHandler() {
        return new NettyClientEventHandler() {
            @Override
            public void activeHandler(ChannelHandlerContext ctx) {
                log.info("[{}]-[{}] connected to nameserver",
                        ctx.channel().attr(NettyConstants.CLIENT_NAME).get(), ctx.channel().remoteAddress());
            }

            @Override
            public void exceptionHandler(ChannelHandlerContext ctx, Throwable cause) {
                log.error("nameserver error", cause);
            }
        };
    }
}
