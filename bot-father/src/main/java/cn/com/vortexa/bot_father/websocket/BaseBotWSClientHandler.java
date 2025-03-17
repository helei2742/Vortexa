package cn.com.vortexa.bot_father.websocket;

import cn.com.vortexa.common.dto.ConnectStatusInfo;
import cn.com.vortexa.websocket.netty.base.AbstractWebSocketClientHandler;
import cn.com.vortexa.websocket.netty.base.HandlerEntity;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseBotWSClientHandler<Req, Resp, T> extends AbstractWebSocketClientHandler<Req, Resp, T> {

    /**
     * channel 空闲，向其发送心跳
     *
     * @param ctx ctx
     */
    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        BaseBotWSClient<Req, Resp> botWSClient = getBotWSClient();


        Req heartbeatMessage = botWSClient.getHeartbeatMessage();
        if (heartbeatMessage != null) {
            botWSClient
                    .sendMessage(heartbeatMessage)
                    .whenCompleteAsync((unused, throwable) -> {
                        ConnectStatusInfo connectStatusInfo = botWSClient
                                .getAccountContext()
                                .getConnectStatusInfo();

                        if (throwable != null) {
                            log.error("client[{}] 发送心跳异常", websocketClient.getName(), throwable);
                            // 发送心跳失败，记录次数
                            connectStatusInfo.getErrorHeartBeat().getAndIncrement();
                        }

                        // 心跳计数
                        connectStatusInfo.getHeartBeat()
                                .getAndIncrement();
                    }, botWSClient.getCallbackInvoker());
        }
    }

    protected void whenReceiveMessage(String text) {
        Resp message = convertMessageToRespType(text);

        Object responseId = getResponseId(message);

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
    public abstract Resp convertMessageToRespType(String message);


    /**
     * 获取请求id
     *
     * @param request request
     * @return id
     */
    public Object getRequestId(Req request) {
        return getBotWSClient().getRequestId(request);
    }

    /**
     * 获取响应id
     *
     * @param response 响应
     * @return id
     */
    public Object getResponseId(Resp response) {
        return getBotWSClient().getResponseId(response);
    }


    /**
     * 处理请求响应的消息
     *
     * @param id       id
     * @param response 响应消息体
     */
    protected void handleResponseMessage(Object id, Resp response) {
        HandlerEntity<Resp> handlerEntity = requestIdMap.get(id);

        if (System.currentTimeMillis() > handlerEntity.getExpireTime()) {
            log.warn("请求[{}]得到响应超时", id);
        } else {
            websocketClient.getCallbackInvoker().execute(() -> handlerEntity.getCallback().accept(response));
        }
    }

    /**
     * 处理其他类型消息
     *
     * @param message 消息
     */
    protected void handleOtherMessage(Resp message) {
        getBotWSClient().whenAccountReceiveMessage(message);
    }

    /**
     * 处理close消息
     *
     * @param ch   Channel ch
     * @param ping closeWebSocketFrame
     */
    protected void handlerClose(Channel ch, T ping) {
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
        websocketClient.sendPong();
    }

    private BaseBotWSClient<Req, Resp> getBotWSClient() {
        return (BaseBotWSClient<Req, Resp>) websocketClient;
    }
}
