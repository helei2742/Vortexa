package cn.com.vortexa.script_node.constants;

/**
 * Depin Bot 状态
 */
public enum BotStatus {
    NOT_LOADED,

    NEW,

    INIT,
    INIT_ERROR,
    INIT_FINISH,

    STARTING,
    RUNNING,

    STOPPING,
    STOPPED,

    SHUTDOWN,
}
