package cn.com.vortexa.control.constant;

public class RemotingCommandFlagConstants {

    public static final String REQUEST_ERROR = "request_error";

    public static final int TIME_OUT_EXCEPTION = 501;   // 超时异常

    public static final int PARAMS_ERROR = 502; // 参数解析异常

    public static final int PING = 100; // ping
    public static final int PONG = -100;    // pong

    public static final int SCRIPT_AGENT_METRICS_UPLOAD = 101; // 指标上传
    public static final int SCRIPT_AGENT_METRICS_UPLOAD_RESPONSE = -101; // 指标擅闯的响应

    public static final int CLIENT_REGISTRY_SERVICE = 200; // 客户端服务注册的消息
    public static final int CLIENT_REGISTRY_SERVICE_RESPONSE = -200; // 客户端服务注册的响应

    public static final int CLIENT_DISCOVER_SERVICE = 201; // 客户端服务发现的消息
    public static final int CLIENT_DISCOVER_SERVICE_RESPONSE = -201; // 客户端服务发现的响应

    public static final int CUSTOM_COMMAND = 300;   // 自定义命令
    public static final int CUSTOM_COMMAND_RESPONSE = -300;  // 自定义命令响应
}
