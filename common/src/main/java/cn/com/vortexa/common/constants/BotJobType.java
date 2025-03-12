package cn.com.vortexa.common.constants;

/**
 * Bot Job 类型
 */
public enum BotJobType {
    /**
     * 注册
     */
    REGISTER,
    /**
     * 登录
     */
    LOGIN,
    /**
     * 查询奖励
     */
    QUERY_REWARD,
    /**
     * 只运行一次的任务
     */
    ONCE_TASK,
    /**
     * 定时任务
     */
    TIMED_TASK,
    /**
     * web socket连接任务
     */
    WEB_SOCKET_CONNECT,

    /**
     * 按账户拆分后的JOB
     */
    ACCOUNT_SPLIT_JOB
}
