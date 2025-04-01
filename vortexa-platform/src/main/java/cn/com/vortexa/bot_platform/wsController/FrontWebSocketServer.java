package cn.com.vortexa.bot_platform.wsController;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.util.StringUtil;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
@Component
@ServerEndpoint("/websocket/{token}")
public class FrontWebSocketServer {
    //Map用来存储已连接的客户端信息
    private static final ConcurrentHashMap<String, SocketDomain> websocketMap = new ConcurrentHashMap<>();
    //在线客户端数量
    private static int onlineCount = 0;
    @Getter
    private static final ConcurrentHashMap<Integer, List<UIMessageHandler>> messageHandlerMap = new ConcurrentHashMap<>();

    private static BiConsumer<FrontWebSocketServer, String> closeHandler;

    //当前客户端名称
    private String token = "";


    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        if (!websocketMap.containsKey(token)) {
            FrontWebSocketServer.onlineCount++;
        }

        this.token = token;

        SocketDomain socketDomain = new SocketDomain();
        socketDomain.setSession(session);
        socketDomain.setUri(session.getRequestURI().toString());
        websocketMap.put(token, socketDomain);

        log.info("[{}] connect, online:{}", token, onlineCount);
    }

    @OnClose
    public void onClose() {
        onlineCount--;
        log.info("[{}] close, online:{}", token, onlineCount);
        websocketMap.remove(token);
        if (closeHandler != null) {
            closeHandler.accept(this, token);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws FrontWSException {
        if (StringUtil.isNotEmpty(message)) {
            UIWSMessage uiwsMessage = JSONObject.parseObject(message, UIWSMessage.class);
            log.info("receive:{}, message:{}", token, message);


            List<UIMessageHandler> handlerList = messageHandlerMap.get(uiwsMessage.getCode());
            if (handlerList != null) {
                for (UIMessageHandler handler : handlerList) {
                    UIWSMessage response = handler.handle(token, session, uiwsMessage);
                    if (response != null) {
                        sendMessage(token, response);
                    }
                }
            } else {
                log.warn("unknown message code {}", uiwsMessage.getCode());
            }
        }
    }

    /**
     * 检查token是否在线
     *
     * @param token token
     * @return boolean
     */
    public boolean isSessionOnline(String token) {
        return StrUtil.isNotBlank(token) && websocketMap.containsKey(token);
    }

    /**
     * 添加消息处理器
     *
     * @param flag    flag
     * @param handler handler
     */
    public void addMessageHandler(Integer flag, UIMessageHandler handler) {
        messageHandlerMap.compute(flag, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(handler);
            return v;
        });
    }

    /**
     * 添加消息处理器
     *
     * @param closeHandler closeHandler
     */
    public void setCloseHandler(BiConsumer<FrontWebSocketServer, String> closeHandler) {
        FrontWebSocketServer.closeHandler = closeHandler;
    }

    /**
     * 给token客户端发消息
     *
     * @param token token
     * @param obj   obj
     */
    public void sendMessage(String token, Object obj) throws FrontWSException {
        synchronized (getSessionLock(token)) {
            SocketDomain socketDomain = websocketMap.get(token);
            if (socketDomain != null) {
                socketDomain.getSession().getAsyncRemote().sendText(JSONObject.toJSONString(obj));
            } else {
                throw new FrontWSException("send message to[%s] error, target session not exist".formatted(token));
            }
        }
    }


    private String getSessionLock(String token) {
        return token;
    }
}
