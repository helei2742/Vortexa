package cn.com.vortexa.control.dto;


import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.control.constant.ControlServerState;
import lombok.Data;

/**
 * @author helei
 * @since 2025-03-20
 */
@Data
public class RemoteControlServerStatus {

    private ControlServerState controlServerState = ControlServerState.SHUT_DOWN;

    private long lastUpdateTimestamp = System.currentTimeMillis();

    private ServiceInstance nameserverInstance;
}
