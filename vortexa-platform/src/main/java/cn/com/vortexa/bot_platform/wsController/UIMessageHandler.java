package cn.com.vortexa.bot_platform.wsController;

import jakarta.websocket.Session;

/**
 * @author helei
 * @since 2025/4/1 17:34
 */
public interface UIMessageHandler {
    UIWSMessage handle(String username, Session session, UIWSMessage message);
}
