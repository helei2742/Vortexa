package cn.com.vortexa.common.dto;

import cn.com.vortexa.common.constants.ConnectStatus;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Account的连接状态
 */
@Data
public class ConnectStatusInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -2394832984738927493L;

    /**
     * 开始时间
     */
    private volatile LocalDateTime startDateTime;

    /**
     * 更新时间
     */
    private volatile LocalDateTime updateDateTime;

    /**
     * 心跳数
     */
    private final AtomicInteger heartBeat = new AtomicInteger(0);

    /**
     * 错误的心跳数
     */
    private final AtomicInteger errorHeartBeat = new AtomicInteger(0);

    /**
     * 重启次数
     */
    private final AtomicInteger restart = new AtomicInteger(0);

    /**
     * 连接状态
     */
    private volatile ConnectStatus connectStatus = ConnectStatus.NEW;
}
