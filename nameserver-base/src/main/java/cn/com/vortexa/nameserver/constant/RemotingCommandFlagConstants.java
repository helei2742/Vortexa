package cn.com.vortexa.nameserver.constant;

public class RemotingCommandFlagConstants {

    public static final String REQUEST_ERROR = "request_error";

    /**
     * 超时异常
     */
    public static final int TIME_OUT_EXCEPTION = 501;

    /**
     * 参数解析异常
     */
    public static final int PARAMS_ERROR = 502;

    /**
     * ping
     */
    public static final int PING = 100;

    /**
     * pong
     */
    public static final int PONG = -100;

    /**
     *  客户端服务注册的消息
     */
    public static final int CLIENT_REGISTRY_SERVICE = 101;

    /**
     *  客户端服务注册的响应
     */
    public static final int CLIENT_REGISTRY_SERVICE_RESPONSE = 101;

    /**
     * 客户端服务发现的消息
     */
    public static final int CLIENT_DISCOVER_SERVICE = 102;

    /**
     * 客户端服务发现的响应
     */
    public static final int CLIENT_DISCOVER_SERVICE_RESPONSE = -102;
}
