package cn.com.vortexa.common.constants;

/**
 * @author helei
 * @since 2025/3/26 14:45
 */
public class BotRemotingCommandFlagConstants {

    public static final int SCRIPT_BOT_ON_LINE = 6000;  // 暴露bot
    public static final int SCRIPT_BOT_ON_LINE_RESPONSE = -6000; // 暴露bot的响应

    public static final int SCRIPT_BOT_OFF_LINE = 6001;  // 暴露bot
    public static final int SCRIPT_BOT_OFF_LINE_RESPONSE = -6001; // 暴露bot的响应

    public static final int START_UP_BOT_LOG = 6002;    // 开始上传Bot运行日志
    public static final int START_UP_BOT_LOG_RESPONSE = -6002;  // 开始上传Bot运行日志的响应
    public static final int STOP_UP_BOT_LOG = 6003;    // 停止上传Bot运行日志
    public static final int STOP_UP_BOT_LOG_RESPONSE = -6003;  // 停止上传Bot运行日志的响应

    public static final int BOT_RUNTIME_LOG = 6666;    // 上传日志
    public static final int BOT_RUNTIME_LOG_RESPONSE = -6666;    // 上传日志响应

    public static final int START_BOT_JOB = 6004;   // 开始Job
    public static final int START_BOT_JOB_RESPONSE = -6004; //开始Job的响应

    public static final int STOP_BOT_JOB = 6005;   // 停止Job
    public static final int STOP_BOT_JOB_RESPONSE = -6005; // 停止Job的响应

    public static final int START_BOT = 6006;   // 启动Bot
    public static final int START_BOT_RESPONSE = -6006;     //  启动Bot响应

    public static final int STOP_BOT = 6007;    //  关闭Bot
    public static final int STOP_BOT_RESPONSE = -6007;  //  关闭Bot响应

    public static final int QUERY_BOT_ACCOUNT = 6008;   //  查bot账户
    public static final int QUERY_BOT_ACCOUNT_RESPONSE = -6008; //  查bot账户响应
}
