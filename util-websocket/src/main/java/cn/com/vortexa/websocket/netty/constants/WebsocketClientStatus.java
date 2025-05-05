package cn.com.vortexa.websocket.netty.constants;


import java.io.Serial;
import java.io.Serializable;

/**
 * WS客户端状态
 */
public enum WebsocketClientStatus implements Serializable {

    /**
     * 新建
     */
    NEW,

    /**
     * 正在启动
     */
    STARTING,

    /**
     * 正在运行
     */
    RUNNING,

    /**
     * 已暂停
     */
    STOP,

    /**
     * 已禁止使用
     */
    SHUTDOWN
    ;

    @Serial
    private static final long serialVersionUID = -8347538478384783743L;
}
