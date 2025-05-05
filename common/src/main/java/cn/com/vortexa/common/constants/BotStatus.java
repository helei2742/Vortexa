package cn.com.vortexa.common.constants;

import java.io.Serial;
import java.io.Serializable;

/**
 * Depin Bot 状态
 */
public enum BotStatus implements Serializable {
    NOT_LOADED,

    NEW,

    INIT,
    INIT_ERROR,
    INIT_FINISH,

    STARTING,
    RUNNING,

    STOPPING,
    STOPPED,

    SHUTDOWN
    ;
    @Serial
    private static final long serialVersionUID = -3487492237319264823L;
}
