package cn.com.vortexa.script_node.websocket;

import cn.com.vortexa.common.dto.ConnectStatusInfo;
import cn.com.vortexa.websocket.netty.handler.AbstractWebSocketClientHandler;
import cn.com.vortexa.websocket.netty.util.HandlerEntity;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseBotWSClientHandler<T> extends AbstractWebSocketClientHandler<T> {

    @Override
    protected void handlerMessage(ChannelHandlerContext ctx, T msg) {
        Channel ch = ctx.channel();
        // 如果握手未完成，处理 FullHttpResponse
        if (getHandshake() != null && !getHandshake().isHandshakeComplete()) {
            if (msg instanceof FullHttpResponse response) {
                try {

                    getHandshake().finishHandshake(ch, response);
                    log.info("WebSocket client [{}] Handshake complete!", ch.attr(NettyConstants.CLIENT_NAME).get());
                    getHandshakeFuture().setSuccess();

                    connectCompleteHandler(ch);
                } catch (WebSocketHandshakeException e) {
                    log.info("WebSocket client [{}] Handshake failed!", ch.attr(NettyConstants.CLIENT_NAME).get());
                    getHandshakeFuture().setFailure(e);
                }
                return;
            }
        }

        if (msg instanceof FullHttpResponse response) {
            if (response.status().code() / 100 > 3) {
                throw new IllegalStateException(
                        "Unexpected FullHttpResponse (getStatus=" + response.status() +
                                ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
            }
        } else if (msg instanceof WebSocketFrame frame) {
            switch (frame) {
                case TextWebSocketFrame textFrame -> {
                    log.debug("websocket client [{}] 接收到的消息：{}", ch.attr(NettyConstants.CLIENT_NAME).get(), textFrame.text());
                    whenReceiveMessage(textFrame.text());
                }
                case PongWebSocketFrame pongWebSocketFrame -> handlerPong(ch, pongWebSocketFrame);
                case PingWebSocketFrame pingWebSocketFrame -> handlerPing(ch, pingWebSocketFrame);
                case CloseWebSocketFrame closeWebSocketFrame -> handlerClose(ch, closeWebSocketFrame);
                default -> {
                    log.warn("channel[{}]收到位置类型的消息[{}]", ch.attr(NettyConstants.CLIENT_NAME).get(), frame.getClass().getName());
                }
            }
        }
    }

    /**
     * channel 空闲，向其发送心跳
     *
     * @param ctx ctx
     */
    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        BaseBotWSClient<T> botWSClient = getBotWSClient();


        T heartbeatMessage = botWSClient.getHeartbeatMessage();
        if (heartbeatMessage != null) {
            botWSClient
                    .sendMessage(heartbeatMessage)
                    .whenCompleteAsync((unused, throwable) -> {
                        ConnectStatusInfo connectStatusInfo = botWSClient
                                .getAccountContext()
                                .getConnectStatusInfo();

                        if (throwable != null) {
                            log.error("client[{}] 发送心跳异常", autoConnectWSService.getName(), throwable);
                            // 发送心跳失败，记录次数
                            connectStatusInfo.getErrorHeartBeat().getAndIncrement();
                        }

                        // 心跳计数
                        connectStatusInfo.getHeartBeat()
                                .getAndIncrement();
                    }, botWSClient.getCallbackInvoker());
        }
    }

    /**
     * 收到消息时调用
     *
     * @param text text
     */
    protected void whenReceiveMessage(String text) {
        T message = convertMessageToRespType(text);

        Object responseId = getMessageId(message);

        if (responseId != null) {
            //有id，是发送请求的响应
            //提交response
            handleResponseMessage(responseId, message);
        } else {
            //没有id，按其它格式处理
            handleOtherMessage(message);
        }
    }


    /**
     * 将websocket收到的文本消息转换为响应类型 Resp
     *
     * @param message websocket收到的原始消息
     * @return typedMessage
     */
    public abstract T convertMessageToRespType(String message);


    /**
     * 处理请求响应的消息
     *
     * @param id       id
     * @param response 响应消息体
     */
    protected void handleResponseMessage(Object id, T response) {
        HandlerEntity<T> handlerEntity = requestIdMap.get(id);

        if (System.currentTimeMillis() > handlerEntity.getExpireTime()) {
            log.warn("请求[{}]得到响应超时", id);
        } else {
            autoConnectWSService.getCallbackInvoker().execute(() -> handlerEntity.getCallback().accept(response));
        }
    }

    /**
     * 处理其他类型消息
     *
     * @param message 消息
     */
    protected void handleOtherMessage(T message) {
        getBotWSClient().whenAccountReceiveMessage(message);
    }

    /**
     * 处理close消息
     *
     * @param ch   Channel ch
     * @param ping closeWebSocketFrame
     */
    protected void handlerClose(Channel ch, CloseWebSocketFrame ping) {
        log.warn("websocket client关闭");
        ch.close();
    }


    /**
     * 处理pong消息
     *
     * @param ch                 Channel ch
     * @param pongWebSocketFrame pongWebSocketFrame
     */
    protected void handlerPong(Channel ch, PongWebSocketFrame pongWebSocketFrame) {
        log.info("WebSocket Client [{}] received pong", ch.attr(NettyConstants.CLIENT_NAME).get());
    }


    /**
     * 处理ping消息
     *
     * @param ch                 ch
     * @param pingWebSocketFrame pingWebSocketFrame
     */
    protected void handlerPing(Channel ch, PingWebSocketFrame pingWebSocketFrame) {
        log.info("WebSocket Client [{}] received ping", ch.attr(NettyConstants.CLIENT_NAME).get());
    }

    private BaseBotWSClient<T> getBotWSClient() {
        return (BaseBotWSClient<T>) autoConnectWSService;
    }
}
