package cn.com.vortexa.websocket.netty.base;

import cn.com.vortexa.common.entity.ProxyInfo;
import cn.com.vortexa.common.util.NamedThreadFactory;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.com.vortexa.websocket.netty.handler.AbstractWebSocketClientHandler;
import cn.hutool.core.util.StrUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Websocket客户端
 */
@Slf4j
@Setter
@Getter
public abstract class AbstractWebsocketClient<T> extends AutoConnectWSService {
    private static final int MAX_FRAME_SIZE = 10 * 1024 * 1024;  // 10 MB or set to your desired size

    private final AbstractWebSocketClientHandler<T> handler;   //netty pipeline 最后一个执行的handler
    private final ExecutorService callbackInvoker;  //执行回调的线程池
    private String name;
    private boolean handshake = true;
    private int allIdleTimeSecond = 10; //空闲时间

    public AbstractWebsocketClient(
            String url,
            String name,
            AbstractWebSocketClientHandler<T> handler
    ) {
        super(url);
        this.name = name;
        this.handler = handler;
        this.callbackInvoker = Executors.newThreadPerTaskExecutor(new NamedThreadFactory(name));
    }

    @Override
    protected void afterBoostrapConnected(Channel channel) throws InterruptedException {
        if (handshake && handler.handshakeFuture() != null) {
            handler.handshakeFuture().sync();
        }
    }

    @Override
    protected void init() throws SSLException, URISyntaxException {
        URI uri = new URI(getUrl());

        WebSocketClientHandshaker webSocketClientHandshaker = handshake ? WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, getHeaders(), MAX_FRAME_SIZE
        ) : null;

        handler.init(this, webSocketClientHandshaker, callbackInvoker);

        final SslContext sslCtx;
        if (isUseSSL()) {
            sslCtx = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } else {
            sslCtx = null;
        }

        setBootstrap(new Bootstrap());
        getBootstrap().group(getEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 设置连接超时为10秒
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        ProxyInfo proxy = getProxy();

                        if (proxy != null) {
                            // 添加 HttpProxyHandler 作为代理
                            if (StrUtil.isNotBlank(proxy.getUsername())) {
                                p.addFirst(new HttpProxyHandler(proxy.generateAddress(), proxy.getUsername(),
                                        proxy.getPassword()));
                            } else {
                                p.addFirst(new HttpProxyHandler(proxy.generateAddress()));
                            }
                        }

                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
                        }

                        addPipeline(p);
                    }
                });
    }

    /**
     * 添加 pipeline
     *
     * @param p p
     */
    public abstract void addPipeline(ChannelPipeline p);

    /**
     * 发送消息，没有回调
     *
     * @param message message
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> sendMessage(T message) {
        return CompletableFuture.runAsync(() -> {
            if (message == null) {
                throw new IllegalArgumentException("message is null");
            }

            doSendMessage(message, false);
        }, callbackInvoker);
    }

    /**
     * 发送请求, 注册响应监听
     *
     * @param request 请求体
     */
    public CompletableFuture<T> sendRequest(T request) {
        log.debug("send request [{}]", request);

        return CompletableFuture.supplyAsync(() -> {
            if (request == null) {
                log.error("request is null");
                return null;
            }

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<T> jb = new AtomicReference<>(null);

            boolean flag = handler.registryRequest(request, response -> {
                jb.set(response);
                latch.countDown();
            });

            if (flag) {
                doSendMessage(request, true);
            } else {
                log.error("request id registered");
                return null;
            }

            try {
                if (!latch.await(NettyConstants.REQUEST_WAITE_SECONDS, TimeUnit.SECONDS)) {
                    return null;
                }

                return jb.get();
            } catch (InterruptedException e) {
                log.error("send request interrupted", e);
                return null;
            }
        }, callbackInvoker);
    }

    /**
     * 发送消息
     *
     * @param message   message
     * @param isRequest isRequest
     */
    protected void doSendMessage(T message, boolean isRequest) {
        try {
            getChannel().writeAndFlush(message);
            log.debug("send request [{}] success", message);
        } catch (Exception e) {
            throw new RuntimeException("send message [" + message + "] error");
        }
    }
}
