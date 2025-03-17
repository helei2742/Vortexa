package cn.com.vortexa.bot_father.websocket;

import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public abstract class BotJsonWSClient extends BaseBotWSClient<JSONObject, JSONObject> {

    private String idFieldName = "id";

    public BotJsonWSClient(
            AccountContext accountContext,
            String connectUrl
    ) {
        super(accountContext, connectUrl, new SimpleBotWSClientHandler());
        ((SimpleBotWSClientHandler) handler).setWsClient(this);
    }

    @Override
    public Object getRequestId(JSONObject request) {
        return request.get(idFieldName);
    }

    @Override
    public Object getResponseId(JSONObject response) {
        return response.get(idFieldName);
    }



    @Setter
    @Getter
    private static class SimpleBotWSClientHandler extends BaseBotWSClientHandler<JSONObject, JSONObject, Object> {

        private BotJsonWSClient wsClient;

        @Override
        public JSONObject convertMessageToRespType(String message) {
            return JSONObject.parseObject(message);
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            // 如果握手未完成，处理 FullHttpResponse
            if (getHandshaker() != null && !getHandshaker().isHandshakeComplete()) {
                if (msg instanceof FullHttpResponse response) {
                    try {

                        getHandshaker().finishHandshake(ch, response);
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
    }
}
