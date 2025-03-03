package cn.com.helei.websocket.netty.handler;

public interface WSStatusHandler {

    void onConnected();

    void onClosed();
}
