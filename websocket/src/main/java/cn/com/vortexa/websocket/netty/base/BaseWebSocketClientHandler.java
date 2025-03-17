package cn.com.vortexa.websocket.netty.base;

import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.com.vortexa.websocket.netty.constants.WebsocketClientStatus;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * WebSocket客户端处理器基础类
 * 处理连接握手
 *
 * @param <Req>
 * @param <Resp>
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class BaseWebSocketClientHandler<Req, Resp, T> extends SimpleChannelInboundHandler<T> {

    @Getter
    private WebSocketClientHandshaker handshaker;

    @Getter
    private ChannelPromise handshakeFuture;

    protected AbstractWebsocketClient<Req, Resp, T> websocketClient;


    public void init(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        if (handshaker != null) {
            handshaker.handshake(channel);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("WebSocket Client [{}] disconnected!", ctx.channel().attr(NettyConstants.CLIENT_NAME).get());
        websocketClient.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.warn("WebSocket Client [{}] unregistered!", ctx.channel().attr(NettyConstants.CLIENT_NAME).get());
        websocketClient.close();

        if (!websocketClient.getClientStatus().equals(WebsocketClientStatus.SHUTDOWN)) {
            websocketClient.reconnect();
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (handshakeFuture != null && !handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
        log.error("业务处理错误，websocket client关闭", cause);
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
     * 连接完成
     *
     * @param ch ch
     */
    protected void connectCompleteHandler(Channel ch) {
        log.debug("websocket active");
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
        websocketClient.sendPing();
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }
}
