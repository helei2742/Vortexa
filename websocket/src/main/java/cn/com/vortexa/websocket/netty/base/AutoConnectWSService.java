package cn.com.vortexa.websocket.netty.base;

import cn.com.vortexa.common.entity.ProxyInfo;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.com.vortexa.websocket.netty.constants.WebsocketClientStatus;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

/**
 * @author helei
 * @since 2025/3/18 10:52
 */
@Slf4j
@Setter
@Getter
public abstract class AutoConnectWSService implements IWSService {
    private final AtomicInteger reconnectTimes = new AtomicInteger(0);  //重链接次数
    private final ReentrantLock reconnectLock = new ReentrantLock();    //重连锁
    private final Condition startingWaitCondition = reconnectLock.newCondition();   //启动中阻塞的condition
    private final EventLoopGroup eventLoopGroup;    //netty线程组
    private final String url; //websocket的url字符串
    private Bootstrap bootstrap; //netty bootstrap
    private Channel channel;    //连接channel
    private String host;    //连接host
    private int port;    //连接port
    private boolean useSSL; //是否ssl
    private HttpHeaders headers;    //请求头
    private ProxyInfo proxy = null; //代理
    private int reconnectCountDownSecond = 180; //重连次数减少的间隔
    private int reconnectLimit = 3; //重连次数限制
    private volatile WebsocketClientStatus clientStatus = WebsocketClientStatus.NEW;    //客户端当前状态
    private Consumer<WebsocketClientStatus> clientStatusChangeHandler = socketCloseStatus -> {
    };    //clientStatus更新的回调

    protected AutoConnectWSService(String url) {
        this.url = url;
        this.eventLoopGroup = new NioEventLoopGroup();
    }

    protected abstract void init() throws SSLException, URISyntaxException;

    /**
     * 连接
     *
     * @return CompletableFuture<Boolean>
     */
    public final CompletableFuture<Boolean> connect() {
        return switch (clientStatus) {
            case NEW, STOP -> reconnect();
            case STARTING -> waitForStarting();
            case RUNNING -> {
                log.warn("ws service [{}] in running, clientStatus[{}]", getName(), clientStatus);
                yield CompletableFuture.supplyAsync(() -> true);
            }
            case SHUTDOWN -> throw new RuntimeException("");
        };
    }

