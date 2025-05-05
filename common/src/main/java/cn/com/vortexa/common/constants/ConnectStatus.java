package cn.com.vortexa.common.constants;

import java.io.Serial;
import java.io.Serializable;

/**
 * 连接状态
 */
public enum ConnectStatus implements Serializable {

    NEW,
    STARTING,
    RUNNING,
    STOP
    ;

    @Serial
    private static final long serialVersionUID = -43238492374823742L;
}
