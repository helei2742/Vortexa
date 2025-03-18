package cn.com.vortexa.websocket.netty.handler;

import cn.com.vortexa.websocket.netty.base.AutoConnectWSService;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.com.vortexa.websocket.netty.constants.WebsocketClientStatus;
import io.netty.channel.*;

import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

/**
 * WebSocket客户端处理器抽象类
 * 能够处理请求响应类型的消息。
 * 其它类型的消息要通过handleOtherMessage()抽象方法处理
 *
 * @param <T>
 */
@Slf4j
@Getter
@ChannelHandler.Sharable
public abstract class AbstractWebSocketClientHandler<T> extends BaseWebSocketInboundHandler<T> {

    private WebSocketClientHandshaker handshake;
    private ChannelPromise handshakeFuture;
    protected AutoConnectWSService autoConnectWSService;

    public void init(
            AutoConnectWSService autoConnectWSService,
            WebSocketClientHandshaker handshake,
            ExecutorService executorService
    ) {
        super.init(executorService);
        this.autoConnectWSService = autoConnectWSService;
        this.handshake = handshake;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        if (handshake != null) {
            handshake.handshake(channel);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("WebSocket Client [{}] disconnected!", ctx.channel().attr(NettyConstants.CLIENT_NAME).get());
        autoConnectWSService.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.warn("WebSocket Client [{}] unregistered!", ctx.channel().attr(NettyConstants.CLIENT_NAME).get());
        autoConnectWSService.close();

        if (!autoConnectWSService.getClientStatus().equals(WebsocketClientStatus.SHUTDOWN)) {
            autoConnectWSService.reconnect();
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


    /**
     * 超过限定时间channel没有读写时触发
     *
     * @param ctx ctx
     */
    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        autoConnectWSService.sendPing();
    }

    /**
     * 连接完成
     *
     * @param ch ch
     */
    protected void connectCompleteHandler(Channel ch) {
        log.debug("websocket active");
    }


    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }
}