    /**
     * 重连接
     *
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> reconnect() {
        return switch (clientStatus) {
            case NEW, STOP -> reconnectLogic();
            case STARTING -> waitForStarting().thenApplyAsync(success -> {
                if (success) {
                    return true;
                }
                try {
                    return reconnectLogic().get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
            case RUNNING -> {
                log.warn("ws service [{}] in running, can't reconnect. clientStatus[{}]", getName(), clientStatus);
                yield CompletableFuture.supplyAsync(() -> true);
            }
            case SHUTDOWN -> CompletableFuture.supplyAsync(() -> {
                log.error("client[{}] already shutdown", getName());
                return false;
            });
        };
    }

    /**
     * 重连接逻辑
     *
     * @return CompletableFuture<Boolean>
     */
    private CompletableFuture<Boolean> reconnectLogic() {
        updateClientStatus(WebsocketClientStatus.STARTING);

        String name = getName();

        return CompletableFuture.supplyAsync(() -> {
            //Step 1 重连次数超过限制，关闭
            if (reconnectTimes.get() >= reconnectLimit) {
                log.error("reconnect times out of limit [{}], close websocket client", reconnectLimit);
                shutdown();
                return false;
            }

            AtomicBoolean isSuccess = new AtomicBoolean(false);

            //Step 2 重连逻辑
            //Step 2.1 加锁保证只要一个线程进行重连
            reconnectLock.lock();
            try {
                //Step 2.2 已经再running状态，直接返回true。变为shutdown、stop状态，直接返回false
                if (clientStatus.equals(WebsocketClientStatus.RUNNING)) {
                    log.info("client started by other thread");
                    return true;
                } else if (clientStatus.equals(WebsocketClientStatus.SHUTDOWN)) {
                    log.error("clint {} when client starting", clientStatus);
                    return false;
                }

                //Step 3 初始化
                log.info("start init Websocket client");
                try {
                    resolveParamFromUrl();

                    init();
                } catch (SSLException | URISyntaxException e) {
                    throw new RuntimeException("init websocket clioent error", e);
                }
                log.info("init Websocket finish，start connect server [{}]", url);

                //Step 4 链接服务器
                if (reconnectTimes.incrementAndGet() <= reconnectLimit) {

                    //Step 4.1 每进行重连都会先将次数加1并设置定时任务将重连次数减1
                    eventLoopGroup.schedule(() -> {
                        reconnectTimes.decrementAndGet();
                    }, reconnectCountDownSecond, TimeUnit.SECONDS);

                    long waitingConnectTime = reconnectTimes.get() == 1 ? 0 : NettyConstants.RECONNECT_DELAY_SECONDS;
                    log.info("start connect client [{}], url[{}], current times [{}], start after [{}]s",
                            name, url, reconnectTimes.get(), waitingConnectTime);

                    //Step 4.2 latch用于同步等等链接完成
                    CountDownLatch latch = new CountDownLatch(1);

                    //Step 4.3 延迟再进行连接
                    eventLoopGroup.schedule(() -> {
                        try {
                            channel = bootstrap.connect(host, port).sync().channel();

                            afterBoostrapConnected(channel);

                            log.info("success connect to {}", url);
                            //Step 4.4 连接成功设置标识
                            isSuccess.set(true);
                        } catch (Exception e) {
                            log.error("connect client [{}], url[{}] error, times [{}]", name, url, reconnectTimes.get(),
                                    e);
                            isSuccess.set(false);
                        } finally {
                            latch.countDown();
                        }
                    }, waitingConnectTime, TimeUnit.SECONDS);

                    //Step 4.5 等待链接完成
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        log.error("connect client [{}], url[{}] interrupted, times [{}]", name, url,
                                reconnectTimes.get(), e);
                    }

                    //Step 6 未成功启动，关闭
                    if (!isSuccess.get()) {
                        log.info("connect client [{}], url[{}] fail, current times [{}]", name, url,
                                reconnectTimes.get());

                        close();
                    } else {
                        log.info("connect client [{}], url[{}] success, current times [{}]", name, url,
                                reconnectTimes.get());

                        updateClientStatus(WebsocketClientStatus.RUNNING);
                        reconnectTimes.set(0);
                    }
                }
            } catch (Exception e) {
                //exception 遇到未处理异常，直接关闭
                close();
                throw new RuntimeException(String.format("connect client [%s] appear unknown error", name), e);
            } finally {
                //Step 5 释放等待启动的线程
                startingWaitCondition.signalAll();
                reconnectLock.unlock();
            }

            return isSuccess.get();
        }, getCallbackInvoker());
    }

    /**
     * 关闭
     */
    public void close() {
        if (clientStatus.equals(WebsocketClientStatus.STOP)
                || clientStatus.equals(WebsocketClientStatus.SHUTDOWN)) {
            return;
        }

        updateClientStatus(WebsocketClientStatus.STOP);

        log.info("closing websocket client [{}], [{}]", getName(), channel == null ? "null" : getChannel().hashCode());
        if (channel != null) {
            channel.close();
            channel = null;
        }
        log.warn("web socket client [{}] closed", getName());
    }

    /**
     * 终止
     */
    public final void shutdown() {
        updateClientStatus(WebsocketClientStatus.SHUTDOWN);

        if (channel != null) {
            channel.close();
            channel = null;
        }
        eventLoopGroup.shutdownGracefully();
        log.warn("web socket client [{}] already shutdown !", getName());
    }

    /**
     * 更新status
     *
     * @param newStatus newStatus
     */
    protected void updateClientStatus(WebsocketClientStatus newStatus) {
        synchronized ("CLIENT_STATUS_LOCK") {
            if (clientStatus.equals(newStatus)) {
                return;
            }

            if (clientStatus.equals(WebsocketClientStatus.SHUTDOWN)) {
                throw new IllegalArgumentException("client status is shutdown，new status can not be " + newStatus);
            }

            try {
                if (newStatus != clientStatus && clientStatusChangeHandler != null) {
                    clientStatusChangeHandler.accept(newStatus);
                }
            } finally {
                log.debug("client[{}] status [{}] -> [{}]", getName(), clientStatus, newStatus);
                clientStatus = newStatus;
            }
        }
    }

    /**
     * 获取回调执行线程池
     *
     * @return ExecutorService
     */
    public abstract ExecutorService getCallbackInvoker();

    /**
     * 每次netty bootstrap 连接后调用
     *
     * @throws InterruptedException InterruptedException
     */
    protected abstract void afterBoostrapConnected(Channel channel) throws InterruptedException;

    /**
     * 等待启动完成
     *
     * @return CompletableFuture<Void>
     */
    private CompletableFuture<Boolean> waitForStarting() {
        return CompletableFuture.supplyAsync(() -> {
            log.warn("client [{}] is starting, waiting for complete", getName());
            reconnectLock.lock();
            try {
                while (clientStatus.equals(WebsocketClientStatus.STARTING)) {
                    startingWaitCondition.await();
                }

                if (clientStatus.equals(WebsocketClientStatus.STOP) || clientStatus.equals(
                        WebsocketClientStatus.SHUTDOWN)) {
                    log.error("启动WS客户端[{}]失败, ClientStatus [{}}", getName(), clientStatus);
                    return false;
                }
                return true;
            } catch (InterruptedException e) {
                log.error("waiting for start client [{}] error", getName());
                throw new RuntimeException(e);
            } finally {
                reconnectLock.unlock();
            }
        }, getCallbackInvoker());
    }

    /**
     * 解析参数
     *
     * @throws URISyntaxException url解析错误
     */
    private void resolveParamFromUrl() throws URISyntaxException {
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            log.error("Only WS(S) is supported.");
            throw new IllegalArgumentException("url error, Only WS(S) is supported.");
        }

        useSSL = "wss".equalsIgnoreCase(scheme);
    }
}
