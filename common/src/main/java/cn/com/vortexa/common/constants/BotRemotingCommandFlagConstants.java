package cn.com.vortexa.common.constants;

/**
 * @author helei
 * @since 2025/3/26 14:45
 */
public class BotRemotingCommandFlagConstants {

    public static final int START_UP_BOT_LOG = 6001;    // 开始上传Bot运行日志
    public static final int START_UP_BOT_LOG_RESPONSE = -6001;  // 开始上传Bot运行日志的响应
    public static final int STOP_UP_BOT_LOG = 6002;    // 停止上传Bot运行日志
    public static final int STOP_UP_BOT_LOG_RESPONSE = -6002;  // 停止上传Bot运行日志的响应

    public static final int BOT_RUNTIME_LOG =  6666;    // 上传日志
    public static final int BOT_RUNTIME_LOG_RESPONSE =  -6666;    // 上传日志

    public static final int START_BOT_JOB = 6003;   // 开始Job
    public static final int START_BOT_JOB_RESPONSE = -6003; //开始Job的响应

    public static final int STOP_BOT_JOB = 6004;   // 停止Job
    public static final int STOP_BOT_JOB_RESPONSE = -6004; // 停止Job的响应

}
