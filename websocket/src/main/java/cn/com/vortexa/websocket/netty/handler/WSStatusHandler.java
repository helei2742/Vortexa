package cn.com.vortexa.websocket.netty.handler;

public interface WSStatusHandler {

    void onConnected();

    void onClosed();
}
