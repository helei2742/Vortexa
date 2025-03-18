package cn.com.vortexa.websocket.netty.base;

import java.util.concurrent.CompletableFuture;

/**
 * @author h30069248
 * @since 2025/3/18 11:50
 */
public interface IWSService {
    /**
     * 获取服务名字
     *
     * @return String
     */
    String getName();

    /**
     * 发送ping
     */
    void sendPing();

    /**
     * 发送pong
     */
    void sendPong();

}
