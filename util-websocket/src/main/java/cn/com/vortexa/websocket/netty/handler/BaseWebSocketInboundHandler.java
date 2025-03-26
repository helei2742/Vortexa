package cn.com.vortexa.websocket.netty.handler;

import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.com.vortexa.websocket.netty.util.HandlerEntity;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * WebSocket客户端处理器基础类
 * 处理连接握手
 */
@Slf4j
@Getter
@ChannelHandler.Sharable
public abstract class BaseWebSocketInboundHandler<T> extends SimpleChannelInboundHandler<T> {

    /**
     * 存放请求响应的回调
     */
    protected final ConcurrentMap<Object, HandlerEntity<T>> requestIdMap = new ConcurrentHashMap<>();

    /**
     * 执行回调的线程池
     */
    private ExecutorService callbackInvoker;

    protected void init(ExecutorService callbackInvoker) {
        this.callbackInvoker = callbackInvoker;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    protected final void channelRead0(ChannelHandlerContext ctx, T message) throws Exception {
        Channel channel = ctx.channel();
        log.debug("{} -> {}  message: {}", channel.remoteAddress(), channel.localAddress(), message);
        // Step 1 尝试调用请求回调
        tryInvokeResponseCallback(message)
                .thenAccept(success -> {
                    // Step 2 返回false，说明不是是请求的响应, 往下处理
                    if (!success) {
                        handlerMessage(ctx, message);
                    }
                })
                .exceptionally(throwable -> {
                    if (throwable != null) {
                        log.error("request callback invoke error", throwable);
                    }
                    return null;
                });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // IdleStateHandler 所产生的 IdleStateEvent 的处理逻辑.
        if (evt instanceof IdleStateEvent e) {
            switch (e.state()) {
                case READER_IDLE:
                    handleReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    handleWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    handleAllIdle(ctx);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 注册request
     *
     * @param request request
     * @return 是否注册成功
     */
    public boolean registryRequest(T request, Consumer<T> callback) {
        AtomicBoolean res = new AtomicBoolean(false);
        Object requestId = getMessageId(request);

        if (requestId == null) {
            return false;
        }

        requestIdMap.compute(requestId, (k, v) -> {
            if (v == null) {
                res.set(true);
                long expireTime = System.currentTimeMillis() + NettyConstants.REQUEST_WAITE_SECONDS * 1000;
                v = new HandlerEntity<>(expireTime, callback);
                log.debug("registry request id[{}] success, expire time [{}]", requestId, expireTime);
            }
            return v;
        });

        return res.get();
    }

    /**
     * 调用响应的回调
     *
     * @param response response
     */
    public CompletableFuture<Boolean> tryInvokeResponseCallback(T response) {
        Object responseId = getMessageId(response);
        log.debug("responseId[{}], {}", responseId, response);

        if (responseId == null || !requestIdMap.containsKey(responseId)) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HandlerEntity<T> remove = requestIdMap.remove(responseId);
                if (remove != null) {
                    remove.getCallback().accept(response);
                }
            } catch (Exception e) {
                log.error("response callback invoke error", e);
            }
            return true;
        }, callbackInvoker);
    }

    /**
     * 超过限定时间channel没有读时触发
     *
     * @param ctx ctx
     */
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
    }

    /**
     * 超过限定时间channel没有写时触发
     *
     * @param ctx ctx
     */
    protected void handleWriterIdle(ChannelHandlerContext ctx) {
    }

    /**
     * 超过限定时间channel没有读写时触发
     *
     * @param ctx ctx
     */
    protected void handleAllIdle(ChannelHandlerContext ctx) {
    }

    /**
     * 从消息中取id
     *
     * @param message message
     * @return id
     */
    protected abstract Object getMessageId(T message);

    /**
     * 处理消息
     *
     * @param ctx     ctx
     * @param message message
     */
    protected abstract void handlerMessage(ChannelHandlerContext ctx, T message);
}
